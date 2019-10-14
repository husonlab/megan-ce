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
package rusch.megan5client;

import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.ArrayList;
import java.util.List;


/**
 * Just an adapter for the MEGAN {@link IReadBlock}
 *
 * @author Hans-Joachim Ruscheweyh
 * 3:07:32 PM - Oct 27, 2014
 */
public class ReadBlockServer {
    private long readUid;
    private String readHeader;
    private String readSequence;
    private int readWeight;
    private long mateReadUId;
    private byte mateType;
    private int readLength;
    private float complexity;
    private int numberOfMatches;

    private MatchBlockServer[] matchBlocks = new MatchBlockServer[0];


    public ReadBlockServer() {

    }

    public ReadBlockServer(IReadBlock block, String[] classnames) {
        this.readUid = block.getUId();
        this.readHeader = block.getReadHeader();
        this.readSequence = block.getReadSequence();
        this.readWeight = block.getReadWeight();
        this.mateReadUId = block.getMateUId();
        this.mateType = block.getMateType();
        this.readLength = block.getReadLength();
        this.complexity = block.getComplexity();
        this.numberOfMatches = block.getNumberOfMatches();
        List<MatchBlockServer> mbs = new ArrayList<>();
        if (block.getMatchBlocks() != null) {
            for (IMatchBlock mb : block.getMatchBlocks()) {
                mbs.add(new MatchBlockServer(mb, classnames));
            }
        }
        this.matchBlocks = mbs.toArray(new MatchBlockServer[0]);
    }


    public long getReadUid() {
        return readUid;
    }

    public void setReadUid(long uid) {
        this.readUid = uid;
    }

    public String getReadHeader() {
        return readHeader;
    }

    public void setReadHeader(String readHeader) {
        this.readHeader = readHeader;
    }

    public String getReadSequence() {
        return readSequence;
    }

    public void setReadSequence(String readSequence) {
        this.readSequence = readSequence;
    }

    public int getReadWeight() {
        return readWeight;
    }

    public void setReadWeight(int readWeight) {
        this.readWeight = readWeight;
    }

    public long getMateReadUId() {
        return mateReadUId;
    }

    public void setMateReadUId(long mateReadUId) {
        this.mateReadUId = mateReadUId;
    }

    public byte getMateType() {
        return mateType;
    }

    public void setMateType(byte mateType) {
        this.mateType = mateType;
    }

    public int getReadLength() {
        return readLength;
    }

    public void setReadLength(int readLength) {
        this.readLength = readLength;
    }

    public float getComplexity() {
        return complexity;
    }

    public void setComplexity(float complexity) {
        this.complexity = complexity;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    public void setNumberOfMatches(int numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    public MatchBlockServer[] getMatchBlocks() {
        return matchBlocks;
    }

    public void setMatchBlocks(MatchBlockServer[] matchBlocks) {
        this.matchBlocks = matchBlocks;
    }


}
