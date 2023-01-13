package megan.data.merge;

import jloda.util.BiFunctionWithIOException;
import jloda.util.FunctionWithIOException;
import jloda.util.NumberUtils;
import jloda.util.Single;
import jloda.util.progress.ProgressListener;
import megan.core.DataTable;
import megan.core.MeganFile;
import megan.core.SampleAttributeTable;
import megan.core.SyncArchiveAndDataTable;
import megan.data.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * opens a set of DAA or RMA files as a merged summary file
 * Daniel Huson, 5.2022
 */
public class MergeConnector implements IConnector {
	private String fileName;
	private final ArrayList<MeganFile> files=new ArrayList<>();

	private final Map<String,IClassificationBlock> classificationNameBlockMap=new HashMap<>();
	private final Map<String,Integer> classificationSizeMap=new HashMap<>();
	private String[] allClassificationNames;
	private int numberOfReads=0;
	private int numberOfMatches=0;
	private final Map<String, byte[]> auxiliaryData=new HashMap<>();

	public static FunctionWithIOException<Collection<String>,Boolean> checkConnectors;

	static
	{
		checkConnectors=files->{
			var ok=true;
			for(var file:files) {
				if(!MeganFile.hasReadableDAAConnector(file)) {
					ok=false;
					System.err.println("Warning: File not found or not of required type: "+file);
				}
			}
			return ok;
		};
	}

	public MergeConnector(String fileName, Collection<String> inputFiles) throws IOException {
		setFile(fileName);
		setInputFiles(inputFiles);
	}

	@Override
	public String getFilename() {
		return fileName;
	}

	public static boolean canOpenAllConnectors(Collection<String> fileNames) throws IOException {
		return fileNames.size()>0 &&  checkConnectors.apply(fileNames);
	}

	@Override
	public void setFile(String filename) throws IOException {
		this.fileName=filename;
		files.clear();
		classificationSizeMap.clear();
		numberOfReads=0;
		numberOfMatches=0;
	}

	public void setInputFiles(Collection<String> inputFiles) throws IOException {
		files.clear();
		classificationSizeMap.clear();
		numberOfReads=0;
		numberOfMatches=0;

		var classificationNames=new ArrayList<String>();
		for(var name:inputFiles) {
					var meganFile = new MeganFile();
					meganFile.setFileFromExistingFile(name, true);
					if (meganFile.hasDataConnector()) {
						files.add(meganFile);
						var connector=meganFile.getConnector();
						numberOfReads+=connector.getNumberOfReads();
						numberOfMatches+=connector.getNumberOfMatches();
						for(var cName:connector.getAllClassificationNames()) {
							var size=connector.getClassificationSize(cName);
							classificationSizeMap.put(cName,classificationSizeMap.getOrDefault(cName,0)+size);
							if(!classificationNames.contains(cName))
								classificationNames.add(cName);
						}
					}
					else {
						System.err.println("Not a DAA or RMA file, skipped: "+name);
					}
				}
			allClassificationNames=classificationNames.toArray(new String[0]);
			if(files.size()==0)
				throw new IOException("Bundle does not contain any existing DAA or RMA files: "+fileName);
		}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public long getUId() throws IOException {
		return Files.readAttributes(Paths.get(fileName), BasicFileAttributes.class).creationTime().toMillis();
	}

