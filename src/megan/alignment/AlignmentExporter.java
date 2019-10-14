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
package megan.alignment;

import jloda.util.*;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;
import megan.core.Document;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.rma2.MatchBlockRMA2;
import megan.rma2.ReadBlockRMA2;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * exports alignments to a set of files
 * Daniel Huson, 9.2011
 */
public class AlignmentExporter {
    private final Document doc;
    private final JFrame parent;

    private String classificationName;
    private int classId;
    private String className;

    private final Map<String, List<Pair<IReadBlock, IMatchBlock>>> reference2ReadMatchPairs = new HashMap<>();

    private final Set<String> usedReferences = new HashSet<>();
    private boolean useEachReferenceOnlyOnce = true;

    private boolean warned = false;
    private boolean overwrite = true;

    private boolean verbose = false;

    /**
     * constructor
     *
     * @param doc
     * @param parent
     */
    public AlignmentExporter(Document doc, JFrame parent) {
        this.doc = doc;
        this.parent = parent;
    }

    /**
     * load data for complete dataset
     *
     * @param progressListener
     */
    public void loadData(ProgressListener progressListener) throws CanceledException, IOException {
        className = "Total sample";

        int totalReads = 0;
        int totalReadsUsed = 0;

        reference2ReadMatchPairs.clear();

        Set<String> matchesSeenForGivenRead = new HashSet<>();
        progressListener.setSubtask("Processing total dataset");

        try (IReadBlockIterator it = doc.getConnector().getAllReadsIterator(doc.getMinScore(), doc.getMaxExpected(), true, true)) {
            progressListener.setMaximum(it.getMaximumProgress());
            progressListener.setProgress(0);

            while (it.hasNext()) {
                IReadBlock readBlock = it.next();
                totalReads++;
                boolean readUsed = false;

                for (IMatchBlock matchBlock : readBlock.getMatchBlocks()) {
                    if (matchBlock.getBitScore() >= doc.getMinScore() && matchBlock.getExpected() <= doc.getMaxExpected() &&
                            (matchBlock.getPercentIdentity() == 0 || matchBlock.getPercentIdentity() >= doc.getMinPercentIdentity())) {
                        String key = Basic.getFirstLine(matchBlock.getText());
                        if (!matchesSeenForGivenRead.contains(key)) {
                            matchesSeenForGivenRead.add(key);
                            List<Pair<IReadBlock, IMatchBlock>> pairs = reference2ReadMatchPairs.computeIfAbsent(key, k -> new LinkedList<>());
                            pairs.add(new Pair<>(readBlock, matchBlock));
                            readUsed = true;
                        }
                    }
                }
                matchesSeenForGivenRead.clear();
                progressListener.incrementProgress();
                if (readUsed)
                    totalReadsUsed++;
            }
        }
        System.err.println("Reads total: " + totalReads);
        System.err.println("Reads used:  " + totalReadsUsed);
        System.err.println("References:  " + reference2ReadMatchPairs.keySet().size());
    }

    /**
     * load data for given class
     *
     * @param classificationName
     * @param classId
     * @param progressListener
     */
    public void loadData(String classificationName, Integer classId, String name, boolean refSeqOnly, ProgressListener progressListener) throws CanceledException, IOException {
        this.classificationName = classificationName;
        this.classId = classId;
        this.className = name;

        int totalReads = 0;
        int totalReadsUsed = 0;

        reference2ReadMatchPairs.clear();

        Set<String> matchesSeenForGivenRead = new HashSet<>();
        progressListener.setSubtask("Processing '" + name + "'");

        try (IReadBlockIterator it = doc.getConnector().getReadsIterator(classificationName, classId, 0, 10, true, true)) {
            progressListener.setMaximum(it.getMaximumProgress());
            progressListener.setProgress(0);

            while (it.hasNext()) {
                IReadBlock readBlock = copy(it.next(), new String[]{classificationName});
                totalReads++;
                boolean readUsed = false;

                for (IMatchBlock matchBlock : readBlock.getMatchBlocks()) {
                    if (matchBlock.getBitScore() >= doc.getMinScore() && matchBlock.getExpected() <= doc.getMaxExpected() &&
                            (matchBlock.getPercentIdentity() == 0 || matchBlock.getPercentIdentity() >= doc.getMinPercentIdentity())) {
                        if (!refSeqOnly || (matchBlock.getRefSeqId() != null && matchBlock.getRefSeqId().length() > 0)) {
                            String key = Basic.getFirstLine(matchBlock.getText());
                            if (!matchesSeenForGivenRead.contains(key)) {
                                matchesSeenForGivenRead.add(key);

                                List<Pair<IReadBlock, IMatchBlock>> pairs = reference2ReadMatchPairs.computeIfAbsent(key, k -> new LinkedList<>());
                                pairs.add(new Pair<>(readBlock, matchBlock));
                                readUsed = true;
                            }
                        }
                    }
                }
                matchesSeenForGivenRead.clear();
                progressListener.incrementProgress();
                if (readUsed)
                    totalReadsUsed++;
            }
        }
        System.err.println(String.format("Reads total: %,10d", totalReads));
        System.err.println(String.format("Reads used:  %,10d ", totalReadsUsed));
        System.err.println(String.format("References:  %,10d", reference2ReadMatchPairs.keySet().size()));
    }

