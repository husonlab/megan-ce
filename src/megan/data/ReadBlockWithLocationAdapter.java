/*
 * ReadBlockWithLocationAdapter.java Copyright (C) 2022 Daniel H. Huson
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
package megan.data;

/**
 * holds a read block and additional location information
 * Daniel Huson, 2.2011
 */
public class ReadBlockWithLocationAdapter implements IReadBlockWithLocation {
    private final IReadBlock readBlock;
    private IMatchBlockWithLocation[] matchBlocks;
    private Location location;

    public ReadBlockWithLocationAdapter(IReadBlock readBlock, Location location) {
        this.readBlock = readBlock;
        setMatchBlocks(readBlock.getMatchBlocks());
        this.location = location;
    }

    public long getUId() {
        return readBlock.getUId();
    }

    public void setUId(long uid) {
        readBlock.setUId(uid);
    }

    public String getReadName() {
        return readBlock.getReadName();
    }

    public String getReadHeader() {
        return readBlock.getReadHeader();
    }

    public void setReadHeader(String readHeader) {
        readBlock.setReadHeader(readHeader);
    }

    public String getReadSequence() {
        return readBlock.getReadSequence();
    }

    public void setReadSequence(String readSequence) {
        readBlock.setReadSequence(readSequence);
    }

    /**
     * set the weight of a read
     *
     * @param weight
     */
    public void setReadWeight(int weight) {
        readBlock.setReadWeight(weight);
    }

    /**
     * get the weight of a read
     *
     * @return weight
     */
    public int getReadWeight() {
        return readBlock.getReadWeight();
    }

    public long getMateUId() {
        return readBlock.getMateUId();
    }

    public void setMateUId(long mateReadUId) {
        readBlock.setMateUId(mateReadUId);
    }

    public byte getMateType() {
        return readBlock.getMateType();
    }

    public void setMateType(byte type) {
        readBlock.setMateType(type);
    }


    public void setReadLength(int readLength) {
        readBlock.setReadLength(readLength);
    }

    public int getReadLength() {
        return readBlock.getReadLength();
    }

    public void setComplexity(float complexity) {
        readBlock.setComplexity(complexity);
    }

    public float getComplexity() {
        return readBlock.getComplexity();
    }

    public int getNumberOfMatches() {
        return readBlock.getNumberOfMatches();
    }

    public void setNumberOfMatches(int numberOfMatches) {
        readBlock.setNumberOfMatches(numberOfMatches);
    }

    public int getNumberOfAvailableMatchBlocks() {
        return readBlock.getNumberOfAvailableMatchBlocks();
    }

    public IMatchBlockWithLocation[] getMatchBlocks() {
        return matchBlocks;
    }

    public void setMatchBlocks(IMatchBlock[] matchBlocks) {
        this.matchBlocks = new IMatchBlockWithLocation[matchBlocks.length];
        for (int i = 0; i < matchBlocks.length; i++) {
            this.matchBlocks[i] = new MatchBlockWithLocationAdapter(matchBlocks[i], null);
        }
    }

    public String toString() {
        return readBlock + (location != null ? "\n" + location : "");
    }

    public IMatchBlockWithLocation getMatchBlock(int i) {
        return matchBlocks[i];
    }

    public Location getTextLocation() {
        return location;
    }

    public void setTextLocation(Location location) {
        this.location = location;
    }
}
