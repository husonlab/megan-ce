/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.util.Basic;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;


/**
 * Adapter for Server ReadBlocks
 *
 * @author Hans-Joachim Ruscheweyh
 *         3:28:14 PM - Oct 27, 2014
 */
public class ReadBlock implements IReadBlock {
    private final ReadBlockServer block;
    private IMatchBlock[] mblocks;

    public ReadBlock(ReadBlockServer readBlockServer) {
        this.block = readBlockServer;
        if (readBlockServer.getMatchBlocks() == null) {
            mblocks = new MatchBlock[0];
        } else {
            mblocks = new MatchBlock[readBlockServer.getMatchBlocks().length];
            for (int i = 0; i < readBlockServer.getMatchBlocks().length; i++) {
                mblocks[i] = new MatchBlock(readBlockServer.getMatchBlocks()[i]);
            }
        }

    }

    @Override
    public long getUId() {
        return block.getReadUid();
    }

    @Override
    public void setUId(long uid) {
        block.setReadUid(uid);

    }

    @Override
    public String getReadName() {
        if (block.getReadHeader() != null) {
            String name = Basic.getFirstWord(block.getReadHeader());
            if (name.startsWith(">"))
                return name.substring(1).trim();
            else
                return name;
        }
        return null;
    }

    @Override
    public String getReadHeader() {
        return block.getReadHeader();
    }

    @Override
    public void setReadHeader(String readHeader) {
        block.setReadHeader(readHeader);

    }

    @Override
    public String getReadSequence() {
        return block.getReadSequence();
    }

    @Override
    public void setReadSequence(String readSequence) {
        block.setReadSequence(readSequence);

    }

    @Override
    public long getMateUId() {
        return block.getMateReadUId();
    }

    @Override
    public void setMateUId(long mateReadUId) {
        block.setMateReadUId(mateReadUId);

    }

    @Override
    public byte getMateType() {
        return block.getMateType();
    }

    @Override
    public void setMateType(byte type) {
        block.setMateType(type);

    }

    @Override
    public void setReadLength(int readLength) {
        block.setReadLength(readLength);

    }

    @Override
    public int getReadLength() {
        return block.getReadLength();
    }

    @Override
    public void setComplexity(float complexity) {
        block.setComplexity(complexity);

    }

    @Override
    public float getComplexity() {
        return block.getComplexity();
    }

    @Override
    public void setReadWeight(int weight) {
        block.setReadWeight(weight);

    }

    @Override
    public int getReadWeight() {
        return block.getReadWeight();
    }

    @Override
    public int getNumberOfMatches() {
        return block.getNumberOfMatches();
    }

    @Override
    public void setNumberOfMatches(int numberOfMatches) {
        block.setNumberOfMatches(numberOfMatches);

    }

    @Override
    public int getNumberOfAvailableMatchBlocks() {
        return mblocks.length;
    }

    @Override
    public IMatchBlock[] getMatchBlocks() {
        return mblocks;
    }

    @Override
    public void setMatchBlocks(IMatchBlock[] matchBlocks) {
        mblocks = matchBlocks;

    }

    @Override
    public IMatchBlock getMatchBlock(int i) {
        return mblocks[i];
    }


}