    private IReadBlock copy(IReadBlock src, String[] cNames) {
        final ReadBlockRMA2 readBlock = new ReadBlockRMA2();
        readBlock.setReadHeader(src.getReadHeader());
        readBlock.setReadSequence(src.getReadSequence());
        readBlock.setReadWeight(src.getReadWeight());
        readBlock.setNumberOfMatches(src.getNumberOfMatches());
        IMatchBlock[] matches = new MatchBlockRMA2[src.getNumberOfMatches()];
        for (int i = 0; i < src.getNumberOfMatches(); i++)
            matches[i] = copy(src.getMatchBlock(i), cNames);
        readBlock.setMatchBlocks(matches);
        return readBlock;
    }

    private IMatchBlock copy(IMatchBlock src, String[] cNames) {
        final MatchBlockRMA2 matchBlock = new MatchBlockRMA2();
        matchBlock.setBitScore(src.getBitScore());
        matchBlock.setExpected(src.getExpected());
        matchBlock.setLength(src.getLength());
        matchBlock.setPercentIdentity(src.getPercentIdentity());
        matchBlock.setText(src.getText());
        matchBlock.setIgnore(src.isIgnore());
        for (String name : cNames) {
            matchBlock.setId(name, src.getId(name));
        }
        return matchBlock;
    }

