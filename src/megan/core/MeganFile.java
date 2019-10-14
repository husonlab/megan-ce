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
package megan.core;

import jloda.util.Pair;
import megan.daa.connector.DAAConnector;
import megan.data.ConnectorCombiner;
import megan.data.IConnector;
import megan.remote.client.RemoteServiceManager;
import megan.rma2.RMA2Connector;
import megan.rma2.RMA2File;
import megan.rma3.RMA3Connector;
import megan.rma6.RMA6Connector;
import rusch.megan5client.connector.Megan5ServerConnector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * manages MEGAN file associated with a document, can be an RMA file, a summary file or a MEGAN server file
 * Daniel Huson, 11.2012
 */
public class MeganFile {
    private final static Map<Pair<String, Long>, Integer> openFiles = new HashMap<>();
    private String fileName = null;
    private Type fileType = Type.UNKNOWN_FILE;
    private boolean readOnly = false;

    private IConnector connector;

    public enum Type {UNKNOWN_FILE, RMA1_FILE, RMA2_FILE, RMA3_FILE, RMA6_FILE, DAA_FILE, MEGAN_SUMMARY_FILE, MEGAN_SERVER_FILE}

    private final ArrayList<String> embeddedSourceFiles = new ArrayList<>();

    /**
     * set the megan file from an existing file
     *
     * @param fileName
     * @param readOnly
     */
    public void setFileFromExistingFile(String fileName, boolean readOnly) {
        if (!fileName.equals(this.fileName))
            connector = null;
        this.fileName = fileName;
        this.readOnly = readOnly;

        if (fileName.contains("::")) {
            fileType = Type.MEGAN_SERVER_FILE;
        } else if (fileName.toLowerCase().endsWith(".rma1")) {
            fileType = Type.RMA1_FILE;
        } else if (fileName.toLowerCase().endsWith(".rma2")) {
            fileType = Type.RMA2_FILE;
        } else if (fileName.toLowerCase().endsWith(".rma3")) {
            fileType = Type.RMA3_FILE;
        } else if (fileName.toLowerCase().endsWith(".rma6")) {
            fileType = Type.RMA6_FILE;
        } else if (fileName.toLowerCase().endsWith(".rma")) {
            int version = RMA2File.getRMAVersion(new File(fileName));
            if (version == 1)
                fileType = Type.RMA1_FILE;
            else if (version == 2)
                fileType = Type.RMA2_FILE;
            else if (version == 3)
                fileType = Type.RMA3_FILE;
            else if (version == 6)
                fileType = Type.RMA6_FILE;
            else
                fileType = Type.UNKNOWN_FILE;
        } else if (fileName.toLowerCase().endsWith(".daa")) {
            fileType = Type.DAA_FILE;
        } else if (fileName.toLowerCase().endsWith(".meg") || fileName.toLowerCase().endsWith(".megan")
                || fileName.toLowerCase().endsWith(".meg.gz") || fileName.toLowerCase().endsWith(".megan.gz")
                || fileName.toLowerCase().endsWith(".meg.zip") || fileName.toLowerCase().endsWith(".megan.zip")) {
            fileType = Type.MEGAN_SUMMARY_FILE;
            setEmbeddedSourceFiles(determineEmbeddedSourceFiles(fileName));
        } else
            fileType = Type.UNKNOWN_FILE;
    }

    /**
     * set new file of given type
     *
     * @param fileName
     * @param fileType
     */
    public void setFile(String fileName, Type fileType) {
        if (fileName == null || !fileName.equals(this.fileName))
            connector = null;
        this.fileName = fileName;
        this.fileType = fileType;
        readOnly = false;
    }

    /**
     * is the set file ok to read?
     *
     * @return true, if file exists and of correct type
     */
    public void checkFileOkToRead() throws IOException {
        if (fileType == Type.MEGAN_SERVER_FILE)
            return;

        File file = new File(fileName);
        if (!file.canRead())
            throw new IOException("File not readable: " + fileName);

        switch (fileType) {
            case RMA1_FILE: {
                throw new IOException("RMA version 1 not supported: " + fileName);
            }
            case RMA2_FILE: {
                int version = RMA2File.getRMAVersion(file);
                if (version != 2)
                    throw new IOException("RMA version (" + version + ") not supported: " + fileName);
                if (!file.canWrite())
                    setReadOnly(true);
                return;
            }
            case RMA3_FILE:
            case RMA6_FILE:
            case DAA_FILE:
            case MEGAN_SUMMARY_FILE:
                if (!file.canWrite())
                    setReadOnly(true);
                return;
            default:
                throw new IOException("File has unknown type: " + fileName);
        }
    }

