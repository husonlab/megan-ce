package megan.data.merge;

import jloda.util.FunctionWithIOException;
import megan.core.MeganFile;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * iterates over all reads in a bundle
 */
public class MergeReadIterator implements IReadBlockIterator {
	private int whichFile=-1;
	private final long numberOfFiles;
	private IReadBlockIterator current;
	private final Iterator<IReadBlockIterator> metaIterator;

	private long countReads =0;

	public MergeReadIterator(ArrayList<MeganFile> files, FunctionWithIOException<MeganFile,IReadBlockIterator> fileIteratorFunction){
		numberOfFiles =files.size();

		metaIterator =new Iterator<>() {
			@Override
			public boolean hasNext() {
				return whichFile<files.size();
			}
			@Override
			public IReadBlockIterator next() {
				try {
					return fileIteratorFunction.apply(files.get(++whichFile));
				} catch (Exception e) {
					return null;
				}
			}
		};
		current= metaIterator.next();
		while(current!=null && !current.hasNext()) {
			try {
				current.close();
			} catch (IOException ignored) {
			}
			if(metaIterator.hasNext())
				current= metaIterator.next();
			else
				current=null;
		}
	}

	@Override
	public void close() throws IOException {
		if(current!=null)
			current.close();
	}

	@Override
	public long getMaximumProgress() {
		return numberOfFiles;
	}

	@Override
	public long getProgress() {
		return whichFile;
	}

	@Override
	public String getStats() {
		return "Reads: " + countReads;
	}

	@Override
	public boolean hasNext() {
		return current!=null && current.hasNext();
	}

	@Override
	public IReadBlock next() {
		if(current==null)
			throw new NoSuchElementException();
		var next=current.next();
		while(current!=null && !current.hasNext()) {
			try {
				current.close();
			} catch (IOException ignored) {
			}
			if(metaIterator.hasNext())
			current= metaIterator.next();
			else
				current=null;
		}
		countReads++;
		return next;
	}

}
