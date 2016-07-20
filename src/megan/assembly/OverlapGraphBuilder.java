/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.assembly;

import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.graph.NodeMap;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.IOException;
import java.util.*;

/**
 * assembles a set of readDatas that align to a specific class in some classification
 * <p/>
 * Daniel Huson, 5.2015
 */
public class OverlapGraphBuilder {
    private final Graph overlapGraph = new Graph();
    private final NodeMap<String> node2readName = new NodeMap<>(overlapGraph);
    private List<Integer>[] readId2ContainedReads;
    private ReadData[] readDatas;
    private int minOverlap;

    /**
     * constructor
     */
    public OverlapGraphBuilder(int minOverlap) {
        this.minOverlap = minOverlap;
    }

    /**
     * apply the assembler to the given readDatas
     *
     * @param progress
     * @return list of contig names and contigs
     */
    public void apply(final IReadBlockIterator iterator, final ProgressListener progress) throws IOException, CanceledException {
        // collect all readDatas:
        progress.setSubtask("Collecting reads");
        {
            final List<ReadData> list = new LinkedList<>();

            int countReads = 0;
            {
                progress.setMaximum(iterator.getMaximumProgress());
                progress.setProgress(0);
                while (iterator.hasNext()) {
                    IReadBlock readBlock = iterator.next();
                    //System.err.println(readBlock.getReadName()+" -> "+countReads);
                    list.add(createReadData(countReads++, readBlock));
                    progress.setProgress(iterator.getProgress());
                }
            }
            if (progress instanceof ProgressPercentage)
                ((ProgressPercentage) progress).reportTaskCompleted();
            readDatas = list.toArray(new ReadData[list.size()]);

            System.err.println(String.format("Total reads:%,10d", countReads));
        }


        // collect all matches for each reference:

        progress.setSubtask("Sorting reads and matches by reference");
        progress.setMaximum(readDatas.length);
        progress.setProgress(0);

        readId2ContainedReads = new List[readDatas.length];

        long countPairs = 0;
        Map<String, SortedSet<MatchData>> ref2matches = new HashMap<>();

        for (int r = 0; r < readDatas.length; r++) {
            final ReadData read = readDatas[r];
            for (int m = 0; m < read.getMatches().length; m++) {
                final MatchData match = read.getMatches()[m];
                SortedSet<MatchData> set = ref2matches.get(match.getRefName());
                if (set == null) {
                    set = new TreeSet<>(new MatchData());
                    ref2matches.put(match.getRefName(), set);
                }
                set.add(match);
                countPairs++;
            }
            progress.setProgress(r);
        }
        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();
        System.err.println(String.format("Overlaps:   %,10d", countPairs));

        buildOverlapGraph(readDatas, ref2matches, minOverlap);
    }

