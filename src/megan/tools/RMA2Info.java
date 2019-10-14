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

import jloda.graph.Node;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.Name2IdMap;
import megan.core.Document;
import megan.data.IClassificationBlock;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.dialogs.export.CSVExportFViewer;
import megan.main.Megan6;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.*;
import java.util.*;

/**
 * provides info on a RMA files
 * Daniel Huson, 11.2016
 */
public class RMA2Info {
    /**
     * RMA 2 info
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("RMA2Info");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new RMA2Info()).run(args);
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
    private void run(String[] args) throws UsageException, IOException, ClassNotFoundException, CanceledException {
        final ArgsOptions options = new ArgsOptions(args, this, "Analyses an RMA file");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output");
        final String daaFile = options.getOptionMandatory("-i", "in", "Input RMA file", "");
        final String outputFile = options.getOption("-o", "out", "Output file or '-' for stdout", "-");

        options.comment("Commands");
        final boolean listGeneralInfo = options.getOption("-l", "list", "List general info about file", false);
        final boolean listMoreStuff = options.getOption("-m", "listMore", "List more info about file (if meganized)", false);

        final Set<String> listClass2Count = new HashSet<>(options.getOption("-c2c", "class2count", "List class to count for named classification(s) (Possible values: " + Basic.toString(ClassificationManager.getAllSupportedClassifications(), " ") + ")", new ArrayList<>()));
        final Set<String> listRead2Class = new HashSet<>(options.getOption("-r2c", "read2class", "List read to class assignments for named classification(s) (Possible values: " + Basic.toString(ClassificationManager.getAllSupportedClassifications(), " ") + ")", new ArrayList<>()));
        final boolean reportNames = options.getOption("-n", "names", "Report class names rather than class Id numbers", false);
        final boolean reportPaths = options.getOption("-p", "paths", "Report class paths rather than class Id numbers", false);
        final boolean prefixRank = options.getOption("-r", "ranks", "When reporting taxonomy, report taxonomic rank using single letter (K for Kingdom, P for Phylum etc)", false);
        final boolean majorRanksOnly = options.getOption("-mro", "majorRanksOnly", "Only use major taxonomic ranks", false);
        final boolean bacteriaOnly = options.getOption("-bo", "bacteriaOnly", "Only report bacterial reads and counts in taxonomic report", false);
        final boolean viralOnly = options.getOption("-vo", "virusOnly", "Only report viral reads and counts in taxonomic report", false);
        final boolean ignoreUnassigned = options.getOption("-u", "ignoreUnassigned", "Don't report on reads that are unassigned", true);

        final String extractSummaryFile = options.getOption("-es", "extractSummaryFile", "Output a MEGAN summary file (contains all classifications, but no reads or alignments", "");

        options.done();

        final int taxonomyRoot;
        if (bacteriaOnly && viralOnly)
            throw new UsageException("Please specify only one of -bo and -vo");
        else if (bacteriaOnly)
            taxonomyRoot = TaxonomyData.BACTERIA_ID;
        else if (viralOnly)
            taxonomyRoot = TaxonomyData.VIRUSES_ID;
        else
            taxonomyRoot = 0; // means no root set

        final Document doc = new Document();
        doc.getMeganFile().setFileFromExistingFile(daaFile, true);
        if (!doc.getMeganFile().isRMA2File() && !doc.getMeganFile().isRMA3File() && !doc.getMeganFile().isRMA6File())
            throw new IOException("Incorrect file type: " + doc.getMeganFile().getFileType());
        doc.loadMeganFile();

        try (Writer outs = (outputFile.equals("-") ? new BufferedWriter(new OutputStreamWriter(System.out)) : new FileWriter(FileDescriptor.out))) {
            if (listGeneralInfo || listMoreStuff) {
                final IConnector connector = doc.getConnector();
                outs.write(String.format("# Number of reads:   %,d\n", doc.getNumberOfReads()));
                outs.write(String.format("# Number of matches: %,d\n", connector.getNumberOfMatches()));
                outs.write(String.format("# Alignment mode:  %s\n", doc.getDataTable().getBlastMode()));

                outs.write("# Classifications:");
                for (String classificationName : connector.getAllClassificationNames()) {
                    if (ClassificationManager.getAllSupportedClassifications().contains(classificationName)) {
                        outs.write(" " + classificationName);
                    }
                }
                outs.write("\n");

                if (listMoreStuff) {
                    outs.write("# Summary:\n");
                    outs.write(doc.getDataTable().getSummary().replaceAll("^", "## ").replaceAll("\n", "\n## ") + "\n");
                }
            }
            if (listClass2Count.size() > 0 || listRead2Class.size() > 0) {
                reportFileContent(doc, listGeneralInfo, listMoreStuff, reportPaths, reportNames, prefixRank, ignoreUnassigned, majorRanksOnly, listClass2Count, listRead2Class, taxonomyRoot, outs);
            }
        }
        if (extractSummaryFile.length() > 0) {
            try (Writer w = new FileWriter(extractSummaryFile)) {
                doc.getDataTable().write(w);
                doc.getSampleAttributeTable().write(w, false, true);
            }
        }
    }

    /**
     * report the file content
     *
     * @param doc
     * @param listGeneralInfo
     * @param listMoreStuff
     * @param reportPaths
     * @param reportNames
     * @param prefixRank
     * @param ignoreUnassigned
     * @param majorRanksOnly
     * @param listClass2Count
     * @param listRead2Class
     * @param taxonomyRoot     if set, only report taxa on or below this node
     * @param outs
     * @throws IOException
     */
    public static void reportFileContent(Document doc, boolean listGeneralInfo, boolean listMoreStuff, boolean reportPaths, boolean reportNames,
                                         boolean prefixRank, boolean ignoreUnassigned, boolean majorRanksOnly, Collection<String> listClass2Count,
                                         Collection<String> listRead2Class, int taxonomyRoot, Writer outs) throws IOException {
        final IConnector connector = doc.getConnector();

        final Map<String, Name2IdMap> classification2NameMap = new HashMap<>();
        final Set<String> availableClassificationNames = new HashSet<>();

        for (String classificationName : connector.getAllClassificationNames()) {
            if (ClassificationManager.getAllSupportedClassifications().contains(classificationName)) {
                availableClassificationNames.add(classificationName);
            }
        }

        ClassificationFullTree taxonomyTree = null;

        for (String classificationName : listClass2Count) {
            if (availableClassificationNames.contains(classificationName)) {
                if (listGeneralInfo || listMoreStuff)
                    outs.write("# Class to count for '" + classificationName + "':\n");

                if (!availableClassificationNames.contains(classificationName))
                    throw new IOException("Classification '" + classificationName + "' not found in file, available: " + Basic.toString(availableClassificationNames, " "));

                final boolean isTaxonomy = (classificationName.equals(Classification.Taxonomy));

                final Name2IdMap name2IdMap;
                if (isTaxonomy && reportPaths) {
                    ClassificationManager.ensureTreeIsLoaded(Classification.Taxonomy);
                    name2IdMap = null;
                } else if (reportNames) {
                    name2IdMap = new Name2IdMap();
                    name2IdMap.loadFromFile((classificationName.equals(Classification.Taxonomy) ? "ncbi" : classificationName.toLowerCase()) + ".map");
                    classification2NameMap.put(classificationName, name2IdMap);
                } else {
                    name2IdMap = null;
                }
                if (isTaxonomy && prefixRank) {
                    ClassificationManager.ensureTreeIsLoaded(Classification.Taxonomy);
                }
                if (isTaxonomy && taxonomyRoot > 0) {
                    taxonomyTree = ClassificationManager.get(Classification.Taxonomy, true).getFullTree();
                }

                final IClassificationBlock classificationBlock = connector.getClassificationBlock(classificationName);
                if (isTaxonomy) {
                    final Map<Integer, Float> taxId2count = new HashMap<>();
                    if (!majorRanksOnly) {
                        for (int taxId : classificationBlock.getKeySet()) {
                            if (taxonomyRoot == 0 || isDescendant(Objects.requireNonNull(taxonomyTree), taxId, taxonomyRoot))
                                taxId2count.put(taxId, classificationBlock.getWeightedSum(taxId));
                        }
                    } else { // major ranks only
                        for (int taxId : classificationBlock.getKeySet()) {
                            if (taxonomyRoot == 0 || isDescendant(Objects.requireNonNull(taxonomyTree), taxId, taxonomyRoot)) {
                                int classId = TaxonomyData.getLowestAncestorWithMajorRank(taxId);
                                Float count = taxId2count.get(classId);
                                if (count == null)
                                    count = classificationBlock.getWeightedSum(taxId);
                                else
                                    count += classificationBlock.getWeightedSum(taxId);
                                taxId2count.put(classId, count);
                            }
                        }
                    }
                    for (Integer taxId : taxId2count.keySet()) {
                        if (taxId > 0 || !ignoreUnassigned) {
                            if (taxonomyRoot == 0 || isDescendant(Objects.requireNonNull(taxonomyTree), taxId, taxonomyRoot)) {
                                final String className;
                                if (reportPaths) {
                                    className = TaxonomyData.getPathOrId(taxId, majorRanksOnly);
                                } else if (name2IdMap == null || name2IdMap.get(taxId) == null)
                                    className = "" + taxId;
                                else
                                    className = name2IdMap.get(taxId);
                                if (prefixRank) {
                                    int rank = TaxonomyData.getTaxonomicRank(taxId);
                                    String rankLabel = null;
                                    if (TaxonomicLevels.isMajorRank(rank))
                                        rankLabel = TaxonomicLevels.getName(rank);
                                    if (rankLabel == null || rankLabel.length() == 0)
                                        rankLabel = "-";
                                    outs.write(rankLabel.charAt(0) + "\t");
                                }
                                outs.write(className + "\t" + taxId2count.get(taxId) + "\n");
                            }
                        }
                    }

                } else { // not taxonomy
                    if (reportPaths) {
                        final Classification classification = ClassificationManager.get(classificationName, true);

                        for (Integer classId : classificationBlock.getKeySet()) {
                            final Set<Node> nodes = classification.getFullTree().getNodes(classId);
                            if (nodes != null) {
                                for (Node v : nodes) {
                                    String label = CSVExportFViewer.getPath(classification, v);
                                    outs.write(label + "\t" + classificationBlock.getWeightedSum(classId) + "\n");
                                }
                            } else {
                                outs.write("Class " + classId + "\t" + classificationBlock.getWeightedSum(classId) + "\n");
                            }
                        }
                    } else {
                        for (Integer classId : classificationBlock.getKeySet()) {
                            if (classId > 0 || !ignoreUnassigned) {
                                final String className;
                                if (name2IdMap == null || name2IdMap.get(classId) == null)
                                    className = "" + classId;
                                else
                                    className = name2IdMap.get(classId);
                                outs.write(className + "\t" + classificationBlock.getWeightedSum(classId) + "\n");
                            }
                        }
                    }
                }
            }
        }

        for (String classificationName : listRead2Class) {
            if (availableClassificationNames.contains(classificationName)) {

                if (listGeneralInfo || listMoreStuff)
                    outs.write("# Reads to class for '" + classificationName + "':\n");

                if (!availableClassificationNames.contains(classificationName))
                    throw new IOException("Classification '" + classificationName + "' not found in file, available: " + Basic.toString(availableClassificationNames, " "));

                final boolean isTaxonomy = (classificationName.equals(Classification.Taxonomy));

                final Name2IdMap name2IdMap;
                final Classification classification;
                if (reportPaths) {
                    classification = ClassificationManager.get(classificationName, true);
                    name2IdMap = null;
                } else if (reportNames) {
                    if (classification2NameMap.containsKey(classificationName))
                        name2IdMap = classification2NameMap.get(classificationName);
                    else {
                        name2IdMap = new Name2IdMap();
                        name2IdMap.loadFromFile((classificationName.equals(Classification.Taxonomy) ? "ncbi" : classificationName.toLowerCase()) + ".map");
                        classification2NameMap.put(classificationName, name2IdMap);
                    }
                    classification = null;
                } else {
                    name2IdMap = null;
                    classification = null;
                }
                if (isTaxonomy && prefixRank) {
                    ClassificationManager.ensureTreeIsLoaded(Classification.Taxonomy);
                }
                if (isTaxonomy && taxonomyRoot > 0) {
                    taxonomyTree = ClassificationManager.get(Classification.Taxonomy, true).getFullTree();
                }

                final Set<Integer> ids = new TreeSet<>(connector.getClassificationBlock(classificationName).getKeySet());

                for (Integer classId : ids) {
                    if (isTaxonomy && !(taxonomyRoot == 0 || isDescendant(Objects.requireNonNull(taxonomyTree), classId, taxonomyRoot)))
                        continue;

                    if (classId > 0 || !ignoreUnassigned) {
                        try (IReadBlockIterator it = connector.getReadsIterator(classificationName, classId, 0, 10, true, false)) {
                            while (it.hasNext()) {
                                final IReadBlock readBlock = it.next();
                                final String className;

                                if (isTaxonomy) {
                                    if (majorRanksOnly)
                                        classId = TaxonomyData.getLowestAncestorWithMajorRank(classId);

                                    if (reportPaths) {
                                        className = TaxonomyData.getPathOrId(classId, majorRanksOnly);
                                    } else if (name2IdMap == null || name2IdMap.get(classId) == null)
                                        className = "" + classId;

                                    else
                                        className = name2IdMap.get(classId);
                                    if (prefixRank) {
                                        int rank = TaxonomyData.getTaxonomicRank(classId);
                                        String rankLabel = TaxonomicLevels.getName(rank);
                                        if (rankLabel == null || rankLabel.length() == 0)
                                            rankLabel = "?";
                                        outs.write(readBlock.getReadName() + "\t" + rankLabel.charAt(0) + "\t" + className + "\n");
                                    } else
                                        outs.write(readBlock.getReadName() + "\t" + className + "\n");

                                } else {
                                    if (reportPaths) {
                                        Collection<Node> nodes = classification.getFullTree().getNodes(classId);
                                        if (nodes != null) {
                                            for (Node v : nodes) {
                                                String label = CSVExportFViewer.getPath(classification, v);
                                                outs.write(readBlock.getReadName() + "\t" + label + "\n");
                                            }
                                        }
                                    } else {
                                        if (name2IdMap == null || name2IdMap.get(classId) == null)
                                            className = "" + classId;
                                        else
                                            className = name2IdMap.get(classId);
                                        outs.write(readBlock.getReadName() + "\t" + className + "\n");
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * determine whether given taxon is ancestor of one of the named taxa
     *
     * @param taxonomy
     * @param taxId
     * @param ancestorIds
     * @return true, if is ancestor
     */
    private static boolean isDescendant(ClassificationFullTree taxonomy, int taxId, int... ancestorIds) {
        Node v = taxonomy.getANode(taxId);
        while (true) {
            for (int id : ancestorIds)
                if ((Integer) v.getInfo() == id)
                    return true;
                else if (v.getInDegree() > 0)
                    v = v.getFirstInEdge().getSource();
                else
                    return false;
        }
    }
}