	@Override
	public IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
		return new MergeReadIterator(files, f->f.getConnector().getAllReadsIterator(minScore,maxExpected,wantReadSequence,wantMatches));
	}

	@Override
	public IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
		return new MergeReadIterator(files, f->f.getConnector().getReadsIterator(classification,classId,minScore,maxExpected,wantReadSequence,wantMatches));
	}

	@Override
	public IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
		return new MergeReadIterator(files, f->f.getConnector().getReadsIteratorForListOfClassIds(classification,classIds,minScore,maxExpected,wantReadSequence,wantMatches));
	}

	@Override
	public IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
		return new MergeReaderGetter( NumberUtils.range(0,files.size()), f->files.get(f).getConnector().getReadBlockGetter(minScore,maxExpected,wantReadSequence,wantMatches));
	}

	@Override
	public String[] getAllClassificationNames() throws IOException {
		return allClassificationNames;
	}

	@Override
	public int getClassificationSize(String classificationName) throws IOException {
		return classificationSizeMap.get(classificationName);
	}

	@Override
	public int getClassSize(String classificationName, int classId) throws IOException {
		var size = 0;
		for (var file : files) {
			size+=file.getConnector().getClassSize(classificationName,classId);
		}
		return size;
	}

	@Override
	public IClassificationBlock getClassificationBlock(String classificationName) throws IOException {
		var classificationBlock=classificationNameBlockMap.get(classificationName);
		if(classificationBlock==null) {
			classificationBlock = new MergeClassificationBlock(classificationName, files);
			classificationNameBlockMap.put(classificationName, classificationBlock);
		}
		return classificationBlock;
	}

	@Override
	public void updateClassifications(String[] classificationNames, List<UpdateItem> updateItems, ProgressListener progressListener) throws IOException {
		throw new RuntimeException("Read only");
	}

	@Override
	public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
		return new MergeReadIterator(files, f->f.getConnector().getFindAllReadsIterator(regEx,findSelection,canceled));
	}

	@Override
	public int getNumberOfReads() throws IOException {
		return numberOfReads;
	}

	@Override
	public int getNumberOfMatches() throws IOException {
		return numberOfMatches;
	}

	@Override
	public void setNumberOfReads(int numberOfReads) throws IOException {
		throw new RuntimeException("Read only");
	}

	@Override
	public void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
		throw new RuntimeException("Read only");
	}

	@Override
	public Map<String, byte[]> getAuxiliaryData() throws IOException {
		if(auxiliaryData.size()==0) {
			auxiliaryData.putAll(createAuxiliaryData(fileName,files));
		}
		return auxiliaryData;
	}

	public static Map<String, byte[]> createAuxiliaryData(String fileName,Collection<MeganFile> inputFiles) throws IOException {
		var auxiliaryData=new HashMap<String,byte[]>();
			var table = new DataTable();
			for(var file:inputFiles) {
				var label2data = file.getConnector().getAuxiliaryData();
				if(table.getNumberOfSamples()==0) {
					SyncArchiveAndDataTable.syncAux2Summary(fileName, label2data.get(SampleAttributeTable.USER_STATE), table);
					if (label2data.containsKey(SampleAttributeTable.SAMPLE_ATTRIBUTES)) {
						auxiliaryData.put(SampleAttributeTable.SAMPLE_ATTRIBUTES, label2data.get(SampleAttributeTable.SAMPLE_ATTRIBUTES));
					}
				}
				else {
					// add counts:
					var otherTable=new DataTable();
					SyncArchiveAndDataTable.syncAux2Summary(fileName, label2data.get(SampleAttributeTable.USER_STATE), otherTable);
					for(var classification:otherTable.getClassification2Class2Counts().keySet()){
						var classMap=table.getClass2Counts(classification);
						var otherMap=otherTable.getClass2Counts(classification);
						for(var id:otherMap.keySet()) {
							var other=otherMap.get(id);
							if(other!=null) {
								var array=classMap.get(id);
								if(array!=null) {
									array[0]+=other[0];
								}
								else {
									classMap.put(id,other);
								}
							}
						}
					};
					table.setTotalReads(table.getTotalReads()+otherTable.getTotalReads());
					table.setTotalReadWeights(table.getTotalReadWeights()+otherTable.getTotalReadWeights());
					table.setSamples(new String[]{fileName},new Long[]{table.getSampleUIds()[0]}, new float[]{table.getSampleSizes()[0]+otherTable.getSampleSizes()[0]},table.getBlastModes());
				}
			}
			auxiliaryData.put(SampleAttributeTable.USER_STATE, table.getUserStateAsBytes());
			return auxiliaryData;
		}
}