    /**
     * creates the data object associated with a given read and its matches
     *
     * @param readBlock
     * @return read data
     * @throws IOException
     */
    private ReadData createReadData(int id, IReadBlock readBlock) throws IOException {
        ReadData readData = new ReadData(id, readBlock.getReadName());

        int best = -1;
        float bestScore = 0;
        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            if (readBlock.getMatchBlock(m).getBitScore() > bestScore) {
                best = m;
                bestScore = readBlock.getMatchBlock(m).getBitScore();
            }
        }
        if (best >= 0) {
            int[] bestCoordinates = getQueryCoordinates(readBlock.getMatchBlock(best));
            if (bestCoordinates[0] < bestCoordinates[1])
                readData.setSegment(readBlock.getReadSequence().substring(bestCoordinates[0] - 1, bestCoordinates[1]));
            else
                readData.setSegment(Basic.getReverseComplement(readBlock.getReadSequence().substring(bestCoordinates[1] - 1, bestCoordinates[0])));

            final List<MatchData> matches = new LinkedList<>();

            for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
                if (readBlock.getMatchBlock(m).getBitScore() == bestScore) {
                    final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                    final int[] queryCoordinates = getQueryCoordinates(matchBlock);
                    if (queryCoordinates[0] == bestCoordinates[0] && queryCoordinates[1] == bestCoordinates[1]) { // must all reference same segment in same orientation
                        int[] refCoordinates = getReferenceCoordinates(matchBlock);
                        matches.add(new MatchData(readData, Basic.getFirstWord(matchBlock.getText()), refCoordinates[0], refCoordinates[1], matchBlock.getText(), matchBlock.getBitScore()));
                    }
                }
            }
            readData.setMatches(matches.toArray(new MatchData[matches.size()]));
        }
        return readData;
    }

    /**
     * build the overlap graph
     *
     * @param reads
     * @param ref2matches
     */
    private void buildOverlapGraph(ReadData[] reads, Map<String, SortedSet<MatchData>> ref2matches, int minOverlap) {
        Node[] nodes = new Node[reads.length];

        final BitSet containedReadIds = new BitSet();

        for (String refName : ref2matches.keySet()) {
            final MatchData[] matches = ref2matches.get(refName).toArray(new MatchData[ref2matches.get(refName).size()]);

            for (int i = 0; i < matches.length; i++) {
                final MatchData iMatch = matches[i];

                if (!containedReadIds.get(iMatch.getRead().getId())) {
                    Node v = nodes[iMatch.getRead().getId()];
                    if (v == null) {
                        v = nodes[iMatch.getRead().getId()] = overlapGraph.newNode(iMatch.getRead().getId());
                        node2readName.set(v, iMatch.getRead().getName());
                    }

                    for (int j = i + 1; j < matches.length; j++) {
                        final MatchData jMatch = matches[j];
                        if (3 * (iMatch.getLastPosInRef() - jMatch.getFirstPosInRef()) <= minOverlap)
                            break; // no chance of an overlap

                        int overlapLength = computePerfectOverlapLength(iMatch, jMatch);
                        if (overlapLength > 0 && jMatch.getLastPosInRef() <= iMatch.getLastPosInRef()) { // contained
                            containedReadIds.set(jMatch.getRead().getId());
                            List<Integer> contained = readId2ContainedReads[i];
                            if (contained == null) {
                                contained = readId2ContainedReads[i] = new ArrayList<>();
                            }
                            contained.add(j);
                        } else if (overlapLength >= minOverlap) {
                            Node w = nodes[jMatch.getRead().getId()];
                            if (w == null) {
                                w = nodes[jMatch.getRead().getId()] = overlapGraph.newNode(jMatch.getRead().getId());
                                node2readName.set(w, jMatch.getRead().getName());
                            }

                            final Edge e = overlapGraph.getCommonEdge(v, w);
                            if (e == null) {
                                overlapGraph.newEdge(v, w, overlapLength);
                            } else if ((Integer) e.getInfo() < overlapLength) {
                                e.setInfo(overlapLength);
                            }
                        }
                    }
                }
            }
        }
        System.err.println(String.format("Graph nodes:%,10d", overlapGraph.getNumberOfNodes()));
        System.err.println(String.format("Graph edges:%,10d", overlapGraph.getNumberOfEdges()));
        System.err.println(String.format("Cont. reads:%,10d", containedReadIds.cardinality()));
    }

    /**
     * if readDatas are identical for the matched sequence, gets the number of matching letters, else returns 0
     *
     * @param iMatch
     * @param jMatch
     * @return number of matching letters or 0
     */
    private int computePerfectOverlapLength(MatchData iMatch, MatchData jMatch) {
        try {
            int first = Math.max(iMatch.getFirstPosInRef(), jMatch.getFirstPosInRef());
            int last = Math.min(iMatch.getLastPosInRef(), jMatch.getLastPosInRef());

            int count = 0;
            for (int refPos = first; refPos <= last; refPos++) {
                for (int k = 0; k < 3; k++) {
                    int iPos = 3 * (refPos - iMatch.getFirstPosInRef()) + k;
                    int jPos = 3 * (refPos - jMatch.getFirstPosInRef()) + k;
                    char iChar = Character.toLowerCase(iMatch.getRead().getSegment().charAt(iPos));
                    char jChar = Character.toLowerCase(jMatch.getRead().getSegment().charAt(jPos));

                    if (iChar != jChar && iChar != 'n' && jChar != 'n')
                        return 0;
                    else if (Character.isLetter(iMatch.getRead().getSegment().charAt(iPos)))
                        count++;
                }
            }
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }


    /**
     * get start and end query coordinates of a match
     *
     * @param matchBlock
     * @return query coordinates
     * @throws IOException
     */
    private int[] getQueryCoordinates(IMatchBlock matchBlock) throws IOException {
        String[] tokens = getLineTokens("Query:", matchBlock.getText());
        if (tokens == null)
            tokens = getLineTokens("Query", matchBlock.getText());
        if (tokens == null || tokens.length != 4) {
            throw new IOException("Failed to parse query line for match:\n" + matchBlock.getText());
        }
        int a = Integer.parseInt(tokens[1]);
        int b = Integer.parseInt(tokens[3]);
        return new int[]{a, b};
    }

    /**
     * get start and end reference coordinates of a match
     *
     * @param matchBlock
     * @return reference coordinates
     * @throws IOException
     */
    private int[] getReferenceCoordinates(IMatchBlock matchBlock) throws IOException {
        String[] tokens = getLineTokens("Sbjct:", matchBlock.getText());
        if (tokens == null)
            tokens = getLineTokens("Sbjct", matchBlock.getText());
        if (tokens == null || tokens.length != 4) {
            throw new IOException("Failed to parse sbjct line for match:\n" + matchBlock.getText());
        }
        int a = Integer.parseInt(tokens[1]);
        int b = Integer.parseInt(tokens[3]);
        return new int[]{a, b};
    }

    /**
     * get the tokens of the query line
     *
     * @param text
     * @return query line tokens
     */
    private String[] getLineTokens(String start, String text) {
        String[] lines = Basic.split(text, '\n');
        for (String line : lines) {
            if (line.startsWith(start))
                return line.split("\\s+");
        }
        return null;
    }

    /**
     * get the overlap graph
     *
     * @return overlap graph
     */
    public Graph getOverlapGraph() {
        return overlapGraph;
    }

    /**
     * get the readDatas associated with the overlap graph
     *
     * @return readDatas
     */
    public ReadData[] getReadId2ReadData() {
        return readDatas;
    }

    /**
     * gets the the name of the read associated with a node
     *
     * @return read name
     */
    public NodeMap<String> getNode2ReadNameMap() {
        return node2readName;
    }


    public int getMinOverlap() {
        return minOverlap;
    }

    public void setMinOverlap(int minOverlap) {
        this.minOverlap = minOverlap;
    }

    public List<Integer>[] getReadId2ContainedReads() {
        return readId2ContainedReads;
    }
}