    /**
     * export all loaded alignments to individual files
     *
     * @param totalFilesWritten
     * @param fileNameTemplate
     * @param useAnyReadOnlyOnce
     * @param blastXAsProtein
     * @param asConsensus
     * @param minReads
     * @param minLength
     * @param minCoverage
     * @param progressListener
     * @return the number of reads and files
     * @throws IOException
     * @throws CanceledException
     */
    public Pair<Integer, Integer> exportToFiles(int totalFilesWritten, final String fileNameTemplate,
                                                final boolean useAnyReadOnlyOnce, final boolean blastXAsProtein, final boolean asConsensus,
                                                int minReads, int minLength, final double minCoverage,
                                                final ProgressListener progressListener) throws IOException, CanceledException {

        // sort data by decreasing number of reads associated with a given reference sequence
        final SortedSet<Pair<String, List<Pair<IReadBlock, IMatchBlock>>>> sorted =
                new TreeSet<>((pair1, pair2) -> {
                    if (pair1.getSecond().size() > pair2.getSecond().size())
                        return -1;
                    else if (pair1.getSecond().size() < pair2.getSecond().size())
                        return 1;
                    else
                        return pair1.getFirst().compareTo(pair2.getFirst());
                });
        for (String reference : reference2ReadMatchPairs.keySet()) {
            List<Pair<IReadBlock, IMatchBlock>> value = reference2ReadMatchPairs.get(reference);
            if (value != null)
                sorted.add(new Pair<>(reference, value));
        }

        Blast2Alignment blast2Alignment = new Blast2Alignment(doc);

        progressListener.setSubtask("Writing data");
        progressListener.setMaximum(sorted.size());
        progressListener.setProgress(0);

        int totalOutputSequences = 0;
        Set<String> fileNames = new HashSet<>();

        final Alignment alignment = new Alignment();

        // go through all alignments in decreasing order of size and write to files
        while (sorted.size() > 0) {
            progressListener.incrementProgress();
            final Pair<String, List<Pair<IReadBlock, IMatchBlock>>> pair = sorted.first();
            sorted.remove(pair);

            final String reference = pair.getFirst();

            blast2Alignment.loadData(classificationName, classId, className, reference, pair.getSecond());
            blast2Alignment.makeAlignment(reference, alignment, true, new ProgressCmdLine());

            if (minReads > 1 && alignment.getNumberOfSequences() < minReads) {
                if (verbose) System.err.println(" (too few reads, skipped)");
                continue;
            }
            if (verbose && minLength > 1 && alignment.getLength() < minLength) {
                if (verbose) System.err.println(" (too short, skipped)");
                continue;
            }

            if (minCoverage > 0) {
                final Pair<Double, Double> gcAndCoverage = ComputeAlignmentProperties.computeCGContentAndCoverage(alignment, verbose ? new ProgressCmdLine() : null);
                if (verbose)
                    System.err.print(String.format("%s: %s: coverage=%.2f", className, reference, gcAndCoverage.get2()));
                if (gcAndCoverage.get2() < minCoverage) {
                    if (verbose) System.err.println(" (coverage too low, skipped)");
                    continue;
                } else if (verbose) System.err.println();
            }

            if (!(useEachReferenceOnlyOnce && usedReferences.contains(reference))) {
                if (useEachReferenceOnlyOnce)
                    usedReferences.add(reference);

                totalFilesWritten++;
                String fileName = "" + fileNameTemplate;
                if (fileName.contains("%n"))
                    fileName = fileNameTemplate.replaceAll("%n", String.format("%05d", totalFilesWritten));
                if (fileName.contains("%c"))
                    fileName = fileName.replaceAll("%c", (className != null ? Basic.toCleanName(className.trim()) : ""));
                if (fileName.contains("%r"))
                    fileName = fileName.replaceAll("%r", Basic.toCleanName(reference.trim()));

                if (fileNames.contains(fileName))
                    fileName = Basic.replaceFileSuffix(fileName, "-" + totalFilesWritten + Basic.getFileSuffix(fileName));
                fileNames.add(fileName);

                if ((new File(fileName)).exists()) {
                    if (!warned) {
                        if (ProgramProperties.isUseGUI()) {
                            int result = JOptionPane.showConfirmDialog(parent, "Some files already exist, overwrite all existing files?", "Overwrite files?", JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon());
                            switch (result) {
                                case JOptionPane.NO_OPTION:
                                    overwrite = false;
                                    break;
                                case JOptionPane.CANCEL_OPTION:
                                    throw new CanceledException();
                                default:
                                    break;
                            }
                        }
                        warned = true;
                    }
                    if (!overwrite) {
                        System.err.println("Skipping existing file: '" + fileName + "'");
                        continue;
                    }
                }

                final File outputFile = new File(fileName);

                try (BufferedWriter w = new BufferedWriter(new FileWriter(outputFile))) {
                    if (asConsensus) {
                        System.err.println("Writing consensus of " + alignment.getNumberOfSequences() + " reads to file: '" + fileName + "'");
                        String consensus = alignment.computeConsensus();
                        w.write("> Consensus\n" + consensus + "\n");
                        totalOutputSequences++;
                    } else {
                        System.err.println("Writing " + alignment.getNumberOfSequences() + " reads to file: '" + fileName + "'");

                        if (blastXAsProtein) { // Write DNA as translated
                            if (alignment.getSequenceType().equals(Alignment.cDNA) && alignment.getReference() != null && alignment.getReference().getLength() > 0) {
                                w.write(alignment.getReferenceName() + "\n");
                                final String ref;
                                if (alignment.getOriginalReference() != null)
                                    ref = alignment.getOriginalReference().getSequence();
                                else
                                    ref = alignment.getReference().getSequence();
                                w.write(ref);   // todo: ref sequence is currently missing leading gaps
                                w.write("\n");
                            }
                            int minRow = 0;
                            int maxRow = alignment.getNumberOfSequences() - 1;
                            for (int row = minRow; row <= maxRow; row++) {
                                w.write(">" + Basic.swallowLeadingGreaterSign(alignment.getName(row)) + "\n");
                                Lane lane = alignment.getLane(row);
                                for (int i = 0; i < lane.getFirstNonGapPosition(); i += 3)
                                    w.write("-");
                                String sequence = lane.getBlock();
                                for (int i = 0; i < sequence.length() - 2; i += 3) {
                                    w.write(SequenceUtils.getAminoAcid(sequence, i));
                                }
                                for (int i = lane.getLastNonGapPosition() + 1; i < lane.getLength(); i += 3)
                                    w.write("-");
                                w.write("\n");
                            }
                        } else { // write as DNA
                            if (alignment.getSequenceType().equals(Alignment.cDNA) && alignment.getReference() != null && alignment.getReference().getLength() > 0) {
                                w.write(alignment.getReferenceName() + "\n");
                                final String ref;
                                if (alignment.getOriginalReference() != null)
                                    ref = alignment.getOriginalReference().getSequence();
                                else
                                    ref = alignment.getReference().getSequence();
                                w.write(ref);    // todo: ref sequence is currently missing leading gaps
                                w.write("\n");
                            }
                            w.write(alignment.toFastA());
                        }
                        totalOutputSequences += blast2Alignment.getTotalNumberOfReads();
                    }
                    if (useAnyReadOnlyOnce)
                        removeReadsFromSets(sorted, pair); // any read used is removed from all other alignments
                }

                if (outputFile.exists() && outputFile.length() == 0) {
                    if (outputFile.delete())
                        totalFilesWritten--;
                }
            }
        }
        sorted.clear();
        reference2ReadMatchPairs.clear();
        System.err.println(String.format("Output reads:%,10d", totalOutputSequences));
        System.err.println(String.format("Output files:%,10d", totalFilesWritten));

        return new Pair<>(totalOutputSequences, totalFilesWritten);
    }

