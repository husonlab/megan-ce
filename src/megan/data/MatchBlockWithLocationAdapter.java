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
package megan.data;

/**
 * adapts a match block to be a match block with location
 * Daniel Huson, 2.2011
 */
public class MatchBlockWithLocationAdapter implements IMatchBlock, IMatchBlockWithLocation {
    private final IMatchBlock matchBlock;
    private Location location;

    public MatchBlockWithLocationAdapter(IMatchBlock matchBlock, Location location) {
        this.matchBlock = matchBlock;
        this.location = location;
    }

    public long getUId() {
        return matchBlock.getUId();
    }

    public void setUId(long uid) {
        matchBlock.setUId(uid);
    }

    public int getTaxonId() {
        return matchBlock.getTaxonId();
    }

    public void setTaxonId(int taxonId) {
        matchBlock.setTaxonId(taxonId);
    }

    @Override
    public int getId(String cName) {
        return matchBlock.getId(cName);
    }

    @Override
    public void setId(String cName, Integer id) {
        matchBlock.setId(cName, id);
    }

    /**
     * gets all defined ids
     *
     * @param cNames
     * @return ids
     */
    public int[] getIds(String[] cNames) {
        return matchBlock.getIds(cNames);
    }

    public float getBitScore() {
        return matchBlock.getBitScore();
    }

    public void setBitScore(float bitScore) {
        matchBlock.setBitScore(bitScore);
    }

    public float getPercentIdentity() {
        return matchBlock.getPercentIdentity();
    }

    public void setPercentIdentity(float percentIdentity) {
        matchBlock.setPercentIdentity(percentIdentity);
    }

    public String getRefSeqId() {
        return matchBlock.getRefSeqId();
    }

    public void setRefSeqId(String refSeqId) {
        matchBlock.setRefSeqId(refSeqId);
    }

    public void setExpected(float expected) {
        matchBlock.setExpected(expected);
    }

    public float getExpected() {
        return matchBlock.getExpected();
    }

    public void setLength(int length) {
        matchBlock.setLength(length);
    }

    public int getLength() {
        return matchBlock.getLength();
    }

    public boolean isIgnore() {
        return matchBlock.isIgnore();
    }

    public void setIgnore(boolean ignore) {
        matchBlock.setIgnore(ignore);
    }

    public String getText() {
        return matchBlock.getText();
    }

    @Override
    public String getTextFirstWord() {
        return matchBlock.getTextFirstWord();
    }

    public void setText(String text) {
        matchBlock.setText(text);
    }

    public String toString() {
        return matchBlock + (location != null ? "\n" + location : "");
    }

    public Location getTextLocation() {
        return location;
    }

    public void setTextLocation(Location location) {
        this.location = location;
    }
}

