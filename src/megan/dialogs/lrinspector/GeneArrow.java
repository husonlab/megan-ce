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

package megan.dialogs.lrinspector;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.shape.Polygon;
import jloda.swing.util.BasicSwing;
import jloda.util.Basic;
import megan.classification.ClassificationManager;
import megan.data.IMatchBlock;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * block arrow used to represent a gene
 * Created by huson on 2/21/17.
 */
public class GeneArrow extends Polygon implements Iterable<IMatchBlock> {
    private final String[] cNames;
    private final SortedSet<IMatchBlock> matchBlocks = new TreeSet<>(new BitScoreComparator());
    private final int start;
    private final int end;
    private final boolean reverse;

    private float bestBitScore;
    private float bestNormalizedScore;

    private final ArrayList<Label> labels = new ArrayList<>();

    /**
     * constructor
     *
     * @param readLength
     * @param arrowHeight
     * @param panelWidth
     */
    public GeneArrow(String[] cNames, int readLength, int arrowHeight, double panelWidth, double panelHeight, int start, int end, final Set<Integer> starts) {
        this.cNames = cNames;
        this.start = Math.min(start, end);
        this.end = Math.max(start, end);
        this.reverse = (end < start);
        setStrokeWidth(0.5);
        rescale(readLength, arrowHeight, panelWidth, panelHeight, starts);
        Tooltip.install(this, new Tooltip(createTooltipString()));
    }

    /**
     * rescale coordinates to fit changed panel dimensions
     *
     * @param panelWidth
     * @param panelHeight
     */
    public void rescale(int maxReadLength, int arrowHeight, double panelWidth, double panelHeight, final Set<Integer> starts) {
        final double a = (panelWidth * getStart()) / maxReadLength;
        final double b = (panelWidth * getEnd()) / maxReadLength;

        getPoints().clear();

        final double diff = Math.max(0, Math.min(Math.abs(a - b) - 1, 3));

        double middleHeight = 0.5 * panelHeight;
        final double increment = Math.max(1.5, 0.01 * panelHeight);

        if (isReverse()) {
            int c = (int) Math.round(a);
            while (starts.contains(-c)) {
                c++;
                middleHeight += increment; // start lower
            }
            starts.add(-c);

            getPoints().addAll(b, middleHeight,
                    a + diff, middleHeight,
                    a, middleHeight + 0.5 * arrowHeight,
                    a + diff, middleHeight + arrowHeight,
                    b, middleHeight + arrowHeight);

        } else {
            int c = (int) Math.round(a);
            while (starts.contains(c)) {
                c++;
                middleHeight -= increment; // start higher
            }
            starts.add(c);

            getPoints().addAll(a, middleHeight - arrowHeight,
                    a, middleHeight,
                    b - diff, middleHeight,
                    b, middleHeight - 0.5 * arrowHeight,
                    b - diff, middleHeight - arrowHeight);
        }
    }

    public void addMatchBlock(IMatchBlock matchBlock) {
        matchBlocks.add(matchBlock);
        bestBitScore = Math.max(bestBitScore, matchBlock.getBitScore());
        bestNormalizedScore = Math.max(bestNormalizedScore, matchBlock.getBitScore() / getLength());

        setStrokeWidth(0.5 + Math.log(matchBlocks.size()));
        Tooltip.install(this, new Tooltip(createTooltipString()));
    }

    public float getBestBitScore() {
        return bestBitScore;
    }

    public float getBestNormalizedScore() {
        return bestNormalizedScore;
    }

    public Collection<IMatchBlock> getMatchBlocks() {
        return matchBlocks;
    }

    public Iterator<IMatchBlock> iterator() {
        return matchBlocks.iterator();
    }

    private int getLength() {
        return getEnd() - getStart() + 1;
    }

    public boolean isReverse() {
        return reverse;
    }

    private int getStart() {
        return start;
    }

    private int getEnd() {
        return end;
    }

    public double getMiddle() {
        return 0.5 * (getStart() + getEnd());
    }

    /**
     * shows the arrow context menu
     *
     * @param screenX
     * @param screenY
     */
    public void showContextMenu(double screenX, double screenY) {
        final Set<String> accessions = new TreeSet<>();
        for (IMatchBlock matchBlock : getMatchBlocks()) {
            accessions.add(Basic.swallowLeadingGreaterSign(matchBlock.getTextFirstWord()));
            if (accessions.size() == 20)
                break;
        }
        if (accessions.size() > 0) {
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem copy = new MenuItem("Copy Alignments");
            copy.setOnAction(event -> {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                final StringBuilder buf = new StringBuilder();
                boolean first = true;
                for (IMatchBlock matchBlock : getMatchBlocks()) {
                    if (first)
                        first = false;
                    else
                        buf.append("\n");
                    buf.append(matchBlock.getText());
                }
                content.putString(buf.toString());
                clipboard.setContent(content);
            });
            contextMenu.getItems().add(copy);
            contextMenu.getItems().add(new SeparatorMenuItem());

            for (final String accession : accessions) {
                final MenuItem menuItem = new MenuItem("Search on NCBI...");
                menuItem.setOnAction(event -> {
                    try {
                        final String query = accession.replaceAll("gi\\|[0-9]*", "").replaceAll("[a-zA-Z]*\\|", " ").replaceAll("[/\\| ]*]", " ").replaceAll("^\\s+", "").replaceAll("\\s+", "+");
                        BasicSwing.openWebPage(new URL("https://www.ncbi.nlm.nih.gov/protein/" + query));
                    } catch (MalformedURLException e) {
                        Basic.caught(e);
                    }
                });
                contextMenu.getItems().add(menuItem);
            }
            contextMenu.show(this, screenX, screenY);
        }
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(createTooltipString());

        for (IMatchBlock matchBlock : getMatchBlocks()) {
            buf.append("\n");
            buf.append(matchBlock.getText());
        }
        return buf.toString();
    }

    /**
     * creates the tool tip
     *
     * @return tool tip
     */
    private String createTooltipString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("Coordinates: ");
        if (!reverse)
            buf.append(start).append(" - ").append(end);
        else
            buf.append(end).append(" - ").append(start);
        buf.append(String.format(" Length: %d\n", (Math.abs(start - end) + 1)));
        if (matchBlocks.size() > 1)
            buf.append("Matches: ").append(matchBlocks.size()).append("\n");
        for (IMatchBlock matchBlock : getMatchBlocks()) {
            final String accession = Basic.swallowLeadingGreaterSign(matchBlock.getTextFirstWord());
            buf.append(String.format("-- Accession: %s --\n", accession));
            buf.append(String.format("Bit score: %.0f, Expect: %f\n", matchBlock.getBitScore(), matchBlock.getExpected()));
            for (String cName : cNames) {
                int classId = matchBlock.getId(cName);
                if (classId > 0) {
                    String name = ClassificationManager.get(cName, true).getName2IdMap().get(classId);
                    if (name == null)
                        name = "[" + classId + "]";
                    buf.append(String.format("%s: %s\n", cName, Basic.abbreviateDotDotDot(name, 80)));
                }
            }
        }
        return buf.toString();
    }

    static class BitScoreComparator implements Comparator<IMatchBlock> {
        public int compare(IMatchBlock a, IMatchBlock b) {
            if (a.getBitScore() > b.getBitScore())
                return -1;
            else if (a.getBitScore() < b.getBitScore())
                return 1;
            else return Long.compare(a.getUId(), b.getUId());
        }
    }

    public ArrayList<Label> getLabels() {
        return labels;
    }
}
