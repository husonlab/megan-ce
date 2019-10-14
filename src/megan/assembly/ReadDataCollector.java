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

package megan.assembly;

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * collects all reads for gc assembly
 * Created by huson on 8/22/16.
 */
public class ReadDataCollector {
    /**
     * collect all read data associated with the given iterator
     *
     * @param progress
     * @return list of contig names and contigs
     */
    public static List<ReadData> apply(final IReadBlockIterator iterator, final ProgressListener progress) throws IOException, CanceledException {
        // collect all readDatas:
        progress.setSubtask("Collecting reads:");

        final List<ReadData> list = new LinkedList<>();

        int countReads = 0;
        {
            progress.setMaximum(iterator.getMaximumProgress());
            progress.setProgress(0);
            while (iterator.hasNext()) {
                final IReadBlock readBlock = iterator.next();
                //System.err.println(readBlock.getReadName()+" -> "+countReads);
                list.add(createReadData(countReads++, readBlock));
                progress.setProgress(iterator.getProgress());
            }
        }
        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();
        return list;
    }

    /**
     * creates the data object associated with a given read and its matches
     *
     * @param readBlock
     * @return read data
     * @throws IOException
     */
    private static ReadData createReadData(int id, IReadBlock readBlock) throws IOException {
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
            readData.setMatches(matches.toArray(new MatchData[0]));
        }
        return readData;
    }

    /**
     * get start and end query coordinates of a match
     *
     * @param matchBlock
     * @return query coordinates, 1-based
     * @throws IOException
     */
    private static int[] getQueryCoordinates(IMatchBlock matchBlock) throws IOException {
        int start = matchBlock.getAlignedQueryStart();
        int end = matchBlock.getAlignedQueryEnd();
        return new int[]{start, end};
    }

    /**
     * get start and end reference coordinates of a match
     *
     * @param matchBlock
     * @return reference coordinates 1-based
     * @throws IOException
     */
    private static int[] getReferenceCoordinates(IMatchBlock matchBlock) throws IOException {
        String[] tokensFirst = getLineTokens("Sbjct:", matchBlock.getText(), false);
        String[] tokensLast = getLineTokens("Sbjct:", matchBlock.getText(), true);
        if (tokensFirst == null) {
            tokensFirst = getLineTokens("Sbjct", matchBlock.getText(), false);
            tokensLast = getLineTokens("Sbjct", matchBlock.getText(), true);
        }
        if (tokensFirst == null || tokensFirst.length != 4 || tokensLast == null || tokensLast.length != 4) {
            throw new IOException("Failed to parse sbjct line for match:\n" + matchBlock.getText());
        }
        int a = Integer.parseInt(tokensFirst[1]);
        int b = Integer.parseInt(tokensLast[3]);
        return new int[]{a, b};
    }

    /**
     * get all tokens on the first line that begin with start
     *
     * @param start
     * @param text
     * @param last  if true, returns last such line rather than first
     * @return tokens
     */
    private static String[] getLineTokens(String start, String text, boolean last) {
        int a = (last ? text.lastIndexOf("\n" + start) : text.indexOf("\n" + start));
        if (a != -1) {
            int b = text.indexOf('\n', a + 1);
            if (b == -1)
                return text.substring(a + 1).split("\\s+");
            else
                return text.substring(a + 1, b).split("\\s+");
        }
        return null;
    }
}
