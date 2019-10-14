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
package megan.remote.client;

import jloda.swing.util.BasicSwing;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.core.DataTable;
import megan.core.MeganFile;
import megan.core.SampleAttributeTable;
import megan.core.SyncArchiveAndDataTable;
import megan.data.IConnector;
import megan.remote.IRemoteService;
import megan.rma3.RMAFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * implements a local directory
 * <p/>
 * Created by huson on 10/3/14.
 */
public class LocalService implements IRemoteService {
    private final String fullServerDirectory;
    private File rootDirectory;
    private final List<String> files;

    private final Map<String, String> fileName2Description = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock(); // need to look when rescanning directory

    /**
     * constructor
     *
     * @path path to root directory
     */
    public LocalService(String rootDirectory) throws IOException {
        fullServerDirectory = "Local::" + rootDirectory;
        files = new LinkedList<>();

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
     *
     * @param rootDirectory
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
    public void rescan(ProgressListener progress) throws IOException, CanceledException {
        progress.setSubtask("Scanning...");
        lock.lock();
        try {
            files.clear();
            List<File> files = BasicSwing.getAllFilesInDirectory(rootDirectory, RMAFileFilter.getInstance(), true);
            for (File file : files) {
                File relative = Basic.getRelativeFile(file, rootDirectory);
                this.files.add(relative.getPath());
                MeganFile meganFile = new MeganFile();
                meganFile.setFileFromExistingFile(file.getPath(), true);
                if (meganFile.hasDataConnector()) {
                    IConnector connector = meganFile.getConnector();
                    DataTable dataTable = new DataTable();
                    SampleAttributeTable sampleAttributeTable = new SampleAttributeTable();
                    SyncArchiveAndDataTable.syncArchive2Summary(null, meganFile.getFileName(), connector, dataTable, sampleAttributeTable);
                    Object description = sampleAttributeTable.get(meganFile.getName(), "Description");
                    if (description == null)
                        description = meganFile.getName();
                    fileName2Description.put(relative.getParent(), description.toString());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * get a short name for the server
     *
     * @return short name
     */
    @Override
    public String getShortName() {
        return getServerURL();
    }

    /**
     * get the full absolute file path
     *
     * @param localFileName
     * @return full file path
     */
    private String getAbsoluteFilePath(String localFileName) {
        return rootDirectory + File.separator + localFileName;
    }

    /**
     * gets the server and file name
     *
     * @param file
     * @return server and file
     */
    @Override
    public String getServerAndFileName(String file) {
        return getAbsoluteFilePath(file);
    }

    public String getInfo() {
        return "<html> Local files</html>";
    }


    /**
     * get the description associated with a given file name
     *
     * @param fileName
     * @return description
     */
    public String getDescription(String fileName) {
        return fileName2Description.get(fileName);
    }

}


