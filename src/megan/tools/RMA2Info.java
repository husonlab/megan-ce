/*
 * RMA2Info.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import megan.dialogs.export.CSVExportCViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.*;
import java.util.*;
import java.util.function.Function;

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
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
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
     */
    private void run(String[] args) throws UsageException, IOException, CanceledException {
        final ArgsOptions options = new ArgsOptions(args, this, "Analyses an RMA file");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2022 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output");
        final String daaFile = options.getOptionMandatory("-i", "in", "Input RMA file", "");
        final String outputFile = options.getOption("-o", "out", "Output file (stdout or .gz ok)", "stdout");

        options.comment("Commands");
		final boolean listGeneralInfo = options.getOption("-l", "list", "List general info about file", false);
		final boolean listMoreStuff = options.getOption("-m", "listMore", "List more info about file (if meganized)", false);

		final Set<String> listClass2Count = new HashSet<>(options.getOption("-c2c", "class2count", "List class to count for named classification(s) (Possible values: " + StringUtils.toString(ClassificationManager.getAllSupportedClassifications(), " ") + ")", new ArrayList<>()));
		final Set<String> listRead2Class = new HashSet<>(options.getOption("-r2c", "read2class", "List read to class assignments for named classification(s) (Possible values: " + StringUtils.toString(ClassificationManager.getAllSupportedClassifications(), " ") + ")", new ArrayList<>()));
		final boolean reportNames = options.getOption("-n", "names", "Report class names rather than class Id numbers", false);
		final boolean reportPaths = options.getOption("-p", "paths", "Report class paths rather than class Id numbers", false);

		final boolean prefixRank = options.getOption("-r", "ranks", "When reporting taxonomy, report taxonomic rank using single letter (K for Kingdom, P for Phylum etc)", false);
		final boolean majorRanksOnly = options.getOption("-mro", "majorRanksOnly", "Only use major taxonomic ranks", false);
		final boolean bacteriaOnly = options.getOption("-bo", "bacteriaOnly", "Only report bacterial reads and counts in taxonomic report", false);
		final boolean viralOnly = options.getOption("-vo", "virusOnly", "Only report viral reads and counts in taxonomic report", false);
		final boolean ignoreUnassigned = options.getOption("-u", "ignoreUnassigned", "Don't report on reads that are unassigned", true);

		final boolean useSummarized = options.getOption("-s", "sum", "Use summarized rather than assigned counts when listing class to count", false);

        final String extractSummaryFile = options.getOption("-es", "extractSummaryFile", "Output a MEGAN summary file (contains all classifications, but no reads or alignments)", "");

        options.done();

        final int taxonomyRoot;
        if (bacteriaOnly && viralOnly)
            throw new UsageException("Please specify only one of -bo and -vo");
        else if (bacteriaOnly)
            taxonomyRoot = TaxonomyData.BACTERIA_ID;
        else if (viralOnly)
            taxonomyRoot = TaxonomyData.VIRUSES_ID;
        else
            taxonomyRoot = TaxonomyData.ROOT_ID; // means no root set

        final Document doc = new Document();
        doc.getMeganFile().setFileFromExistingFile(daaFile, true);
        if (!doc.getMeganFile().isRMA2File() && !doc.getMeganFile().isRMA3File() && !doc.getMeganFile().isRMA6File())
            throw new IOException("Incorrect file type: " + doc.getMeganFile().getFileType());
        doc.loadMeganFile();

		try (Writer outs = new BufferedWriter(new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile)))) {
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
            if (listClass2Count.size() > 0) {
                reportClass2Count(doc, listGeneralInfo, listMoreStuff, reportPaths, reportNames, prefixRank, ignoreUnassigned, majorRanksOnly, listClass2Count, taxonomyRoot,useSummarized, outs);
            }

            if (listRead2Class.size() > 0) {
                reportRead2Count(doc, listGeneralInfo, listMoreStuff, reportPaths, reportNames, prefixRank, ignoreUnassigned, majorRanksOnly, listRead2Class, taxonomyRoot, outs);
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
     * report class to count
     */
    public static void reportClass2Count(Document doc, boolean listGeneralInfo, boolean listMoreStuff, boolean reportPaths, boolean reportNames,
                                         boolean prefixRank, boolean ignoreUnassigned, boolean majorRanksOnly, Collection<String> classificationNames,
                                         int taxonomyRootId, boolean useSummarized, Writer writer) throws IOException {

        final var connector = doc.getConnector();

        final var availableClassificationNames = new HashSet<String>();

        for (var classificationName : connector.getAllClassificationNames()) {
            if (ClassificationManager.getAllSupportedClassifications().contains(classificationName)) {
                availableClassificationNames.add(classificationName);
            }
        }

        ClassificationFullTree taxonomyTree = null;

        for (var classificationName : classificationNames) {
            if (availableClassificationNames.contains(classificationName)) {
                if (listGeneralInfo || listMoreStuff)
                    writer.write("# Class to count for '" + classificationName + "':\n");

                if (!availableClassificationNames.contains(classificationName))
					throw new IOException("Classification '" + classificationName + "' not found in file, available: " + StringUtils.toString(availableClassificationNames, " "));

                final var isTaxonomy = (classificationName.equals(Classification.Taxonomy));

                final Name2IdMap name2IdMap;
                if (isTaxonomy && reportPaths) {
                    ClassificationManager.ensureTreeIsLoaded(Classification.Taxonomy);
                    name2IdMap = null;
                } else if (reportNames) {
                    name2IdMap = new Name2IdMap();
                    name2IdMap.loadFromFile((classificationName.equals(Classification.Taxonomy) ? "ncbi" : classificationName.toLowerCase()) + ".map");
                } else {
                    name2IdMap = null;
                }
                if (isTaxonomy && prefixRank) {
                    ClassificationManager.ensureTreeIsLoaded(Classification.Taxonomy);
                }
                if (isTaxonomy) {
                    taxonomyTree = ClassificationManager.get(Classification.Taxonomy, true).getFullTree();
                }

                final IClassificationBlock classificationBlock = connector.getClassificationBlock(classificationName);

                Function<Integer, Float> id2count;
                var ids = new TreeSet<Integer>();
                if (!useSummarized) {
                    id2count = classificationBlock::getWeightedSum;
                    ids.addAll(classificationBlock.getKeySet());
                } else {
                    ClassificationManager.ensureTreeIsLoaded(classificationName);
                    var tree = ClassificationManager.get(classificationName, true).getFullTree();
                    var id2summarized = new HashMap<Integer, Float>();
                    var root = (isTaxonomy? taxonomyTree.getANode(taxonomyRootId) : tree.getRoot());

                    tree.postorderTraversal(v -> {
						var summarized = classificationBlock.getWeightedSum((Integer) v.getInfo());
						for (var w : v.children()) {
							var id = (Integer) w.getInfo();
							if (id2summarized.containsKey(id))
								summarized += id2summarized.get(id);
						}
						if (summarized > 0) {
							var id = (Integer) v.getInfo();
							id2summarized.put(id, summarized);
						}
                    });
                    id2count = (id) -> id2summarized.getOrDefault(id, 0f);
                    ids.addAll(id2summarized.keySet());
                }

                if (isTaxonomy) {
                    final Function<Integer, Float> taxId2count;
                    if (!majorRanksOnly) {
                        taxId2count = id2count;
                    } else { // major ranks only
                        if (!useSummarized) {
                            var unused = new HashMap<Integer, Float>();
                            var map = new HashMap<Integer, Float>();
                            taxId2count = map::get;

							taxonomyTree.postorderTraversal(taxonomyTree.getANode(taxonomyRootId), v -> {
								var vid = (Integer) v.getInfo();
								var count = id2count.apply(vid);
								for (var w : v.children()) {
									var id = (Integer) w.getInfo();
									count += unused.getOrDefault(id, 0f);
								}
								if (count > 0) {
									if (vid.equals(taxonomyRootId) || TaxonomicLevels.isMajorRank(TaxonomyData.getTaxonomicRank(vid))) {
										map.put(vid, count);
									} else
                                        unused.put(vid, count);
                                }
                            });
                            ids.clear();
                            ids.addAll(map.keySet());
                        } else { // use summarized: remove any ids that are not at official rank
                            taxId2count = id2count;
                            var keep = new ArrayList<Integer>();
                            for (var id : ids) {
                                if (id.equals(taxonomyRootId) || TaxonomicLevels.isMajorRank(TaxonomyData.getTaxonomicRank(id)))
                                    keep.add(id);
                            }
                            ids.clear();
                            ids.addAll(keep);
                        }
                    }
                    for (Integer taxId : ids) {
                        if (taxId > 0 || !ignoreUnassigned) {
                            final String classLabel;
                            if (reportPaths) {
                                classLabel = TaxonomyData.getPathOrId(taxId, majorRanksOnly);
                            } else if (name2IdMap == null || name2IdMap.get(taxId) == null)
                                classLabel = "" + taxId;
                            else
                                classLabel = name2IdMap.get(taxId);
                            if (prefixRank) {
                                int rank = TaxonomyData.getTaxonomicRank(taxId);
                                String rankLabel = null;
                                if (TaxonomicLevels.isMajorRank(rank))
                                    rankLabel = TaxonomicLevels.getName(rank);
                                if (rankLabel == null || rankLabel.length() == 0)
                                    rankLabel = "-";
                                writer.write(rankLabel.charAt(0) + "\t");
                            }
                            writer.write(classLabel + "\t" + taxId2count.apply(taxId) + "\n");
                        }
                    }

                } else { // not taxonomy
                    if (reportPaths) {
                        final var classification = ClassificationManager.get(classificationName, true);

                        for (var classId : ids) {
                            final var nodes = classification.getFullTree().getNodes(classId);
                            if (nodes != null) {
                                for (var v : nodes) {
                                    String label = CSVExportCViewer.getPath(classification, v);
                                    writer.write(label + "\t" + id2count.apply(classId) + "\n");
                                }
                            } else {
                                writer.write("Class " + classId + "\t" + id2count.apply(classId) + "\n");
                            }
                        }
                    } else {
                        for (var classId : classificationBlock.getKeySet()) {
                            if (classId > 0 || !ignoreUnassigned) {
                                final String className;
                                if (name2IdMap == null || name2IdMap.get(classId) == null)
                                    className = "" + classId;
                                else
                                    className = name2IdMap.get(classId);
                                writer.write(className + "\t" + id2count.apply(classId) + "\n");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * report read to count
     */
    public static void reportRead2Count(Document doc, boolean listGeneralInfo, boolean listMoreStuff, boolean reportPaths, boolean reportNames,
                                        boolean prefixRank, boolean ignoreUnassigned, boolean majorRanksOnly,
                                        Collection<String> classificationNames, int taxonomyRoot, Writer w) throws IOException {
        final IConnector connector = doc.getConnector();

        final Map<String, Name2IdMap> classification2NameMap = new HashMap<>();
        final Set<String> availableClassificationNames = new HashSet<>();

        for (String classificationName : connector.getAllClassificationNames()) {
            if (ClassificationManager.getAllSupportedClassifications().contains(classificationName)) {
                availableClassificationNames.add(classificationName);
            }
        }

        ClassificationFullTree taxonomyTree = null;

        for (String classificationName : classificationNames) {
            if (availableClassificationNames.contains(classificationName)) {
                if (listGeneralInfo || listMoreStuff)
                    w.write("# Reads to class for '" + classificationName + "':\n");

                if (!availableClassificationNames.contains(classificationName))
					throw new IOException("Classification '" + classificationName + "' not found in file, available: " + StringUtils.toString(availableClassificationNames, " "));

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
                                        w.write(readBlock.getReadName() + "\t" + rankLabel.charAt(0) + "\t" + className + "\n");
                                    } else
                                        w.write(readBlock.getReadName() + "\t" + className + "\n");

                                } else {
                                    if (reportPaths) {
                                        Collection<Node> nodes = classification.getFullTree().getNodes(classId);
                                        if (nodes != null) {
                                            for (Node v : nodes) {
                                                String label = CSVExportCViewer.getPath(classification, v);
                                                w.write(readBlock.getReadName() + "\t" + label + "\n");
                                            }
                                        }
                                    } else {
                                        if (name2IdMap == null || name2IdMap.get(classId) == null)
                                            className = "" + classId;
                                        else
                                            className = name2IdMap.get(classId);
                                        w.write(readBlock.getReadName() + "\t" + className + "\n");
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
                if (v.getInfo()!=null && (Integer) v.getInfo() == id)
                    return true;
                else if (v.getInDegree() > 0)
                    v = v.getFirstInEdge().getSource();
                else
                    return false;
        }
    }
}
