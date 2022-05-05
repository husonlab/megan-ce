package megan.data.merge;

import megan.data.IMatchBlock;
import megan.data.IReadBlock;

/**
 * readblock used in bundle
 * The getUid() methods contains the file number in its two most signficant bytes
 */
public class MergeReadBlock implements IReadBlock {
	private final IReadBlock readBlock;
	private final int fileNumber;

	public MergeReadBlock(int fileNumber, IReadBlock readBlock) {
		this.fileNumber=fileNumber;
		this.readBlock=readBlock;

	}
	@Override
	public long getUId() {
		return getCombinedFileNumberAndUid(fileNumber,readBlock.getUId());
	}

	@Override
	public void setUId(long uid) {
		readBlock.setUId(uid);

	}

	@Override
	public String getReadName() {
		return readBlock.getReadName();
	}

	@Override
	public String getReadHeader() {
		return readBlock.getReadHeader();
	}

	@Override
	public void setReadHeader(String readHeader) {
		readBlock.setReadHeader(readHeader);

	}

	@Override
	public String getReadSequence() {
		return readBlock.getReadSequence();
	}

	@Override
	public void setReadSequence(String readSequence) {
readBlock.setReadSequence(readSequence);
	}

	@Override
	public long getMateUId() {
		return ((long)fileNumber <<48) | readBlock.getMateUId();
	}

	@Override
	public void setMateUId(long mateReadUId) {
		readBlock.setMateUId(mateReadUId);

	}

	@Override
	public byte getMateType() {
		return readBlock.getMateType();
	}

	@Override
	public void setMateType(byte type) {
		readBlock.setMateType(type);

	}

	@Override
	public void setReadLength(int readLength) {
		readBlock.setReadLength(readLength);
	}

	@Override
	public int getReadLength() {
		return readBlock.getReadLength();
	}

	@Override
	public void setComplexity(float complexity) {
	readBlock.setComplexity(complexity);
	}

	@Override
	public float getComplexity() {
		return readBlock.getComplexity();
	}

	@Override
	public void setReadWeight(int weight) {
readBlock.setReadWeight(weight);
	}

	@Override
	public int getReadWeight() {
		return readBlock.getReadWeight();
	}

	@Override
	public int getNumberOfMatches() {
		return readBlock.getNumberOfMatches();
	}

	@Override
	public void setNumberOfMatches(int numberOfMatches) {
readBlock.setNumberOfMatches(numberOfMatches);
	}

	@Override
	public int getNumberOfAvailableMatchBlocks() {
		return readBlock.getNumberOfAvailableMatchBlocks();
	}

	@Override
	public IMatchBlock[] getMatchBlocks() {
		return readBlock.getMatchBlocks();
	}

	@Override
	public void setMatchBlocks(IMatchBlock[] matchBlocks) {
		readBlock.setMatchBlocks(matchBlocks);
	}

	@Override
	public IMatchBlock getMatchBlock(int i) {
		return readBlock.getMatchBlock(i);
	}

	public static long getCombinedFileNumberAndUid(int fileNumber,long uid) {
		return ((long)fileNumber<<48) | uid;
	}

	public static long getOriginalUid(long uidWithFileNumber) {
		return uidWithFileNumber & 0x00FFFFFFFFFFFFFFFL;
	}

	public static int getFileNumber(long uidWithFileNumber) {
		return (int)(uidWithFileNumber >>>48);
	}
}
