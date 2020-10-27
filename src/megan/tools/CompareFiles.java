/*
 * ComputeComparison.java Copyright (C) 2020. Daniel H. Huson
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

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.core.ClassificationType;
import megan.core.Document;
import megan.dialogs.compare.Comparer;
import megan.main.Megan6;

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
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
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
        options.setLicense("Copyright (C) 2020 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output:");
        final ArrayList<String> inputFiles = new ArrayList<>(Arrays.asList(options.getOptionMandatory("-i", "in", "Input RMA and/or meganized DAA files (single directory ok)", new String[0])));
        final String outputFile = options.getOption("-o", "out", "Output file", "comparison.megan");

        final String metadataFile = options.getOption("-mdf", "metaDataFile", "Metadata file", "");
        options.comment("Options:");

        final boolean normalize = options.getOption("-n", "normalize", "Normalize counts", true);
        final boolean ignoreUnassignedReads = options.getOption("-iu", "ignoreUnassignedReads", "Ignore unassigned, no-hit or contaminant reads", false);

        final Document.ReadAssignmentMode readAssignmentMode = Document.ReadAssignmentMode.valueOfIgnoreCase(options.getOption("-ram", "readAssignmentMode", "Set the desired read-assignment mode", Document.ReadAssignmentMode.readCount.toString()));
        final boolean keepOne = options.getOption("-k1", "keepOne", "In a normalized comparison, minimum non-zero count is set to 1", false);

        options.done();

        if (inputFiles.size() == 1 && Basic.isDirectory(inputFiles.get(0))) {
            final String directory = inputFiles.get(0);
            inputFiles.clear();
            inputFiles.addAll(Basic.getAllFilesInDirectory(directory, true, ".megan",".megan.gz",".daa", ".rma", ".rma6"));
        }

        for (String fileName : inputFiles) {
            if (!Basic.fileExistsAndIsNonEmpty(fileName))
                throw new IOException("No such file or file empty: " + fileName);
        }

        if (inputFiles.size() == 0)
            throw new UsageException("No input file");


        final ArrayList<SampleData> samples=new ArrayList<>();

        for(var fileName:inputFiles) {
            var doc=new Document();
            doc.getMeganFile().setFileFromExistingFile(fileName,true);
            doc.loadMeganFile();

            final String[] docSamples=doc.getSampleNamesAsArray();
            for(var s=0;s<docSamples.length;s++) {
                final SampleData sample=new SampleData(doc,s);
                samples.add(sample);
                System.err.println(sample);
            }
        }

        System.err.printf("Input files:%12d%n",inputFiles.size());
        //System.err.printf("Input files: %s%n",Basic.toString(inputFiles,", "));

        System.err.printf("Total count:%,12d%n",(long) getTotalCount(samples));
        System.err.printf("T. assigned:%,12d%n",(long) getTotalAssigned(samples));
        System.err.printf("Read assignment mode: %s%n",samples.get(0).getReadAssignmentMode());

        if(new HashSet<>(Arrays.asList(getBlastModes(samples))).size()>1)
            throw new IOException("Can't compare normalized samples with mixed assignment modes");

         final OptionalDouble min;

        if(ignoreUnassignedReads)
            min=samples.stream().mapToDouble(SampleData::getAssigned).min();
        else
            min=samples.stream().mapToDouble(SampleData::getCount).min();

        if(min.isEmpty())
            throw new IOException("No reads found");
        else if(normalize)
            System.err.printf("Normalizing to %,d reads%n",(long)min.getAsDouble());

        final int numberOfSamples=samples.size();

        final Document doc = new Document();
        doc.getDataTable().setSamples(getSampleNames(samples), getUids(samples), getCounts(samples), getBlastModes(samples));
        doc.getDataTable().setTotalReads(Math.round(Basic.getSum(getCounts(samples))));

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

            for(int c: getClassIds(classification,samples)) {
                final float[] newValues=class2counts.computeIfAbsent(c,z->new float[numberOfSamples]);
                for (int s = 0; s < numberOfSamples; s++) {
                    SampleData sample = samples.get(s);
                    final float[] values = sample.getDoc().getDataTable().getClass2Counts(classification).get(c);
                    if (values != null)
                        newValues[s] += sample.getFactor() * values[sample.getWhich()];
                }
            }
            doc.getDataTable().setClass2Counts(classification,class2counts);
        }

        if (Basic.notBlank(metadataFile)) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(Basic.getInputStreamPossiblyZIPorGZIP(metadataFile)))) {
                System.err.print("Processing Metadata: " + metadataFile);
                doc.getSampleAttributeTable().read(r, doc.getSampleNames(), true);
                System.err.println(", attributes: " + doc.getSampleAttributeTable().getNumberOfUnhiddenAttributes());
            }
        }

        doc.setTopPercent(100);
        doc.setMinScore(0);
        doc.setMinSupportPercent(0);
        doc.setMinSupport(1);
        doc.setMaxExpected(10000);
        doc.setReadAssignmentMode(samples.get(0).getReadAssignmentMode());

        String parameters="mode=" +(normalize? Comparer.COMPARISON_MODE.RELATIVE:Comparer.COMPARISON_MODE.ABSOLUTE);
        if (normalize)
            parameters += " normalizedTo=" + Basic.removeTrailingZerosAfterDot(""+min.getAsDouble());
        parameters+=" readAssignmentMode="+samples.get(0).getReadAssignmentMode().toString();
        if (ignoreUnassignedReads)
            parameters += " ignoreUnassigned=true";
        doc.getDataTable().setParameters(parameters);

        System.err.println("Saving to file: " + outputFile);

        try (FileWriter writer = new FileWriter(outputFile)) {
            doc.getDataTable().write(writer);
            doc.getSampleAttributeTable().write(writer, false, true);
        }
    }

    public static double getTotalCount (Collection<SampleData> samples) {
        return samples.stream().mapToDouble(SampleData::getCount).sum();
    }

    public static float[] getCounts(List<SampleData> samples) {
        final float[] counts=new float[samples.size()];
        for(int s=0;s<samples.size();s++)
            counts[s]=samples.get(s).getCount();
        return counts;
    }

    public static double getTotalAssigned(Collection<SampleData> samples) {
        return samples.stream().mapToDouble(SampleData::getAssigned).sum();
    }

    public static Set<String> getClassifications (Collection<SampleData> samples) {
        return samples.stream().map(SampleData::getClassifications).flatMap(Collection::stream).collect(Collectors.toCollection(TreeSet::new));
    }

    public static Set<Integer> getClassIds (String classification, Collection<SampleData> samples) {
        return samples.parallelStream().map(s->s.getDoc().getDataTable().getClass2Counts(classification)).filter(Objects::nonNull).map(Map::keySet).flatMap(Set::stream).collect(Collectors.toSet());
    }

    public static String[] getSampleNames(Collection<SampleData> samples) {
        return samples.stream().map(SampleData::getSample).toArray(String[]::new);
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
        private final String sample;
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
            this.sample = doc.getSampleNames().get(which);
            this.uid=doc.getDataTable().getSampleUIds()[which];

            final Map<Integer, float[]> class2count = doc.getDataTable().getClass2Counts(ClassificationType.Taxonomy);

            float assigned=0;
            float unassigned=0;
            for(var id:class2count.keySet()) {
                if(id>0)
                    assigned+=class2count.get(id)[which];
                else
                    unassigned+=class2count.get(id)[which];
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

        public String getSample() {
            return sample;
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
                    sample,which,Basic.getFileNameWithoutPath(doc.getMeganFile().getFileName()),(int)count,(int)assigned, readAssignmentMode.toString(),Basic.toString(classifications," "));
         }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SampleData that = (SampleData) o;
            return which == that.which &&
                    Float.compare(that.count, count) == 0 &&
                    Float.compare(that.assigned, assigned) == 0 &&
                    doc.getMeganFile().getFileName().equals(that.doc.getMeganFile().getFileName()) &&
                    sample.equals(that.sample) &&
                    readAssignmentMode == that.readAssignmentMode &&
                    classifications.equals(that.classifications);
        }

        @Override
        public int hashCode() {
            return Objects.hash(doc.getMeganFile().getFileName(), sample, which, count, assigned, readAssignmentMode, classifications);
        }
    }
}
