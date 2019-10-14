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
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.core.MeganFile;
import megan.data.*;
import megan.main.Megan6;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CompareProteinAlignments {
    /**
     * compares protein alignments
     *
     * @param args
     * @throws jloda.util.UsageException
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("Compare Protein Alignments");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            long start = System.currentTimeMillis();
            (new CompareProteinAlignments()).run(args);
            System.err.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run the program
     *
     * @param args
     */
    private void run(String[] args) throws Exception {
        final ArgsOptions options = new ArgsOptions(args, this, "Compares protein alignments for different analyses of the same sequences");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and output");
        final String[] inputFiles = options.getOptionMandatory("-i", "Input DAA or RMA files", "Input files", new String[0]);
        final String outputFileName = options.getOption("-o", "output", "Output file (stdout ok)", "");

        options.comment("Options");
        final NameNormalizer normalizer = new NameNormalizer(options.getOption("-e", "nameEdit", "Command A/B applied as replaceAll(A,B) to all read/contig names", ""));
        final boolean onlyCompareDominatingMatches = options.getOption("-d", "dominatingOnly", "Compare only dominating matches", false);
        options.done();

        if (inputFiles.length < 2)
            throw new UsageException("--input '" + Basic.toString(inputFiles, " ") + "': must specify at least 2 input files");

        if (onlyCompareDominatingMatches)
            throw new UsageException("--dominatingOnly: not implemented");

        final Writer w = new BufferedWriter(outputFileName.equalsIgnoreCase("stdout") ? new OutputStreamWriter(System.out) : new FileWriter(outputFileName));
        try {
            w.write("# " + new ComparisonResult().getFormatString() + "\n");

            for (int i = 0; i < inputFiles.length; i++) {
                final MeganFile file1 = new MeganFile();
                file1.setFileFromExistingFile(inputFiles[i], true);
                final IConnector connector1 = file1.getConnector();

                for (int j = i + 1; j < inputFiles.length; j++) {
                    final MeganFile file2 = new MeganFile();
                    file2.setFileFromExistingFile(inputFiles[j], true);
                    final IConnector connector2 = file2.getConnector();

                    final Map<String, Long> name2Uid = getName2Uid(connector2, normalizer);
                    final IReadBlockGetter getter2 = connector2.getReadBlockGetter(0, 10, true, true);
                    final ComparisonResult total = new ComparisonResult("total", 0, 0);

                    int count = 0;
                    try (IReadBlockIterator it = connector1.getAllReadsIterator(0, 10, true, true);
                         ProgressPercentage progress = new ProgressPercentage("Comparing files " + inputFiles[i] + " and " + inputFiles[j], it.getMaximumProgress())) {
                        w.write("# Comparison " + Basic.getFileNameWithoutPath(inputFiles[i]) + " and " + Basic.getFileNameWithoutPath(inputFiles[j]) + ":\n");
                        while (it.hasNext()) {
                            final IReadBlock readBlock1 = it.next();

                            final String name1 = normalizer.apply(readBlock1.getReadName());
                            final Long uid2 = name2Uid.get(name1);
                            if (uid2 == null)
                                throw new IOException("Read '" + name1 + "' not found, uid=null");

                            final IReadBlock readBlock2 = getter2.getReadBlock(uid2);
                            final ComparisonResult comparison = computeComparison(normalizer.apply(name1), readBlock1, readBlock2);
                            total.add(comparison);
                            w.write(comparison + "\n");
                            progress.setProgress(it.getProgress());
                            count++;
                        }
                    }
                    if (count > 1)
                        w.write(total + "\n");
                }
            }
            w.flush();
        } finally {
            if (!outputFileName.equalsIgnoreCase("stdout"))
                w.close();
        }
    }

    private ComparisonResult computeComparison(final String name, IReadBlock readBlock1, IReadBlock readBlock2) {
        final Map<String, ArrayList<IMatchBlock>> accession2Matches1 = computeAccession2Matches(readBlock1);
        final Map<String, ArrayList<IMatchBlock>> accession2Matches2 = computeAccession2Matches(readBlock2);

        final ComparisonResult comparison = new ComparisonResult(name, readBlock1.getReadLength(), readBlock2.getReadLength());
        comparison.coveredInA = computeIntervalTreeOnQuery(getAllMatches(readBlock1)).getCovered();
        comparison.coveredInB = computeIntervalTreeOnQuery(getAllMatches(readBlock2)).getCovered();

        for (String accession : accession2Matches1.keySet()) {
            final ArrayList<IMatchBlock> matches1 = accession2Matches1.get(accession);

            if (!accession2Matches2.containsKey(accession)) {
                comparison.matchesOnlyInA += matches1.size();
                comparison.alignedAAOnlyInA += computeAlignedBases(matches1);
            } else {
                final IntervalTree<IMatchBlock> intervalTree1 = computeIntervalTreeOnReference(matches1);

                final ArrayList<IMatchBlock> matches2 = accession2Matches2.get(accession);
                final IntervalTree<IMatchBlock> intervalTree2 = computeIntervalTreeOnReference(matches2);

                {
                    int[] count = computeOnlyInFirst(matches1, intervalTree2);
                    comparison.matchesOnlyInA += count[0];
                    comparison.alignedAAOnlyInA += count[1];
                }

                {
                    int[] count = computeLongerInFirst(matches1, intervalTree2);
                    comparison.matchesLongerInA += count[0];
                    comparison.alignedAALongerInA += count[1];
                    comparison.diffAALongerInA += count[2];

                }
                {
                    int[] count = computeLongerInFirst(matches2, intervalTree1);
                    comparison.matchesLongerInB += count[0];
                    comparison.alignedAALongerInB += count[1];
                    comparison.diffAALongerInB += count[2];
                }
                {
                    int[] count = computeSameInBoth(matches2, intervalTree1);
                    comparison.matchesInBoth += count[0];
                    comparison.alignedAAInBoth += count[1];
                }
            }
        }
        for (String accession : accession2Matches2.keySet()) {
            final ArrayList<IMatchBlock> matches2 = accession2Matches2.get(accession);
            if (!accession2Matches1.containsKey(accession)) {
                comparison.matchesOnlyInB += matches2.size();
                comparison.alignedAAOnlyInB += computeAlignedBases(matches2);
            }

        }
        return comparison;
    }

    private ArrayList<IMatchBlock> getAllMatches(IReadBlock readBlock) {
        final ArrayList<IMatchBlock> list = new ArrayList<>(readBlock.getNumberOfAvailableMatchBlocks());
        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++)
            list.add(readBlock.getMatchBlock(i));
        return list;
    }

    /**
     * computes the number of matches not present in the tree
     *
     * @param matches
     * @param tree
     * @return number of alignments and bases
     */
    private static int[] computeOnlyInFirst(ArrayList<IMatchBlock> matches, IntervalTree<IMatchBlock> tree) {
        int[] count = {0, 0};
        for (IMatchBlock matchBlock : matches) {
            int a = getSubjStart(matchBlock);
            int b = getSubjEnd(matchBlock);
            if (tree.getIntervals(a, b).size() == 0) {
                count[0]++;
                count[1] += Math.abs(a - b) + 1;
            }
        }
        return count;
    }

    /**
     * get the number of matches longer than in the tree
     *
     * @param matches
     * @param tree
     * @return number of alignments and bases
     */
    private static int[] computeLongerInFirst(ArrayList<IMatchBlock> matches, IntervalTree<IMatchBlock> tree) {
        int[] count = {0, 0, 0};
        for (IMatchBlock matchBlock : matches) {
            int a = getSubjStart(matchBlock);
            int b = getSubjEnd(matchBlock);

            final Interval<IMatchBlock>[] overlappers = tree.getIntervalsSortedByDecreasingIntersectionLength(a, b);
            if (overlappers.length > 0) {
                final int diff = Math.abs(a - b) + 1 - overlappers[0].length();
                if (diff > 0) {
                    count[0]++;
                    count[1] += Math.abs(a - b) + 1;
                    count[2] += diff;
                }
            }
        }
        return count;
    }

    /**
     * get the number of matches same  in the tree
     *
     * @param matches
     * @param tree
     * @return number of alignments and bases
     */
    private static int[] computeSameInBoth(ArrayList<IMatchBlock> matches, IntervalTree<IMatchBlock> tree) {
        int[] count = {0, 0};
        for (IMatchBlock matchBlock : matches) {
            int a = getSubjStart(matchBlock);
            int b = getSubjEnd(matchBlock);

            final Interval<IMatchBlock>[] overlappers = tree.getIntervalsSortedByDecreasingIntersectionLength(a, b);
            if (overlappers.length > 0) {
                final int diff = Math.abs(a - b) + 1 - overlappers[0].length();
                if (diff == 0) {
                    count[0]++;
                    count[1] += Math.abs(a - b) + 1;
                }
            }
        }
        return count;
    }

    /**
     * compute the interval tree for a set of matches
     *
     * @param matches
     * @return interval tree
     */
    private static IntervalTree<IMatchBlock> computeIntervalTreeOnReference(ArrayList<IMatchBlock> matches) {
        final IntervalTree<IMatchBlock> tree = new IntervalTree<>();
        for (IMatchBlock matchBlock : matches) {
            tree.add(getSubjStart(matchBlock), getSubjEnd(matchBlock), matchBlock);
        }
        return tree;
    }

    /**
     * compute the interval tree for a set of matches
     *
     * @param matches
     * @return interval tree
     */
    private static IntervalTree<IMatchBlock> computeIntervalTreeOnQuery(ArrayList<IMatchBlock> matches) {
        final IntervalTree<IMatchBlock> tree = new IntervalTree<>();
        for (IMatchBlock matchBlock : matches) {
            tree.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
        }
        return tree;
    }

    /**
     * compute the number of aligned bases in this list of matches
     *
     * @param matches
     * @return
     */
    private static int computeAlignedBases(ArrayList<IMatchBlock> matches) {
        int count = 0;
        for (IMatchBlock matchBlock : matches) {
            count += Math.abs(getSubjStart(matchBlock) - getSubjEnd(matchBlock)) + 1;
        }
        return count;
    }

    /**
     * get the subject start
     *
     * @param matchBlock
     * @return start 0 0
     */
    private static int getSubjStart(IMatchBlock matchBlock) {
        final String text = matchBlock.getText();
        int pos = text.indexOf("Sbjct");
        return (pos != -1 ? Basic.parseInt(text.substring(pos + 5)) : 0);
    }

    /**
     * get the subject end
     *
     * @param matchBlock
     * @return end or 0
     */
    private static int getSubjEnd(IMatchBlock matchBlock) {
        final String text = matchBlock.getText();
        int pos = text.lastIndexOf("Sbjct");
        if (pos == -1)
            return 0;
        pos = Basic.skipNonWhiteSpace(text, pos); // Sjbct:
        pos = Basic.skipWhiteSpace(text, pos);
        pos = Basic.skipNonWhiteSpace(text, pos);  // number
        pos = Basic.skipNonWhiteSpace(text, pos);
        pos = Basic.skipWhiteSpace(text, pos); // sequence
        pos = Basic.skipNonWhiteSpace(text, pos);

        if (pos >= text.length())
            return 0;
        return Basic.parseInt(text.substring(pos));
    }

    /**
     * compute accession to matches mapping
     *
     * @param readBlock
     * @return mapping
     */
    private static Map<String, ArrayList<IMatchBlock>> computeAccession2Matches(IReadBlock readBlock) {
        final Map<String, ArrayList<IMatchBlock>> map = new HashMap<>();
        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            final String accession = matchBlock.getTextFirstWord();
            ArrayList<IMatchBlock> matches = map.computeIfAbsent(accession, k -> new ArrayList<>());
            matches.add(matchBlock);
        }
        return map;
    }

    /**
     * computes a  name to Uid
     *
     * @param connector
     * @param normalizer
     * @return mapping
     * @throws IOException
     */
    private Map<String, Long> getName2Uid(IConnector connector, final NameNormalizer normalizer) throws IOException {
        final Map<String, Long> name2uid = new HashMap<>();
        for (IReadBlockIterator it = connector.getAllReadsIterator(0, 10, false, false); it.hasNext(); ) {
            final IReadBlock readBlock = it.next();
            name2uid.put(normalizer.apply(readBlock.getReadName()), readBlock.getUId());
        }
        return name2uid;
    }

    /**
     * reports the result of a comparison
     */
    public static class ComparisonResult {
        String name;

        int lengthA;
        int coveredInA;

        int matchesOnlyInA;
        int alignedAAOnlyInA;

        int matchesLongerInA;
        int alignedAALongerInA;
        int diffAALongerInA;

        int matchesInBoth;
        int alignedAAInBoth;

        int matchesLongerInB;
        int alignedAALongerInB;
        int diffAALongerInB;

        int matchesOnlyInB;
        int alignedAAOnlyInB;

        int lengthB;
        int coveredInB;

        public ComparisonResult() {
        }

        public ComparisonResult(String name, int lengthA, int lengthB) {
            this.name = name;
            this.lengthA = lengthA;
            this.lengthB = lengthB;
        }

        void add(ComparisonResult that) {
            this.lengthA += that.lengthA;

            this.coveredInA += that.coveredInA;

            this.matchesOnlyInA += that.matchesOnlyInA;
            this.alignedAAOnlyInA += that.alignedAAOnlyInA;

            this.matchesLongerInA += that.matchesLongerInA;
            this.alignedAALongerInA += that.alignedAALongerInA;
            this.diffAALongerInA += that.diffAALongerInA;

            this.matchesInBoth += that.matchesInBoth;
            this.alignedAAInBoth += that.alignedAAInBoth;

            this.matchesLongerInB += that.matchesLongerInB;
            this.alignedAALongerInB += that.alignedAALongerInB;
            this.diffAALongerInB += that.diffAALongerInB;

            this.matchesOnlyInB += that.matchesOnlyInB;
            this.alignedAAOnlyInB += that.alignedAAOnlyInB;

            this.lengthB += that.lengthB;
            this.coveredInB = that.coveredInB;
        }

        public String toString() {
            final int totalAA = alignedAAOnlyInA + alignedAALongerInA + alignedAAInBoth + alignedAALongerInB + alignedAAOnlyInB;

            return String.format("%s\t%,d (%,d %.1f%%) %d (%,d %.1f%%) %,d (%,d +%d, %.1f%%) %,d (%,d %.1f%%) %,d (%,d +%,d %.1f%%) %,d (%,d  %.1f%%) %,d (%,d %.1f%%)",
                    name, lengthA,
                    coveredInA, (100.0 * coveredInA) / lengthA,
                    matchesOnlyInA, alignedAAOnlyInA,
                    (100.0 * alignedAAOnlyInA) / totalAA,
                    matchesLongerInA, alignedAALongerInA, diffAALongerInA,
                    (100.0 * alignedAALongerInA) / totalAA,
                    matchesInBoth, alignedAAInBoth,
                    (100.0 * alignedAAInBoth) / totalAA,
                    matchesLongerInB, alignedAALongerInB, diffAALongerInB,
                    (100.0 * alignedAALongerInB) / totalAA,
                    matchesOnlyInB, alignedAAOnlyInB,
                    (100.0 * alignedAAOnlyInB) / totalAA,
                    lengthB,
                    coveredInB, (100.0 * coveredInB) / lengthB);
        }

        String getFormatString() {
            return "name length-A (covered-A %) only-A (aa %) longer-A (aa + %) both (aa %) longer-B (aa + %) only-B (a %) length-B (covered-B %)";
        }
    }

}
