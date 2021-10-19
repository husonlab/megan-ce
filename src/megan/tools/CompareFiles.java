/*
 * ComputeComparison.java Copyright (C) 2021. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megan.tools;

import jloda.seq.BlastMode;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.core.ClassificationType;
import megan.core.Document;
import megan.dialogs.compare.Comparer;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * compares multiple samples
 * Daniel Huson, 8.2018
 */
public class CompareFiles {
    /**
     * ComputeComparison
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
            ProgramProperties.setProgramName("CompareFiles");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new CompareFiles()).run(args);
            System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
            System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void run(String[] args) throws Exception {
        final ArgsOptions options = new ArgsOptions(args, this, "Computes the comparison of multiple megan, RMA or meganized DAA files");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2021 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output:");
        final ArrayList<String> inputFiles = new ArrayList<>(Arrays.asList(options.getOptionMandatory("-i", "in", "Input RMA and/or meganized DAA files (single directory ok)", new String[0])));
        final String outputFile = options.getOption("-o", "out", "Output file", "comparison.megan");
        final String metadataFile = options.getOption("-mdf", "metaDataFile", "Metadata file", "");
        options.comment("Options:");
        final boolean allowSameNames=options.getOption("-s","allowSameNames","All the same sample name to appear multiple times (will add -1, -2 etc)",false);

        final boolean normalize = options.getOption("-n", "normalize", "Normalize counts", true);
        final boolean ignoreUnassignedReads = options.getOption("-iu", "ignoreUnassignedReads", "Ignore unassigned, no-hit or contaminant reads", false);

        final boolean keepOne = options.getOption("-k1", "keepOne", "In a normalized comparison, non-zero counts are mapped to 1 or more", false);

        options.done();

		if (inputFiles.size() == 1 && FileUtils.isDirectory(inputFiles.get(0))) {
			final String directory = inputFiles.get(0);
			inputFiles.clear();
			inputFiles.addAll(FileUtils.getAllFilesInDirectory(directory, true, ".megan", ".megan.gz", ".daa", ".rma", ".rma6"));
		}

        for (String fileName : inputFiles) {
			if (!FileUtils.fileExistsAndIsNonEmpty(fileName))
				throw new IOException("No such file or file empty: " + fileName);
        }

        if (inputFiles.size() == 0)
            throw new UsageException("No input file");

        final ArrayList<SampleData> samples=new ArrayList<>();

        for(var fileName:inputFiles) {
            System.err.println("Processing file: "+fileName);
            final var doc=new Document();
            doc.getMeganFile().setFileFromExistingFile(fileName,true);
            doc.loadMeganFile();

            final var docSamples=doc.getSampleNamesAsArray();
            for(var s=0;s<docSamples.length;s++) {
                final var sample=new SampleData(doc,s);
                samples.add(sample);
                System.err.println(sample);
            }
        }

        // ensure unique names:
        {
            final Set<String> names = new HashSet<>();
            int count=0;
            for (var sample : samples) {
                if (names.contains(sample.getName())) {
                    if (allowSameNames) {
                        if(count==0)
                            System.err.println("Renaming samples to make all names unique:");
						final var name = StringUtils.getUniqueName(sample.getName(), names);
                        System.err.println(sample.getName()+" -> "+name);
                        sample.setName(name);
                    }
                    count++;
                }
                names.add(sample.getName());
            }
            if(count>0 && !allowSameNames)
                throw new IOException("Same sample name occurs more than once, "+count+" times (use option -s to allow)");
        }

        System.err.printf("Input files:%13d%n",inputFiles.size());
        System.err.printf("Input samples:%11d%n",samples.size());

        //System.err.printf("Input files: %s%n",Basic.toString(inputFiles,", "));

        System.err.printf("Input count:%,13d%n",(long) getTotalCount(samples));
        System.err.printf("In assigned:%,13d%n",(long) getTotalAssigned(samples));
        System.err.printf("Read assignment mode: %s%n",samples.get(0).getReadAssignmentMode());

        final Document.ReadAssignmentMode readAssignmentMode;
        {
            final var modes = new TreeSet<>(Arrays.asList(getReadAssignmentModes(samples)));
            if (modes.size() > 1)
				throw new IOException("Can't compare normalized samples with mixed assignment modes, found: " + StringUtils.toString(modes, ", "));
            readAssignmentMode = (modes.size() == 0 ? Document.ReadAssignmentMode.readCount : modes.first());
        }

         final OptionalDouble min;

        if(ignoreUnassignedReads)
            min=samples.stream().mapToDouble(SampleData::getAssigned).min();
        else
            min=samples.stream().mapToDouble(SampleData::getCount).min();

        if(min.isEmpty())
            throw new IOException("No reads found");
        else if(normalize) {
            System.err.printf("Normalizing to:%,10d per sample%n",(long) min.getAsDouble());
        }

        final int numberOfSamples=samples.size();

        final Document doc = new Document();
        final float[] sizes;
        if(!normalize) {
            if(!ignoreUnassignedReads)
                sizes=getCounts(samples);
            else
                sizes=getAssigneds(samples);
        } else {
            sizes = new float[numberOfSamples];
            Arrays.fill(sizes,(float)min.getAsDouble());
        }

        doc.getDataTable().setSamples(getSampleNames(samples), getUids(samples), sizes, getBlastModes(samples));
        {
            final Map<String,Object> sample2source=new HashMap<>();
            for(SampleData sample:samples) {
                sample2source.put(sample.getName(),sample.getDoc().getMeganFile().getFileName());
            }
            doc.getSampleAttributeTable().addAttribute("@Source",sample2source,false,true);
        }

        doc.setNumberReads(Math.round(CollectionUtils.getSum(sizes)));

        for(var classification: getClassifications(samples)) {
            final Map<Integer,float[]> class2counts=new HashMap<>();

            for(var sample:samples) {
                final double factor;
                if(normalize) {
                    if(ignoreUnassignedReads)
                        factor=(sample.getAssigned()>0?min.getAsDouble()/sample.getAssigned():1);
                    else
                        factor=(sample.getCount()>0?min.getAsDouble()/sample.getCount():1);
                }
                else
                    factor=1;
                sample.setFactor(factor);
            }

            for(int c: getClassIds(classification,samples,ignoreUnassignedReads)) {
                final float[] newValues=class2counts.computeIfAbsent(c,z->new float[numberOfSamples]);
                for (int s = 0; s < numberOfSamples; s++) {
                    final SampleData sample = samples.get(s);
                    final int which=sample.getWhich();
                    final float[] values = sample.getDoc().getDataTable().getClass2Counts(classification).get(c);
                    if (values != null && which<values.length) {
                        final float value=values[which];
                         newValues[s] = (float)sample.getFactor() * value;
                         if(keepOne && value>0 && newValues[s]==0)
                             newValues[s]=1;
                    }
                }
            }
            doc.getDataTable().setClass2Counts(classification,class2counts);
        }

         doc.setReadAssignmentMode(readAssignmentMode);

		String parameters = "mode=" + (normalize ? Comparer.COMPARISON_MODE.RELATIVE : Comparer.COMPARISON_MODE.ABSOLUTE);
		if (normalize)
			parameters += " normalizedTo=" + StringUtils.removeTrailingZerosAfterDot("" + min.getAsDouble());
		parameters += " readAssignmentMode=" + readAssignmentMode.toString();
		if (ignoreUnassignedReads)
			parameters += " ignoreUnassigned=true";
		doc.getDataTable().setParameters(parameters);

		System.err.printf("Output count:%,12d%n", doc.getNumberOfReads());

		if (StringUtils.notBlank(metadataFile)) {
			try (BufferedReader r = new BufferedReader(new InputStreamReader(FileUtils.getInputStreamPossiblyZIPorGZIP(metadataFile)))) {
				System.err.print("Processing Metadata: " + metadataFile);
				doc.getSampleAttributeTable().read(r, doc.getSampleNames(), true);
				System.err.println(", attributes: " + doc.getSampleAttributeTable().getNumberOfUnhiddenAttributes());
			}
		}

		System.err.println("Saving to file: " + outputFile);

		try (var writer = new FileWriter(outputFile)) {
			doc.getDataTable().write(writer);
            doc.getSampleAttributeTable().write(writer, false, true);
        }
    }

    public static double getTotalCount (Collection<SampleData> samples) {
        return samples.stream().mapToDouble(SampleData::getCount).sum();
    }

    public static float[] getCounts(List<SampleData> samples) {
        final var counts=new float[samples.size()];
        for(int s=0;s<samples.size();s++)
            counts[s]=samples.get(s).getCount();
        return counts;
    }

    public static float[] getAssigneds(List<SampleData> samples) {
        final var assigneds=new float[samples.size()];
        for(int s=0;s<samples.size();s++)
            assigneds[s]=samples.get(s).getAssigned();
        return assigneds;
    }

    public static double getTotalAssigned(Collection<SampleData> samples) {
        return samples.stream().mapToDouble(SampleData::getAssigned).sum();
    }

    public static List<String> getClassifications (Collection<SampleData> samples) {
        return samples.stream().map(SampleData::getClassifications).flatMap(Collection::stream).distinct().collect(Collectors.toList());
    }

    public static Set<Integer> getClassIds (String classification, Collection<SampleData> samples,boolean assignedOnly) {
        return samples.parallelStream().map(s->s.getDoc().getDataTable().getClass2Counts(classification)).filter(Objects::nonNull).map(Map::keySet).flatMap(Set::stream).
                filter(id->!assignedOnly || id>0).collect(Collectors.toSet());
    }

    public static String[] getSampleNames(Collection<SampleData> samples) {
        return samples.stream().map(SampleData::getName).toArray(String[]::new);
    }

    public static Long[] getUids (Collection<SampleData> samples) {
        return samples.stream().map(SampleData::getUid).toArray(Long[]::new);
    }

    public static BlastMode[] getBlastModes(Collection<SampleData> samples) {
        return samples.stream().map(SampleData::getBlastMode).toArray(BlastMode[]::new);
    }
    public static Document.ReadAssignmentMode[] getReadAssignmentModes(Collection<SampleData> samples) {
        return samples.stream().map(SampleData::getReadAssignmentMode).toArray(Document.ReadAssignmentMode[]::new);
    }

    public static class SampleData {
        private final Document doc;
        private String name;
        private final long uid;
        private final int which;
        private final float count;
        private final float assigned;
        private final BlastMode blastMode;
        private final Document.ReadAssignmentMode readAssignmentMode;
        private final ArrayList<String> classifications;

        private double factor=1;

        public SampleData(Document doc, int which) {
            this.doc = doc;
            this.which = which;
            this.name = doc.getSampleNames().get(which);
            this.uid=doc.getDataTable().getSampleUIds()[which];

            final Map<Integer, float[]> class2count = doc.getDataTable().getClass2Counts(ClassificationType.Taxonomy);

            float assigned=0;
            float unassigned=0;
            for(var id:class2count.keySet()) {
                final float[] values=class2count.get(id);
                if(which<values.length) {
                    if (id > 0)
                        assigned += values[which];
                    else
                        unassigned += values[which];
                }
            }
            this.count=assigned+unassigned;
            this.assigned=assigned;

            blastMode=doc.getBlastMode();
            readAssignmentMode = doc.getReadAssignmentMode();
            classifications = new ArrayList<>(doc.getClassificationNames());
        }

        public Document getDoc() {
            return doc;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getUid() {
            return uid;
        }

        public int getWhich() {
            return which;
        }

        public float getCount() {
            return count;
        }

        public float getAssigned() {
            return assigned;
        }

        public BlastMode getBlastMode() {
            return blastMode;
        }

        public Document.ReadAssignmentMode getReadAssignmentMode() {
            return readAssignmentMode;
        }

        public ArrayList<String> getClassifications() {
            return classifications;
        }

        public double getFactor() {
            return factor;
        }

        public void setFactor(double factor) {
            this.factor = factor;
        }

        @Override
        public String toString() {
			return String.format("Sample %s [%d in %s]: count=%,d assigned=%,d mode=%s classifications=%s",
					name, which, FileUtils.getFileNameWithoutPath(doc.getMeganFile().getFileName()), (int) count, (int) assigned, readAssignmentMode.toString(), StringUtils.toString(classifications, " "));
         }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final var that = (SampleData) o;
            return which == that.which &&
                    Float.compare(that.count, count) == 0 &&
                    Float.compare(that.assigned, assigned) == 0 &&
                    doc.getMeganFile().getFileName().equals(that.doc.getMeganFile().getFileName()) &&
                    name.equals(that.name) &&
                    readAssignmentMode == that.readAssignmentMode &&
                    classifications.equals(that.classifications);
        }

        @Override
        public int hashCode() {
            return Objects.hash(doc.getMeganFile().getFileName(), name, which, count, assigned, readAssignmentMode, classifications);
        }
    }
}
