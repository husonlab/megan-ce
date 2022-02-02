/*
 * ReadDataCollector.java Copyright (C) 2022 Daniel H. Huson
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

package megan.assembly;

import jloda.seq.SequenceUtils;
import jloda.util.CanceledException;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.IOException;
import java.util.ArrayList;
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
     * @return list of contig names and contigs
     */
    public static List<ReadData> apply(final IReadBlockIterator iterator, final ProgressListener progress) throws IOException, CanceledException {
        progress.setSubtask("Collecting reads:");

        final var list = new LinkedList<ReadData>();

        var countReads = 0;
        {
            progress.setMaximum(iterator.getMaximumProgress());
            progress.setProgress(0);
            while (iterator.hasNext()) {
                final var readBlock = iterator.next();
                //System.err.println(readBlock.getReadName()+" -> "+countReads);
                list.add(createReadData(countReads++, readBlock));
                progress.setProgress(iterator.getProgress());
            }
        }
        progress.reportTaskCompleted();
        return list;
    }

    /**
     * creates the data object associated with a given read and its matches
     *
     * @return read data
	 */
    private static ReadData createReadData(int id, IReadBlock readBlock) throws IOException {
        var readData = new ReadData(id, readBlock.getReadName());

        var best = -1;
        var bestScore = 0f;
        for (var m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            if (readBlock.getMatchBlock(m).getBitScore() > bestScore) {
                best = m;
                bestScore = readBlock.getMatchBlock(m).getBitScore();
            }
        }
        if (best >= 0) {
            var bestCoordinates = getQueryCoordinates(readBlock.getMatchBlock(best));
            if (bestCoordinates[0] < bestCoordinates[1])
                readData.setSegment(readBlock.getReadSequence().substring(bestCoordinates[0] - 1, bestCoordinates[1]));
            else
				readData.setSegment(SequenceUtils.getReverseComplement(readBlock.getReadSequence().substring(bestCoordinates[1] - 1, bestCoordinates[0])));

            final var matches = new ArrayList<MatchData>(readBlock.getNumberOfAvailableMatchBlocks());

            for (var m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
                if (readBlock.getMatchBlock(m).getBitScore() == bestScore) {
                    final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                    final int[] queryCoordinates = getQueryCoordinates(matchBlock);
                    if (queryCoordinates[0] == bestCoordinates[0] && queryCoordinates[1] == bestCoordinates[1]) { // must all reference same segment in same orientation
                        int[] refCoordinates = getReferenceCoordinates(matchBlock);
						matches.add(new MatchData(readData, StringUtils.getFirstWord(matchBlock.getText()), refCoordinates[0], refCoordinates[1], matchBlock.getText(), matchBlock.getBitScore()));
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
     * @return query coordinates, 1-based
	 */
    private static int[] getQueryCoordinates(IMatchBlock matchBlock) {
        var start = matchBlock.getAlignedQueryStart();
        var end = matchBlock.getAlignedQueryEnd();
        return new int[]{start, end};
    }

    /**
     * get start and end reference coordinates of a match
     *
     * @return reference coordinates 1-based
	 */
    private static int[] getReferenceCoordinates(IMatchBlock matchBlock) throws IOException {
        var tokensFirst = getLineTokens("Sbjct:", matchBlock.getText(), false);
        var tokensLast = getLineTokens("Sbjct:", matchBlock.getText(), true);
        if (tokensFirst == null) {
            tokensFirst = getLineTokens("Sbjct", matchBlock.getText(), false);
            tokensLast = getLineTokens("Sbjct", matchBlock.getText(), true);
        }
        if (tokensFirst == null || tokensFirst.length != 4 || tokensLast == null || tokensLast.length != 4) {
            throw new IOException("Failed to parse sbjct line for match:\n" + matchBlock.getText());
        }
        var a = Integer.parseInt(tokensFirst[1]);
        var b = Integer.parseInt(tokensLast[3]);
        return new int[]{a, b};
    }

    /**
     * get all tokens on the first line that begin with start
     *
     * @param last  if true, returns last such line rather than first
     * @return tokens
     */
    private static String[] getLineTokens(String start, String text, boolean last) {
        var a = (last ? text.lastIndexOf("\n" + start) : text.indexOf("\n" + start));
        if (a != -1) {
            var b = text.indexOf('\n', a + 1);
            if (b == -1)
                return text.substring(a + 1).split("\\s+");
            else
                return text.substring(a + 1, b).split("\\s+");
        }
        return null;
    }
}
