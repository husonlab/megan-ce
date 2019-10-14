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
package megan.alignment.gui;

import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.ProgressPercentage;

import java.io.StringWriter;
import java.util.*;

/**
 * describes an alignment
 * Daniel Huson, 9.2011
 */
public class Alignment {
    private String name = "Untitled";
    private int length = -1;
    private final Vector<Lane> lanes;
    private final ArrayList<Integer> order;  // lists sequences in their sorted order
    private final GapColumnContractor gapColumnContractor;
    private final RowCompressor rowCompressor;
    private boolean translate = false;

    private String referenceName;
    private Lane reference;
    private Lane originalReference;

    private String referenceType = UNKNOWN;
    private Lane untranslatedConsensus;
    private Lane translatedConsensus;

    private static final String UNKNOWN = "Unknown";
    static public final String DNA = "DNA";
    static public final String cDNA = "codingDNA";
    static public final String PROTEIN = "PROTEIN";

    private String sequenceType = UNKNOWN;

    private final Set<Integer> insertionsIntoReference = new HashSet<>();

    /**
     * constructor
     */
    public Alignment() {
        lanes = new Vector<>();
        order = new ArrayList<>();  // lists sequences in their sorted order
        gapColumnContractor = new GapColumnContractor();
        rowCompressor = new RowCompressor(this);
    }

    public void clear() {
        length = -1;
        lanes.clear();
        gapColumnContractor.clear();
        rowCompressor.clear();
        order.clear();
        insertionsIntoReference.clear();
        untranslatedConsensus = null;
        translatedConsensus = null;
        reference = null;
        originalReference = null;
    }