    /**
     * is file ok to read?
     *
     * @return true, if ok to read
     */
    public boolean isOkToRead() {
        try {
            checkFileOkToRead();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public String getFileName() {
        return Objects.requireNonNullElse(fileName, "Untitled");
    }

    public void setFileName(String fileName) {
        if (fileName == null || !fileName.equals(this.fileName))
            connector = null;
        this.fileName = fileName;
    }

    public Type getFileType() {
        return fileType;
    }

    public void setFileType(Type fileType) {
        this.fileType = fileType;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isMeganSummaryFile() {
        return fileType == Type.MEGAN_SUMMARY_FILE;
    }

    public boolean isMeganServerFile() {
        return fileType == Type.MEGAN_SERVER_FILE;
    }


    public boolean isUnsupportedRMA1File() {
        return fileType == Type.RMA1_FILE;
    }

    public boolean isRMA2File() {
        return fileType == Type.RMA2_FILE;
    }

    public boolean isRMA3File() {
        return fileType == Type.RMA3_FILE;
    }

    public boolean isRMA6File() {
        return fileType == Type.RMA6_FILE;
    }

    public boolean isDAAFile() {
        return fileType == Type.DAA_FILE;
    }


    /**
     * does file have  a data connector associated with it
     *
     * @return true, if data connector present
     */
    public boolean hasDataConnector() {
        try {
            return fileName != null && fileName.length() > 0 && (fileType.toString().startsWith("RMA") || fileType.toString().startsWith("DAA") || fileType == Type.MEGAN_SERVER_FILE
                    || (fileType == Type.MEGAN_SUMMARY_FILE && embeddedSourceFiles.size() > 0 && ConnectorCombiner.canOpenAllConnectors(embeddedSourceFiles)));
        } catch (IOException e) {
            return false;
        }
    }


    /**
     * get the data connector associated with the file
     *
     * @return data connector
     * @throws IOException
     */
    public IConnector getConnector() throws IOException {
        return getConnector(true);
    }

    /**
     * get the data connector associated with the file
     *
     * @return data connector
     * @throws IOException
     */
    IConnector getConnector(boolean openDAAFileOnlyIfMeganized) throws IOException {
        if (connector == null) {
            switch (fileType) {
                case MEGAN_SERVER_FILE: {
                    final String serverURL = RemoteServiceManager.getServerURL(fileName);
                    final String user = RemoteServiceManager.getUser(fileName);
                    final String password = RemoteServiceManager.getPassword(fileName);
                    final String filePath = RemoteServiceManager.getFilePath(fileName);
                    final IConnector connector = new Megan5ServerConnector(serverURL, user, password);
                    connector.setFile(filePath);
                    this.connector = connector;
                    break;
                }
                case RMA2_FILE: {
                    connector = new RMA2Connector(fileName);
                    break;
                }
                case RMA3_FILE: {
                    connector = new RMA3Connector(fileName);
                    break;
                }
                case RMA6_FILE: {
                    connector = new RMA6Connector(fileName);
                    break;
                }
                case DAA_FILE: {
                    synchronized (DAAConnector.syncObject) {
                        boolean save = DAAConnector.openDAAFileOnlyIfMeganized;
                        DAAConnector.openDAAFileOnlyIfMeganized = openDAAFileOnlyIfMeganized;
                        connector = new DAAConnector(fileName);
                        DAAConnector.openDAAFileOnlyIfMeganized = save;
                    }
                    break;
                }
                case MEGAN_SUMMARY_FILE: {
                    if (embeddedSourceFiles.size() > 0) {
                        connector = new ConnectorCombiner(embeddedSourceFiles);
                        break;
                    }
                    // else fall through to default:
                }
                default:
                    throw new IOException("File type '" + fileType.toString() + "': not supported");
            }
        }
        return connector;
    }

    /**
     * gets a short name for the file
     *
     * @return short name
     */
    public String getName() {
        if (fileName == null)
            return "Untitled";

        int p = fileName.lastIndexOf(isMeganServerFile() ? '/' : File.separatorChar);

        if (p >= 0 && p < fileName.length() - 1)
            return fileName.substring(p + 1);
        else
            return fileName;
    }

    /**
     * add the unique identify of a file to the set of open files
     *
     * @param uId
     * @return true, if added, false, if already present
     */
    public static boolean addUIdToSetOfOpenFiles(String name, long uId) {
        final Pair<String, Long> pair = new Pair<>(name, uId);
        final Integer count = openFiles.get(pair);
        if (count == null) {
            openFiles.put(pair, 1);
            return true;
        } else {
            openFiles.put(pair, count + 1);
            return false;
        }
    }

    /**
     * removes the UID of a file from the set of open files
     *
     * @param uId
     */
    public static void removeUIdFromSetOfOpenFiles(String name, long uId) {
        final Pair<String, Long> pair = new Pair<>(name, uId);
        Integer count = openFiles.get(pair);
        if (count == null || count < 2) {
            openFiles.keySet().remove(pair);
        } else
            openFiles.put(pair, count - 1);
    }

    /**
     * determines whether UID of file is present in the set of all open files
     *
     * @param uId
     * @return true, if present
     */
    public static boolean isUIdContainedInSetOfOpenFiles(String name, long uId) {
        final Pair<String, Long> pair = new Pair<>(name, uId);
        Integer count = openFiles.get(pair);
        return count != null && count > 0;
    }

    /**
     * gets the names of all source files embedded in a comparison file
     *
     * @param fileName
     * @return embedded source files
     */
    private static ArrayList<String> determineEmbeddedSourceFiles(String fileName) {
        final Document doc = new Document();
        doc.getMeganFile().setFile(fileName, Type.MEGAN_SUMMARY_FILE);
        try {
            doc.loadMeganFile();
        } catch (Exception e) {
            //Basic.caught(e);
        }
        return doc.getSampleAttributeTable().getSourceFiles();
    }

    /**
     * get the embedded source files
     *
     * @return get
     */
    public ArrayList<String> getEmbeddedSourceFiles() {
        return embeddedSourceFiles;
    }

    /**
     * set the embedded source files
     *
     * @param embeddedSourceFiles
     */
    public void setEmbeddedSourceFiles(ArrayList<String> embeddedSourceFiles) {
        this.embeddedSourceFiles.clear();

        if (embeddedSourceFiles != null) {
            final ArrayList<String> filtered = new ArrayList<>();
            for (String fileName : embeddedSourceFiles) {
                final MeganFile file = new MeganFile();
                file.setFileFromExistingFile(fileName, true);
                if (!file.isMeganSummaryFile() && !file.isUnsupportedRMA1File() && !file.isMeganServerFile())
                    filtered.add(fileName);
            }
            this.embeddedSourceFiles.addAll(filtered);
        }
    }
}
