package megan.data.merge;

import jloda.util.FunctionWithIOException;
import megan.data.IReadBlock;
import megan.data.IReadBlockGetter;

import java.io.IOException;
import java.util.*;

/**
 * read getter for a bundle of files
 * Daniel Huson, 5.2022
 */
public class MergeReaderGetter implements IReadBlockGetter {
	private final Set<Integer> fileNumbers=new TreeSet<>();
	private final Map<Integer,IReadBlockGetter> fileGetterMap=new HashMap<>();
	private final FunctionWithIOException<Integer,IReadBlockGetter> fileReadGetterFunction;
	private long count=0;

	public MergeReaderGetter(Collection<Integer> fileNumbers, FunctionWithIOException<Integer,IReadBlockGetter> fileIReadBlockGetterFunction) throws IOException {
		this.fileNumbers.addAll(fileNumbers);
		this.fileReadGetterFunction =fileIReadBlockGetterFunction;
	}
		@Override
	public IReadBlock getReadBlock(long uid) throws IOException {
		var fileId= MergeReadBlock.getFileNumber(uid);
		if(fileNumbers.contains(fileId)) {
			uid = MergeReadBlock.getOriginalUid(uid);
			if(uid>=0) {
				var readGetter = fileGetterMap.get(fileId);
				if (readGetter == null) {
					readGetter = fileReadGetterFunction.apply(fileId);
					fileGetterMap.put(fileId, readGetter);
				}
				return readGetter.getReadBlock(uid);
			}
		}
		return null;
	}

	@Override
	public void close() {
		for(var readGetter:fileGetterMap.values()) {
			readGetter.close();
		}
	}

	@Override
	public long getCount() {
		if(count==0) {
			for(var fileId:fileNumbers) {
				var readGetter = fileGetterMap.get(fileId);
				if (readGetter == null) {
					try {
						readGetter = fileReadGetterFunction.apply(fileId);
						fileGetterMap.put(fileId, readGetter);
						count+=readGetter.getCount();
					} catch (IOException ignored) {
					}
				}
			}
		}
		return count;
	}
}