    /**
     * remove all reads used in current alignment from the remaining ones in the sorte dset. Reinserts modified alignments so that the sorted set stays sorted
     *
     * @param sorted
     * @param current
     */

    private void removeReadsFromSets(SortedSet<Pair<String, List<Pair<IReadBlock, IMatchBlock>>>> sorted, Pair<String, List<Pair<IReadBlock, IMatchBlock>>> current) {
        // determine set of matches that have just been used
        Set<IReadBlock> reads = new HashSet<>();
        for (Pair<IReadBlock, IMatchBlock> readAndMatch : current.getSecond()) {
            reads.add(readAndMatch.getFirst());

        }

        // determine which remaining alignment sets contain at least one used read
        List<Pair<String, List<Pair<IReadBlock, IMatchBlock>>>> toModify = new LinkedList<>();
        for (Pair<String, List<Pair<IReadBlock, IMatchBlock>>> refReadMatches : sorted) {
            for (Pair<IReadBlock, IMatchBlock> readMatch : refReadMatches.getSecond()) {
                if (reads.contains(readMatch.getFirst())) {
                    toModify.add(refReadMatches);
                    break;
                }
            }
        }

        // remove all used reads, re-inserting modified datasets into the sorted set.
        for (Pair<String, List<Pair<IReadBlock, IMatchBlock>>> refReadMatches : toModify) {
            sorted.remove(refReadMatches);
            List<Pair<IReadBlock, IMatchBlock>> toDelete = new LinkedList<>();
            for (Pair<IReadBlock, IMatchBlock> readMatch : refReadMatches.getSecond()) {
                if (reads.contains(readMatch.getFirst())) {
                    toDelete.add(readMatch);
                }
            }
            refReadMatches.getSecond().removeAll(toDelete);
            if (refReadMatches.getSecond().size() > 0)
                sorted.add(refReadMatches);
        }
    }


    public void setUseEachReferenceOnlyOnce(boolean useEachReferenceOnlyOnce) {
        this.useEachReferenceOnlyOnce = useEachReferenceOnlyOnce;
    }

    public boolean isUseEachReferenceOnlyOnce() {
        return useEachReferenceOnlyOnce;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
