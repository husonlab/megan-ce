/*
 * Copyright (C) 2021. Daniel H. Huson
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
 *
 */

package megan.ms.server;

import jloda.util.Basic;
import jloda.util.FileUtils;
import jloda.util.progress.ProgressPercentage;
import jloda.util.StringUtils;
import megan.core.Document;
import megan.core.MeganFile;
import megan.core.SampleAttributeTable;
import megan.daa.connector.ClassificationBlockDAA;
import megan.data.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Megan Server database
 * Daniel Huson, 8.2020
 */
public class Database {
    private final File rootDirectory;
    private final boolean recursive;
    private final String[] fileExtensions;
    private final Map<String, Integer> fileName2Id = new TreeMap<>();
    private final Map<Integer, FileRecord> id2Record = new HashMap<>();
    private final Map<String,File> file2DescriptionFile =new HashMap<>();
    private long lastRebuild = 0;

    /**
     * constructor
     *
     * @param rootDirectory
     * @param fileExtensions
     * @throws IOException
     */
    public Database(File rootDirectory, String[] fileExtensions, boolean recursive) throws IOException {
        if (!rootDirectory.isDirectory())
            throw new IOException("Not a directory: " + rootDirectory);
        if (!rootDirectory.canRead())
            throw new IOException("Not readable: " + rootDirectory);

        this.rootDirectory = rootDirectory;
        this.fileExtensions =fileExtensions;
        this.recursive = recursive;
    }

    /**
     * rebuild the database
     *
     * @return message
     */
    public String rebuild() {
        fileName2Id.clear();
        id2Record.clear();
        file2DescriptionFile.clear();

		final var files = FileUtils.getAllFilesInDirectory(rootDirectory, recursive, fileExtensions);

        final var fileName2IdRebuilt = new HashMap<String, Integer>();
        final var id2record = new HashMap<Integer, FileRecord>();

        try (ProgressPercentage progress = new ProgressPercentage("Rebuilding database:", files.size())) {
            System.err.println(Basic.getDateString("yyyy-MM-dd hh:mm:ss"));
            for(var file:files) {
                try {
					if (FileUtils.fileExistsAndIsNonEmpty(FileUtils.replaceFileSuffix(file, ".txt"))) {
						file2DescriptionFile.put(FileUtils.getRelativeFile(file, rootDirectory).getPath(), FileUtils.replaceFileSuffix(file, ".txt"));
					}
					final var meganFile = new MeganFile();
					meganFile.setFileFromExistingFile(file.getPath(), true);
					if (meganFile.isMeganSummaryFile()) {
						final var document = new Document();
						try (var r = new BufferedReader(new InputStreamReader(FileUtils.getInputStreamPossiblyZIPorGZIP(file.getPath())))) {
							document.loadMeganSummary(r);
							final var numberOfReads = document.getNumberOfReads();
							final var fileId = fileName2IdRebuilt.size() + 1;
							final String relativePath = FileUtils.getRelativeFile(file, rootDirectory).getPath();
							fileName2IdRebuilt.put(relativePath, fileId);
							try (var w = new StringWriter()) {
								document.getDataTable().write(w);
								document.getSampleAttributeTable().write(w, false, true);
								final var data = new HashMap<String, byte[]>();
								data.put("FILE_CONTENT", w.toString().getBytes());
                                    id2record.put(fileId, new FileRecord(fileId, file, document.getClassificationNames(),data,numberOfReads,0));
                                }
                            }
                        } else if (meganFile.hasDataConnector()) {
                            final var connector = meganFile.getConnector();
                            final var numberOfReads = connector.getNumberOfReads();
                            if (numberOfReads > 0) {
                                final var numberOfMatches = connector.getNumberOfMatches();
								final var fileId = fileName2IdRebuilt.size() + 1;
								final var relativePath = FileUtils.getRelativeFile(file, rootDirectory).getPath();
                                fileName2IdRebuilt.put(relativePath, fileId);
                                id2record.put(fileId, new FileRecord(fileId, file, Arrays.asList(connector.getAllClassificationNames()), connector.getAuxiliaryData(),
                                        numberOfReads,numberOfMatches));
                            }
                        }
                    progress.incrementProgress();
                } catch (IOException ignored) {
                }
            }
        }

		for (var aboutFile : FileUtils.getAllFilesInDirectory(rootDirectory, recursive, "About.txt")) {
			try {
				var relativePath = FileUtils.getRelativeFile(aboutFile.getParentFile(), rootDirectory).getPath();
				file2DescriptionFile.put(relativePath, aboutFile);
			} catch (IOException ignored) {
			}
		}

        this.fileName2Id.putAll(fileName2IdRebuilt);
        this.id2Record.putAll(id2record);
        System.err.printf("Files: %,d%n", id2record.size());
        lastRebuild = System.currentTimeMillis();
        return "Rebuild '"+rootDirectory.getName()+"' completed at " + (new Date(getLastRebuild()))+"\n";
    }

