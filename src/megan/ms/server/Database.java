/*
 * Copyright (C) 2020. Daniel H. Huson
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
import jloda.util.ProgressPercentage;
import megan.core.Document;
import megan.core.MeganFile;
import megan.core.SampleAttributeTable;
import megan.daa.connector.ClassificationBlockDAA;
import megan.data.*;

import java.io.*;
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
    private final Map<Integer, Record> id2Record = new HashMap<>();
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
        this.fileExtensions = fileExtensions;
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

        final Collection<File> files = Basic.getAllFilesInDirectory(rootDirectory, recursive, fileExtensions);

        final Map<String, Integer> fileName2Id = new HashMap<>();
        final Map<Integer, Record> id2record = new HashMap<>();

        try (ProgressPercentage progress = new ProgressPercentage("Rebuilding database:", files.size())) {
            System.err.println(Basic.getDateString("yyyy-MM-dd hh:mm:ss"));
            for(var file:files) {
                try {
                    final MeganFile meganFile = new MeganFile();
                    meganFile.setFileFromExistingFile(file.getPath(), true);
                    if (meganFile.isMeganSummaryFile()) {
                        final Document document = new Document();
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(Basic.getInputStreamPossiblyZIPorGZIP(file.getPath())))) {
                            document.loadMeganSummary(r);
                            final long numberOfReads = document.getNumberOfReads();
                            final int fileId = fileName2Id.size() + 1;
                            final String relativePath = Basic.getRelativeFile(file, rootDirectory).getPath();
                            fileName2Id.put(relativePath, fileId);
                            try (StringWriter w = new StringWriter()) {
                                document.getDataTable().write(w);
                                document.getSampleAttributeTable().write(w, false, true);
                                final Map<String, byte[]> data = new HashMap<>();
                                data.put("FILE_CONTENT", w.toString().getBytes());
                                id2record.put(fileId, new Record(fileId, file, document.getClassificationNames(), data, numberOfReads, 0));
                            }
                        }
                    } else if (meganFile.hasDataConnector()) {
                        final IConnector connector = meganFile.getConnector();
                        final long numberOfReads = connector.getNumberOfReads();
                        if (numberOfReads > 0) {
                            final long numberOfMatches = connector.getNumberOfMatches();
                            final int fileId = fileName2Id.size() + 1;
                            final String relativePath = Basic.getRelativeFile(file, rootDirectory).getPath();
                            fileName2Id.put(relativePath, fileId);
                            id2record.put(fileId, new Record(fileId, file, Arrays.asList(connector.getAllClassificationNames()), connector.getAuxiliaryData(), numberOfReads, numberOfMatches));
                        }
                    }
                    progress.incrementProgress();
                } catch (IOException ignored) {
                }
            }
        }
        this.fileName2Id.putAll(fileName2Id);
        this.id2Record.putAll(id2record);
        System.err.printf("Files: %,d%n", id2record.size());
        lastRebuild = System.currentTimeMillis();
        return "Rebuild completed at " + (new Date(getLastRebuild()))+"\n";
    }

    public Record getRecord(String fileName) {
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
        final Record record = getRecord(fileName);
        if (record != null && record.getAuxiliaryData() != null && record.getAuxiliaryData().containsKey(SampleAttributeTable.SAMPLE_ATTRIBUTES)) {
            return Basic.toString(record.getAuxiliaryData().get(SampleAttributeTable.SAMPLE_ATTRIBUTES));
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

    public Map<Integer, Record> getId2Record() {
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

    public static class Record {
        private final long fileId;
        private final File file;
        private final List<String> classifications;
        private final Map<String, byte[]> auxiliaryData;
        private final long numberOfReads;
        private final long numberOfMatches;

        public Record(long fileId, File file, List<String> classifications, Map<String, byte[]> auxiliaryData, long numberOfReads, long numberOfMatches) {
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