    public int getLength() {
        if (length == -1) {
            for (int i = 0; i < getNumberOfSequences(); i++) {
                length = Math.max(length, getLaneLength(i));
            }
        }
        return length + ProgramProperties.get("alignmentViewerAdditionalPositions", 0);
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    public int getNumberOfSequences() {
        return lanes.size();
    }

    public String getName(int row) {
        return getLane(row).getName();
    }

    public Lane getLane(int row0) {
        int row = getOrder(row0);
        if (row < lanes.size())
            return lanes.get(row);
        else
            return null;
    }

    private int getLaneLength(int row0) {
        Lane lane = getLane(row0);
        try {
            return lane.getLength();
        } catch (Exception e) {
            return 0;
        }
    }

    public Lane getReference() {
        return reference;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public Lane getConsensus() {
        if (isTranslate()) {
            if (translatedConsensus == null || (translatedConsensus.toString().length() == 0 && getLength() > 0))
                translatedConsensus = new Lane(null, "Consensus", "", "Consensus sequence", computeConsensus());
            return translatedConsensus;
        } else {
            if (untranslatedConsensus == null || (untranslatedConsensus.toString().length() == 0 && getLength() > 0))
                untranslatedConsensus = new Lane(null, "Consensus", "", "Consensus sequence", computeConsensus());
            return untranslatedConsensus;
        }
    }

    public void setReferenceName(String name) {
        referenceName = name;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public void setReference(String name, String reference) {
        referenceName = name;
        this.reference = new Lane(null, name, "", null, reference);
        // System.err.println("Reference length: " + reference.length());
    }

    public Lane getOriginalReference() {
        return originalReference;
    }

    public void setOriginalReference(String originalReference) {
        this.originalReference = new Lane(null, name, "", null, originalReference);
    }

    /**
     * adds a sequence to the alignment
     *
     * @param name
     * @param text
     * @param toolTip
     * @param unalignedPrefix unaligned prefix of sequence
     * @param leadingGaps     number of gaps before alignment starts
     * @param block           core of alignment
     * @param trailingGaps    number of trailing gaps
     * @param unalignedSuffix unaligned suffix of sequence
     */
    public void addSequence(String name, String text, String toolTip, String unalignedPrefix, int leadingGaps, String block, int trailingGaps, String unalignedSuffix) {
        lanes.add(new Lane(this, name, text, toolTip, unalignedPrefix, leadingGaps, block, trailingGaps, unalignedSuffix));
    }

    /**
     * get string representation of block in alignment
     *
     * @param includeUnalignedChars
     * @param minRow
     * @param minLayoutCol
     * @param maxRow
     * @param maxLayoutCol          @return string
     */
    public String toFastA(boolean includeUnalignedChars, int minRow, int minLayoutCol, int maxRow, int maxLayoutCol) {
        StringWriter w = new StringWriter();

        if (!getRowCompressor().isEnabled()) {
            final Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
            maxRow = Math.min(maxRow, getNumberOfSequences() - 1);
            for (int read = minRow; read <= maxRow; read++) {
                String readName = getName(read);
                if (readName.startsWith(">"))
                    w.write(readName + "\n");
                else
                    w.write(">" + readName + "\n");
                Lane lane = getLane(read);
                int jc = 0;
                int jumped = 0;
                for (int layoutCol = minLayoutCol; layoutCol <= maxLayoutCol; layoutCol++) {
                    while (jc < jumpCols.length && jumpCols[jc] <= layoutCol) {
                        jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                        jc++;
                    }
                    int trueCol = layoutCol + jumped;
                    if (trueCol < getLength()) {
                        if (includeUnalignedChars && lane.hasUnalignedCharAt(trueCol)) {
                            char ch = lane.getUnalignedCharAt(trueCol);
                            w.write(ch);
                        } else {
                            if (trueCol >= lane.getFirstNonGapPosition() && trueCol <= lane.getLastNonGapPosition()) {
                                char ch = lane.charAt(trueCol);
                                if (ch == 0)
                                    ch = '-';
                                if (ch != ' ')
                                    w.write(ch);
                            } else {
                                if (!isTranslate() || (trueCol % 3) == 0)
                                    w.write('-');
                            }
                        }
                    }
                }
                w.write("\n");
            }
        } else {
            maxRow = Math.min(maxRow, getRowCompressor().getNumberRows() - 1);
            final Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
            int minTrueCol = gapColumnContractor.getTotalJumpBeforeLayoutColumn(minLayoutCol) + minLayoutCol;
            int maxTrueCol = gapColumnContractor.getTotalJumpBeforeLayoutColumn(maxLayoutCol - 1) + maxLayoutCol;

            for (int row = minRow; row <= maxRow; row++) {
                //w.write(">row" + (row + 1) + "\n");

                int lastPos = minTrueCol - 1;
                for (int read : rowCompressor.getCompressedRow2Reads(row)) {
                    int jc = 0;
                    int jumped = 0;
                    Lane lane = getLane(read);

                    if (minTrueCol <= lane.getLastNonGapPosition() && maxTrueCol >= lane.getFirstNonGapPosition()) { // read intersects visible cols
                        int startTrueCol = Math.max(minTrueCol, lane.getFirstNonGapPosition());
                        int endTrueCol = Math.min(maxTrueCol, lane.getLastNonGapPosition());

                        while (lastPos < startTrueCol - (isTranslate() ? 1 : 1)) { // todo: what is going on here?
                            if (!translate || (lastPos % 3) == 0)
                                w.write("-");
                            lastPos++;
                        }

                        for (int layoutCol = minLayoutCol; layoutCol <= maxLayoutCol; layoutCol++) {
                            while (jc < jumpCols.length && jumpCols[jc] <= layoutCol) {
                                jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                                jc++;
                            }
                            int trueCol = layoutCol + jumped;

                            if (trueCol >= startTrueCol && trueCol <= endTrueCol) {
                                if (includeUnalignedChars && lane.hasUnalignedCharAt(trueCol)) {
                                    char ch = lane.getUnalignedCharAt(trueCol);
                                    w.write(ch);
                                } else if (!isTranslate() || trueCol < lane.getLastNonGapPosition() - 2) {
                                    char ch = lane.charAt(trueCol);

                                    if (ch == 0)
                                        ch = '-';

                                    if (ch != ' ')
                                        w.write(ch);
                                }
                                lastPos++;
                            }
                        }
                    }
                }
                while (lastPos < maxTrueCol) {
                    if (!translate || (lastPos % 3) == 0)
                        w.write("-");
                    lastPos++;
                }
                w.write("\n");
            }
        }
        return w.toString();
    }

    /**
     * return a fastA representation in string
     *
     * @return
     */
    public String toFastA() {
        return toFastA(false, 0, 0, getNumberOfSequences() - 1, getLength());
    }

    /**
     * get the position of the row in the row ordering
     *
     * @param row
     * @return ordered position
     */
    public int getOrder(int row) {
        if (order.size() > row && order.get(row) != null)
            return order.get(row);
        else
            return row;
    }

    /**
     * gets the max length of any name
     *
     * @return max name length
     */
    public int getMaxNameLength() {
        int result = 0;
        for (int i = 0; i < getNumberOfSequences(); i++)
            result = Math.max(result, getName(i).length());
        return result;
    }

    public void resetOrder() {
        order.clear();
        for (int i = 0; i < lanes.size(); i++)
            order.add(i);
    }

    public void setOrder(List<Integer> order) {
        if (order.size() < this.order.size()) {
            this.order.removeAll(order);
            final List<Integer> oldOrder = new LinkedList<>(this.order);
            this.order.clear();
            this.order.addAll(order);
            this.order.addAll(oldOrder);
        } else {
            this.order.clear();
            this.order.addAll(order);
        }
    }

    public String getToolTip(int read) {
        if (read >= 0 && read < lanes.size())
            return lanes.get(getOrder(read)).getToolTip();
        else
            return null;
    }

    public String getText(int read) {
        if (read >= 0 && read < lanes.size())
            return lanes.get(getOrder(read)).getText();
        else
            return "";
    }

    public GapColumnContractor getGapColumnContractor() {
        return gapColumnContractor;
    }

    public RowCompressor getRowCompressor() {
        return rowCompressor;
    }

    /**
     * computes the consensus for the complete sequences
     *
     * @return consensus
     */
    public String computeConsensus() {
        System.err.println("Computing consensus:");

        // list of read start and end events
        final List<Pair<Integer, Integer>> list = new LinkedList<>();
        for (int row = 0; row < getNumberOfSequences(); row++) {
            final Lane lane = getLane(row);
            Pair<Integer, Integer> pair = new Pair<>(lane.getFirstNonGapPosition(), row);
            list.add(pair);
            pair = new Pair<>(lane.getLastNonGapPosition() + 1, row);
            list.add(pair);
        }

        final Pair<Integer, Integer>[] array = (Pair<Integer, Integer>[]) list.toArray(new Pair[0]); // pair position,row
        Arrays.sort(array);

        ProgressPercentage progress = new ProgressPercentage();
        progress.setCancelable(false);
        progress.setMaximum(array.length + 2);

        StringBuilder buf = new StringBuilder();  // consensus will be put here

        // prefix of gaps:
        final int firstPos = array.length > 0 ? array[0].get1() : getLength();
        buf.append("-".repeat(Math.max(0, firstPos)));
        progress.incrementProgress();

        final Set<Integer> activeRows = new HashSet<>();

        for (int i = 0; i < array.length; i++) { // consider each event in turn
            progress.incrementProgress();
            int pos = array[i].get1();
            while (true) {
                int row = array[i].get2();
                if (activeRows.contains(row)) // if is active, must remove
                    activeRows.remove(row);
                else
                    activeRows.add(row); // not yet active, add
                if (i + 1 < array.length && array[i + 1].get1() == pos) {
                    i++;
                    progress.incrementProgress();
                } else
                    break;
            }

            final int nextPos = (i + 1 < array.length ? array[i + 1].get1() : getLength());

            boolean debug = false;
            if (debug)
                System.err.println("Active rows: " + Basic.toString(activeRows, ","));
            if (activeRows.size() > 0) {
                for (int col = pos; col < nextPos; col++) {
                    if (debug)
                        System.err.println("col: " + col);
                    Map<Character, Integer> char2count = new HashMap<>();
                    for (final int row : activeRows) {
                        final Lane lane = getLane(row);
                        char ch = lane.charAt(col);
                        if (debug)
                            System.err.println("row: " + row + " ch=" + ch);
                        if (debug)
                            System.err.println("row: " + lane.getFirstNonGapPosition() + " - " + lane.getLastNonGapPosition());
                        if (Character.isLetter(ch) || ch == ' ') {
                            char2count.merge(ch, 1, Integer::sum);
                        }
                    }
                    Character best = null;
                    int bestCount = 0;
                    for (Character ch : char2count.keySet()) {
                        if (char2count.get(ch) > bestCount) {
                            best = ch;
                            bestCount = char2count.get(ch);
                        }
                    }
                    if (best != null) {
                        if (bestCount >= char2count.keySet().size() / 2)
                            best = Character.toUpperCase(best);
                        else
                            best = Character.toLowerCase(best);
                        buf.append(best);
                    } else
                        buf.append('-');
                }
            } else {
                buf.append("-".repeat(Math.max(0, nextPos - pos)));
            }
        }
        // suffix of gaps:
        // prefix of gaps:
        int lastPos = array.length > 0 ? array[array.length - 1].get1() : getLength();
        // todo: FIXME
        buf.append("-".repeat(lastPos - lastPos));
        progress.incrementProgress();
        progress.close();

        return buf.toString();
    }

    /**
     * get string representation of segment of consensus
     *
     * @param minLayoutCol
     * @param maxLayoutCol
     * @return string
     */
    public String getConsensusString(int minLayoutCol, int maxLayoutCol) {
        StringWriter w = new StringWriter();

        final Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
        final Lane lane = getConsensus();
        int jc = 0;
        int jumped = 0;
        for (int layoutCol = minLayoutCol; layoutCol <= maxLayoutCol; layoutCol++) {
            while (jc < jumpCols.length && jumpCols[jc] <= layoutCol) {
                jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                jc++;
            }
            int trueCol = layoutCol + jumped;
            if (trueCol < getLength()) {
                if (trueCol >= lane.getFirstNonGapPosition() && trueCol <= lane.getLastNonGapPosition()) {
                    char ch = lane.charAt(trueCol);
                    if (ch == 0)
                        ch = '-';
                    if (ch != ' ')
                        w.write(ch);
                } else {
                    if (!isTranslate() || (trueCol % 3) == 0)
                        w.write('-');
                }
            }
        }
        return w.toString();
    }

    /**
     * get string representation of segment of reference
     *
     * @param minLayoutCol
     * @param maxLayoutCol
     * @return string
     */
    public String getReferenceString(int minLayoutCol, int maxLayoutCol) {
        StringWriter w = new StringWriter();

        final Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
        Lane lane = getReference();
        int jc = 0;
        int jumped = 0;
        for (int layoutCol = minLayoutCol; layoutCol <= maxLayoutCol; layoutCol++) {
            while (jc < jumpCols.length && jumpCols[jc] <= layoutCol) {
                jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                jc++;
            }
            int trueCol = layoutCol + jumped;
            if (trueCol < getLength()) {
                if (trueCol >= lane.getFirstNonGapPosition() && trueCol <= lane.getLastNonGapPosition()) {
                    char ch = lane.charAt(trueCol);
                    if (ch == 0)
                        ch = '-';
                    if (ch != ' ')
                        w.write(ch);
                } else {
                    if (!isTranslate() || (trueCol % 3) == 0)
                        w.write('-');
                }
            }
        }
        return w.toString();
    }

    /**
     * gets all positions of alignment that are insertions into the reference
     *
     * @return all positions of insertions into the reference
     */
    public Set<Integer> getInsertionsIntoReference() {
        return insertionsIntoReference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isTranslate() {
        return translate;
    }

    public void setTranslate(boolean translate) {
        this.translate = translate;
    }

    /**
     * get the read that is displayed at the given row and column
     *
     * @param row
     * @param col
     * @return read or -1
     */
    public int getHitRead(int row, int col) {
        col += getGapColumnContractor().getTotalJumpBeforeLayoutColumn(col);
        return getRowCompressor().getRead(row, col);
    }

    public void trimToTrueLength(int trueLength) {
        for (Lane lane : lanes) {
            lane.trimFromEnd(trueLength);
        }
    }
}
