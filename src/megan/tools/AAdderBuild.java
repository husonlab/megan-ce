/*
 *  Copyright (C) 2019 Daniel H. Huson
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
 */

package megan.tools;

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.interval.Interval;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.genes.CDS;
import megan.genes.GeneItem;
import megan.genes.GeneItemCreator;
import megan.io.OutputWriter;
import megan.main.Megan6;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * build the aadder index
 * Daniel Huson, 5.2018
 */
public class AAdderBuild {
    final public static byte[] MAGIC_NUMBER_IDX = "AAddIdxV0.1.".getBytes();
    final public static byte[] MAGIC_NUMBER_DBX = "AAddDbxV0.1.".getBytes();

    private final static String INDEX_CREATOR = "AADD";

    /**
     * add functional annotations to DNA alignments
     */
    public static void main(String[] args) {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("AAdderBuild");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new AAdderBuild()).run(args);
            System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
            System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run the program
     */
    private void run(String[] args) throws CanceledException, IOException, UsageException {
        final ArgsOptions options = new ArgsOptions(args, this, "Build the index for AAdd");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input Output");
        final List<String> gffFiles = options.getOptionMandatory("-igff", "inputGFF", "Input GFF3 files or directory (.gz ok)", new LinkedList<>());
        final String indexDirectory = options.getOptionMandatory("-d", "index", "Index directory", "");

        options.comment("Classification mapping");

        final HashMap<String, String> class2AccessionFile = new HashMap<>();

        final String acc2TaxaFile = options.getOption("-a2t", "acc2taxa", "Accession-to-Taxonomy mapping file", "");

        for (String cName : ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy()) {
            class2AccessionFile.put(cName, options.getOption("-a2" + cName.toLowerCase(), "acc2" + cName.toLowerCase(), "Accession-to-" + cName + " mapping file", ""));
        }

        options.comment(ArgsOptions.OTHER);
        final boolean lookInside = options.getOption("-ex", "extraStrict", "When given an input directory, look inside every input file to check that it is indeed in GFF3 format", false);
        options.done();

        // setup the gff file:
        setupGFFFiles(gffFiles, lookInside);

        // setup gene item creator, in particular accession mapping
        final GeneItemCreator creator = setupCreator(acc2TaxaFile, class2AccessionFile);

        // obtains the gene annotations:
        Map<String, ArrayList<Interval<GeneItem>>> dnaId2list = computeAnnotations(creator, gffFiles);

        saveIndex(INDEX_CREATOR, creator, indexDirectory, dnaId2list, dnaId2list.keySet());
    }

    /**
     * setup the GFF files
     *
     * @param gffFiles
     * @param lookInside
     * @throws IOException
     */
    public static void setupGFFFiles(List<String> gffFiles, boolean lookInside) throws IOException {
        if (gffFiles.size() == 1) {
            final File file = new File(gffFiles.get(0));
            if (file.isDirectory()) {
                System.err.println("Looking for GFF3 files in directory: " + file);
                gffFiles.clear();
                for (File aFile : BasicSwing.getAllFilesInDirectory(file, new GFF3FileFilter(true, lookInside), true)) {
                    gffFiles.add(aFile.getPath());
                }
                if (gffFiles.size() == 0)
                    throw new IOException("No GFF files found in directory: " + file);
                else
                    System.err.println(String.format("Found: %,d", gffFiles.size()));
            }
        }
    }

    /**
     * setup the gene item creator
     *
     * @param acc2TaxaFile
     * @param class2AccessionFile
     * @return gene item creator
     * @throws CanceledException
     */
    public static GeneItemCreator setupCreator(String acc2TaxaFile, Map<String, String> class2AccessionFile) throws CanceledException {
        final String[] cNames;
        {
            final ArrayList<String> list = new ArrayList<>();
            if (acc2TaxaFile != null && acc2TaxaFile.length() > 0)
                list.add(Classification.Taxonomy);
            for (String cName : class2AccessionFile.keySet())
                if (class2AccessionFile.get(cName).length() > 0 && !list.contains(cName))
                    list.add(cName);
            cNames = list.toArray(new String[0]);
        }

        final IdMapper[] idMappers = new IdMapper[cNames.length];

        for (int i = 0; i < cNames.length; i++) {
            final String cName = cNames[i];
            idMappers[i] = ClassificationManager.get(cName, true).getIdMapper();
            if (cName.equals(Classification.Taxonomy) && acc2TaxaFile != null && acc2TaxaFile.length() > 0)
                idMappers[i].loadMappingFile(acc2TaxaFile, IdMapper.MapType.Accession, false, new ProgressPercentage());
            else
                idMappers[i].loadMappingFile(class2AccessionFile.get(cName), IdMapper.MapType.Accession, false, new ProgressPercentage());
        }
        return new GeneItemCreator(cNames, idMappers);
    }

