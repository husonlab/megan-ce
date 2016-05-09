/**
 * Copyright (C) 2016 Daniel H. Huson
 * Author Hans-Joachim Ruscheweyh
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rusch.megan5client;

import jloda.util.Basic;
import megan.classification.Classification;
import megan.data.IMatchBlock;


/**
 * Adapter for Server Matchblocks
 *
 * @author Hans-Joachim Ruscheweyh
 *         3:28:28 PM - Oct 27, 2014
 */
public class MatchBlock implements IMatchBlock {
    private final MatchBlockServer block;

    public MatchBlock(MatchBlockServer matchBlockServer) {
        this.block = matchBlockServer;
    }

    @Override
    public long getUId() {
        return block.getMatchUid();
    }

    @Override
    public void setUId(long uid) {
        block.setMatchUid(uid);

    }

    @Override
    public int getTaxonId() {
        return getId(Classification.Taxonomy);
    }

    @Override
    public void setTaxonId(int taxonId) {
    	setId(Classification.Taxonomy, taxonId);

    }

    @Override
    public float getBitScore() {
        return block.getBitScore();
    }

    @Override
    public void setBitScore(float bitScore) {
        block.setBitScore(bitScore);

    }

    @Override
    public float getPercentIdentity() {
        return block.getPercentIdentity();
    }

    @Override
    public void setPercentIdentity(float percentIdentity) {
        block.setPercentIdentity(percentIdentity);

    }


    @Override
    public String getRefSeqId() {
        return block.getRefSeqId();
    }

    @Override
    public void setRefSeqId(String refSeqId) {
        block.setRefSeqId(refSeqId);

    }

    @Override
    public void setExpected(float expected) {
        block.setExpected(expected);

    }

    @Override
    public float getExpected() {
        return block.getExpected();
    }

    @Override
    public void setLength(int length) {
        block.setLength(length);

    }

    @Override
    public int getLength() {
        return block.getLength();
    }

    @Override
    public boolean isIgnore() {
        return block.isIgnore();
    }

    @Override
    public void setIgnore(boolean ignore) {
        block.setIgnore(ignore);

    }

    @Override
    public String getText() {
        return block.getText();
    }

    @Override
    public String getTextFirstWord() {
        return block.getText() != null ? Basic.getFirstWord(block.getText()) : null;
    }

    @Override
    public void setText(String text) {
        block.setText(text);

    }

    @Override
    public int getId(String cName) {
    	Integer id = block.getClass2id().get(cName);
        return id != null ? id : 0;
    }

    /**
     * gets all defined ids
     *
     * @param cNames
     * @return ids
     */
    public int[] getIds(String[] cNames) {
        int[] ids = new int[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            ids[i] = getId(cNames[i]);
        }
        return ids;
    }

    @Override
    public void setId(String cName, Integer id) {
        block.getClass2id().put(cName, id);
    }
}
