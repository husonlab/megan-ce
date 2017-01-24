/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.util.*;
import megan.classification.data.Name2IdMap;
import megan.core.Document;
import megan.data.IClassificationBlock;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.*;
import java.util.*;

/**
 * provides info on a RMA files
 * Daniel Huson, 11.2016
 */
public class RMA2Info {
    /**
     * DAA 2 info
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
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
    public void run(String[] args) throws UsageException, IOException, ClassNotFoundException, CanceledException {
        final ArgsOptions options = new ArgsOptions(args, this, "Analyses an RMA file");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2017 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output");
        final String daaFile = options.getOptionMandatory("-i", "in", "Input RMA file", "");
        final String outputFile = options.getOption("-o", "out", "Output file or '-' for stdout", "-");

        options.comment("Commands");
        final boolean listGeneralInfo = options.getOption("-l", "list", "List general info about file", false);
        final boolean listMoreStuff = options.getOption("-m", "listMore", "List more info about file (if meganized)", false);

        final Set<String> listClass2Count = new HashSet<>(options.getOption("-c2c", "class2count", "List class to count for named classification(s)", new ArrayList<String>()));
        final Set<String> listRead2Class = new HashSet<>(options.getOption("-r2c", "read2class", "List read to class assignments for named classification(s)", new ArrayList<String>()));
        final boolean reportNames = options.getOption("-n", "names", "Report class names rather than class Id numbers", false);
        final boolean ignoreUnassigned = options.getOption("-u", "ignoreUnassigned", "Don't report on reads that are unassigned", true);

        options.done();

        final Document doc = new Document();
        doc.getMeganFile().setFileFromExistingFile(daaFile, true);
        if (!doc.getMeganFile().isRMA2File() && !doc.getMeganFile().isRMA3File() && !doc.getMeganFile().isRMA6File())
            throw new IOException("Incorrect file type: " + doc.getMeganFile().getFileType());
        doc.loadMeganFile();
        final IConnector connector = doc.getMeganFile().getDataConnector();

        try (Writer outs = (outputFile.equals("-") ? new BufferedWriter(new OutputStreamWriter(System.out)) : new FileWriter(FileDescriptor.out))) {
            if (listGeneralInfo || listMoreStuff) {
                outs.write(String.format("# Number of reads:   %,d\n", doc.getNumberOfReads()));
                outs.write(String.format("# Number of matches: %,d\n", connector.getNumberOfMatches()));
                outs.write(String.format("# Alignment mode:  %s\n", doc.getDataTable().getBlastMode()));

                outs.write("# Classifications:");
                for (String classification : connector.getAllClassificationNames()) {
                    outs.write(" " + classification);
                }
                outs.write("\n");

                if (listMoreStuff) {
                    outs.write("# Summary:\n");
                    outs.write(doc.getDataTable().getSummary().replaceAll("^", "## ").replaceAll("\n", "\n## ") + "\n");
                }
            }

            final Map<String, Name2IdMap> classification2NameMap = new HashMap<>();
            final Set<String> availableClassificationNames = new HashSet<>(Arrays.asList(connector.getAllClassificationNames()));

            for (String classification : listClass2Count) {
                if (listGeneralInfo || listMoreStuff)
                    outs.write("# Class to count for '" + classification + "':\n");

                if (!availableClassificationNames.contains(classification))
                    throw new IOException("Classification '" + classification + "' not found in file, available: " + Basic.toString(availableClassificationNames, " "));

                final Name2IdMap name2IdMap;
                if (reportNames) {
                    name2IdMap = new Name2IdMap();
                    name2IdMap.loadFromFile(classification.toLowerCase() + ".map");
                    classification2NameMap.put(classification, name2IdMap);
                } else
                    name2IdMap = null;

                final Set<Integer> ids = new TreeSet<>();
                final IClassificationBlock classificationBlock = connector.getClassificationBlock(classification);
                ids.addAll(classificationBlock.getKeySet());
                for (Integer classId : ids) {
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

            for (String classification : listRead2Class) {
                if (listGeneralInfo || listMoreStuff)
                    outs.write("# Reads to class for '" + classification + "':\n");
                if (!availableClassificationNames.contains(classification))
                    throw new IOException("Classification '" + classification + "' not found in file, available: " + Basic.toString(availableClassificationNames, " "));

                final Name2IdMap name2IdMap;
                if (reportNames) {
                    if (classification2NameMap.containsKey(classification))
                        name2IdMap = classification2NameMap.get(classification);
                    else {
                        name2IdMap = new Name2IdMap();
                        name2IdMap.loadFromFile(classification.toLowerCase() + ".map");
                        classification2NameMap.put(classification, name2IdMap);
                    }
                } else
                    name2IdMap = null;

                final Set<Integer> ids = new TreeSet<>();
                ids.addAll(connector.getClassificationBlock(classification).getKeySet());
                for (Integer classId : ids) {
                    if (classId > 0 || !ignoreUnassigned) {
                        final IReadBlockIterator it = connector.getReadsIterator(classification, classId, 0, 10, true, false);
                        while (it.hasNext()) {
                            final IReadBlock readBlock = it.next();
                            final String className;
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