    /**
     * compute annotations
     *
     * @param creator
     * @param gffFiles
     * @return
     * @throws IOException
     * @throws CanceledException
     */
    public static Map<String, ArrayList<Interval<GeneItem>>> computeAnnotations(GeneItemCreator creator, Collection<String> gffFiles) throws IOException, CanceledException {
        Map<String, ArrayList<Interval<GeneItem>>> dnaId2list = new HashMap<>();

        final Collection<CDS> annotations = CDS.parseGFFforCDS(gffFiles, new ProgressPercentage("Processing GFF files"));

        try (ProgressListener progress = new ProgressPercentage("Building annotation list", annotations.size())) {
            for (CDS cds : annotations) {
                ArrayList<Interval<GeneItem>> list = dnaId2list.computeIfAbsent(cds.getDnaId(), k -> new ArrayList<>());
                final GeneItem geneItem = creator.createGeneItem();
                final String accession = cds.getProteinId();
                geneItem.setProteinId(accession.getBytes());
                geneItem.setReverse(cds.isReverse());
                list.add(new Interval<>(cds.getStart(), cds.getEnd(), geneItem));
                progress.incrementProgress();
            }
        }
        return dnaId2list;
    }

    /**
     * save the index
     *
     * @param creator
     * @param indexDirectory
     * @param dnaId2list
     * @throws IOException
     */
    public static void saveIndex(String indexCreator, GeneItemCreator creator, String indexDirectory, Map<String, ArrayList<Interval<GeneItem>>> dnaId2list, Iterable<String> dnaIdOrder) throws IOException {
        // writes the index file:
        long totalRefWithAGene = 0;

        final File indexFile = new File(indexDirectory, "aadd.idx");
        final File dbFile = new File(indexDirectory, "aadd.dbx");
        try (OutputWriter idxWriter = new OutputWriter(indexFile); OutputWriter dbxWriter = new OutputWriter(dbFile);
             ProgressPercentage progress = new ProgressPercentage("Writing files: " + indexFile + "\n               " + dbFile, dnaId2list.size())) {

            idxWriter.write(MAGIC_NUMBER_IDX);
            idxWriter.writeString(indexCreator);
            idxWriter.writeInt(dnaId2list.size());

            dbxWriter.write(MAGIC_NUMBER_DBX);
            // write the list of classifications:
            dbxWriter.writeInt(creator.numberOfClassifications());
            for (String cName : creator.cNames()) {
                dbxWriter.writeString(cName);
            }


            for (String dnaId : dnaIdOrder) {
                idxWriter.writeString(dnaId);
                final ArrayList<Interval<GeneItem>> list = dnaId2list.get(dnaId);
                if (list == null) {
                    idxWriter.writeLong(0); // no intervals
                } else {
                    idxWriter.writeLong(dbxWriter.getPosition()); // position of intervals in DB file

                    dbxWriter.writeInt(list.size());
                    for (Interval<GeneItem> interval : Basic.randomize(list, 666)) { // need to save in random order
                        dbxWriter.writeInt(interval.getStart());
                        dbxWriter.writeInt(interval.getEnd());
                        interval.getData().write(dbxWriter);
                    }
                    totalRefWithAGene++;
                }
                progress.incrementProgress();
            }
        }

        System.err.println(String.format("Reference sequences with at least one annotation: %,d of %,d", totalRefWithAGene, dnaId2list.size()));
    }
}
