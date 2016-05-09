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

import megan.data.IMatchBlock;

import java.util.HashMap;
import java.util.Map;


/**
 * Just an adapter for the MEGAN {@link IMatchBlock} with getters and setters.
 *
 * @author Hans-Joachim Ruscheweyh
 *         3:06:58 PM - Oct 27, 2014
 */
public class MatchBlockServer {

    private long matchUid;
    private float bitScore;
    private float percentIdentity;
    private String refSeqId;
    private float expected;
    private int length;
    private boolean ignore;
    private String text;
    private Map<String, Integer> class2id;

    public MatchBlockServer() {

    }

    public MatchBlockServer(IMatchBlock mb, String[] classnames) {
        this.matchUid = mb.getUId();
        class2id = new HashMap<String, Integer>();
        for(String classname : classnames){
        	class2id.put(classname, mb.getId(classname));
        }
        this.bitScore = mb.getBitScore();
        this.percentIdentity = mb.getPercentIdentity();
        this.refSeqId = mb.getRefSeqId();
        this.expected = mb.getExpected();
        this.length = mb.getLength();
        this.ignore = mb.isIgnore();
        this.text = mb.getText();
    }

    public long getMatchUid() {
        return matchUid;
    }

    public void setMatchUid(long uid) {
        this.matchUid = uid;
    }

    public Map<String, Integer> getClass2id() {
		return class2id;
	}

	public void setClass2id(Map<String, Integer> class2id) {
		this.class2id = class2id;
	}

	public float getBitScore() {
        return bitScore;
    }

    public void setBitScore(float bitScore) {
        this.bitScore = bitScore;
    }

    public float getPercentIdentity() {
        return percentIdentity;
    }

    public void setPercentIdentity(float percentIdentity) {
        this.percentIdentity = percentIdentity;
    }

    public String getRefSeqId() {
        return refSeqId;
    }

    public void setRefSeqId(String refSeqId) {
        this.refSeqId = refSeqId;
    }

    public float getExpected() {
        return expected;
    }

    public void setExpected(float expected) {
        this.expected = expected;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
