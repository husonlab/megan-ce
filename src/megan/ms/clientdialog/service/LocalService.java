/*
 * LocalService.java Copyright (C) 2021. Daniel H. Huson
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
package megan.ms.clientdialog.service;

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.core.DataTable;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.core.SyncArchiveAndDataTable;
import megan.data.IConnector;
import megan.ms.clientdialog.IRemoteService;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * implements a local directory
 * <p/>
 * Created by huson on 10/3/14.
 */
public class LocalService implements IRemoteService {
    private final String[] fileExtensions;
    private final String fullServerDirectory;
    private File rootDirectory;
    private final List<String> files;

    private final Map<String, String> fileName2Description = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock(); // need to look when rescanning directory

    /**
     * constructor
     */
    public LocalService(String rootDirectory, String... fileExtensions) throws IOException {
        fullServerDirectory = "Local::" + rootDirectory;
        files = new LinkedList<>();
        this.fileExtensions = fileExtensions;


        setRootDirectory(new File(rootDirectory));
    }

    /**
     * is this service available?
     *
     * @return true, if data present
     */
    @Override
    public boolean isAvailable() {
        return files.size() > 0;
    }

    /**
     * get a list of available files and their unique ids
     *
     * @return list of available files in format path,id
     */
    @Override
    public List<String> getAvailableFiles() {
        return files;
    }

    /**
     * get the remote server URL and directory path, e.g. www.megan.de/data/files
     *
     * @return file specification
     */
    @Override
    public String getServerURL() {
        return fullServerDirectory;
    }

    /**
     * set the root directory. All files must be below this directory
     */
    private void setRootDirectory(File rootDirectory) throws IOException {
        if (rootDirectory != null) {
            rootDirectory = rootDirectory.getAbsoluteFile();
            if (!rootDirectory.isDirectory())
                throw new IOException("Not a directory: " + rootDirectory);
            if (!rootDirectory.exists())
                throw new IOException("Directory not found: " + rootDirectory);
            if (!rootDirectory.canRead())
                throw new IOException("Cannot read: " + rootDirectory);
        }
        System.err.println("Set root directory to: " + rootDirectory);
        this.rootDirectory = rootDirectory;
    }

    /**
     * rescan root directory and rescan contents
     */
    public void rescan(ProgressListener progress) {
        progress.setSubtask("Scanning...");
        lock.lock();
        try {
            files.clear();
            final Collection<File> files = Basic.getAllFilesInDirectory(rootDirectory, true, fileExtensions);
            for (File file : files) {
                try {
                    final File relative = Basic.getRelativeFile(file, rootDirectory);
                    final Document doc = new Document();

                    doc.getMeganFile().setFileFromExistingFile(file.getPath(), true);
                    if (doc.getMeganFile().hasDataConnector()) {
                        final IConnector connector = doc.getMeganFile().getConnector();

                        final DataTable dataTable = new DataTable();
                        SampleAttributeTable sampleAttributeTable = new SampleAttributeTable();
                        SyncArchiveAndDataTable.syncArchive2Summary(null, doc.getMeganFile().getFileName(), connector, dataTable, sampleAttributeTable);
                        this.files.add(relative.getPath());
                        fileName2Description.put(relative.getPath(), String.format("reads: %,d, matches: %,d", connector.getNumberOfReads(), connector.getNumberOfMatches()));

                    } else if (doc.getMeganFile().isMeganSummaryFile()) {
                        this.files.add(relative.getPath());
                        fileName2Description.put(relative.getPath(), String.format("reads: %,d", doc.getNumberOfReads()));
                    }
                    progress.checkForCancel();
                } catch (CanceledException ex) {
                    break;
                } catch (IOException ignored) {
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * get the full absolute file path
     */
    private String getAbsoluteFilePath(String localFileName) {
        return rootDirectory + File.separator + localFileName;
    }

    /**
     * gets the server and file name
     */
    @Override
    public String getServerAndFileName(String file) {
        return getAbsoluteFilePath(file);
    }

    public String getInfo() {
        return String.format("Number of files: %,d", fileName2Description.size());
    }


    /**
     * get the description associated with a given file name
     */
    public String getDescription(String fileName) {
        return fileName2Description.get(fileName);
    }

}