    public FileRecord getRecord(String fileName) {
        final Integer fileId;
        if (Basic.isInteger(fileName))
            fileId = Basic.parseInt(fileName);
        else
            fileId = fileName2Id.get(fileName);
        if (fileId != null)
            return id2Record.get(fileId);
        else
            return null;
    }

    public String getMetadata(String fileName) {
        final FileRecord fileRecord = getRecord(fileName);
        if (fileRecord != null && fileRecord.getAuxiliaryData() != null && fileRecord.getAuxiliaryData().containsKey(SampleAttributeTable.SAMPLE_ATTRIBUTES)) {
			return StringUtils.toString(fileRecord.getAuxiliaryData().get(SampleAttributeTable.SAMPLE_ATTRIBUTES));
        } else
            return "";
    }

    public String getInfo() {
        return String.format("directory %s, %,d files", rootDirectory, fileName2Id.size());
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public Map<String, Integer> getFileName2Id() {
        return fileName2Id;
    }

    public Collection<String> getFileNames() {
        return fileName2Id.keySet();
    }

    public Map<Integer, FileRecord> getId2Record() {
        return id2Record;
    }

    public List<String> getClassifications(String fileName) {
        return getRecord(fileName).getClassifications();
    }

    public IClassificationBlock getClassificationBlock(String fileName, String classification) throws IOException {
        final Document document = new Document();
        final File file = getRecord(fileName).getFile();
        document.getMeganFile().setFileFromExistingFile(file.getPath(), true);
        if (document.getMeganFile().hasDataConnector()) {
            final IConnector connector = document.getMeganFile().getConnector();
            return connector.getClassificationBlock(classification);
        } else {
            document.loadMeganFile();
            final ClassificationBlockDAA classificationBlock = new ClassificationBlockDAA(classification);
            if (document.getClassificationNames().contains(classification)) {
                final Map<Integer, float[]> class2count = document.getDataTable().getClass2Counts(classification);
                for (var classId : class2count.keySet()) {
                    classificationBlock.setSum(classId, Math.round(Basic.getSum(class2count.get(classId))));
                }
            }
            return classificationBlock;
        }
    }

    public IReadBlock getRead(String fileName, long readUid, boolean includeMatches) throws IOException {
        try (IReadBlockGetter getter = getConnector(fileName).getReadBlockGetter(0, 10, true, includeMatches)) {
            return getter.getReadBlock(readUid);
        }
    }

    public ReadIteratorPagination.Page getReads(String fileName, ReadsOutputFormat format, int pageSize) throws IOException {
        final IReadBlockIterator iterator = getConnector(fileName).getAllReadsIterator(0, 10, format.isSequences(), format.isMatches());
        long pageId = ReadIteratorPagination.createPagination(iterator, getConnector(fileName).getAllClassificationNames(), format, pageSize);
        return getNextPage(pageId, -1);
    }

    public ReadIteratorPagination.Page getReadsForMultipleClassIds(String fileName, String classification, Collection<Integer> classIds, ReadsOutputFormat format, int pageSize) throws IOException {
        final IReadBlockIterator iterator = getConnector(fileName).getReadsIteratorForListOfClassIds(classification, classIds, 0, 10, format.isSequences(), format.isMatches());
        long pageId = ReadIteratorPagination.createPagination(iterator, getConnector(fileName).getAllClassificationNames(), format, pageSize);
        return getNextPage(pageId, -1);
    }

    public ReadIteratorPagination.Page getNextPage(long pageId, int pageSize) {
        return ReadIteratorPagination.getNextPage(pageId, pageSize);
    }

    private IConnector getConnector(String fileName) throws IOException {
        final MeganFile meganFile = new MeganFile();
        final File file = getRecord(fileName).getFile();
        meganFile.setFileFromExistingFile(file.getPath(), true);
        return meganFile.getConnector();
    }

    public long getLastRebuild() {
        return lastRebuild;
    }

    public String getFileDescription(String fileName) throws IOException {
        var file= file2DescriptionFile.get(fileName);
		if (FileUtils.fileExistsAndIsNonEmpty(file))
			return Files.readString(file.toPath());
		else
			return null;
    }

    public static class FileRecord {
        private final long fileId;
        private final File file;
        private final List<String> classifications;
        private final Map<String, byte[]> auxiliaryData;
        private final long numberOfReads;
        private final long numberOfMatches;

        public FileRecord(long fileId, File file, List<String> classifications, Map<String, byte[]> auxiliaryData, long numberOfReads, long numberOfMatches) {
            this.fileId = fileId;
            this.file = file;
            this.classifications = new ArrayList<>(classifications);
            this.auxiliaryData = auxiliaryData;
            this.numberOfReads = numberOfReads;
            this.numberOfMatches = numberOfMatches;
        }

        public long getFileId() {
            return fileId;
        }

        public File getFile() {
            return file;
        }

        public List<String> getClassifications() {
            return classifications;
        }

        public Map<String, byte[]> getAuxiliaryData() {
            return auxiliaryData;
        }

        public long getNumberOfReads() {
            return numberOfReads;
        }

        public long getNumberOfMatches() {
            return numberOfMatches;
        }
    }
}
