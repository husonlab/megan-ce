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
package megan.data;

import jloda.util.Pair;
import megan.io.InputReader;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * reads text from storage
 * Daniel Huson, 10.2010
 */
public class TextStorageReader {
    private final LocationManager locationManager;
    private final static int MAX_NUMBER_OF_OPEN_FILES = 5;
    private int numberOfOpenFiles = 0;

    private final InputReader[] fileId2raf;
    private final long[] fileId2lastUsed;

    private final static Set<String> warned = new HashSet<>();

    /**
     * constructor
     *
     * @param locationManager
     */
    public TextStorageReader(LocationManager locationManager) {
        this.locationManager = locationManager;
        int maxId = 0;
        for (File file : locationManager.getFiles())
            maxId = Math.max(maxId, locationManager.getFileId(file));
        fileId2raf = new InputReader[maxId + 1];
        fileId2lastUsed = new long[maxId + 1];
    }

    /**
     * close all files used for fetching text
     */
    public void closeAllFiles() {
        for (InputReader r : fileId2raf) {
            try {
                if (r != null)
                    r.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * gets the text stored at the named location
     *
     * @param location
     * @return text
     * @throws IOException
     */
    public String getText(Location location) throws IOException {
        int fileId = location.getFileId();
        if (fileId < 0 || fileId >= fileId2raf.length)
            return null;

        if (fileId2raf[fileId] == null) {
            if (numberOfOpenFiles >= MAX_NUMBER_OF_OPEN_FILES) {
                long min = Long.MAX_VALUE;
                int minId = -1;
                for (int i = 0; i < fileId2lastUsed.length; i++) {
                    if (fileId2raf[i] != null && fileId2lastUsed[i] < min) {
                        min = fileId2lastUsed[i];
                        minId = i;
                    }
                }
                if (minId != -1) {
                    fileId2raf[minId].close();
                    fileId2raf[minId] = null;
                    numberOfOpenFiles--;
                }
            }

            File file = locationManager.getFile(fileId);
            if (!(file.exists() && file.canRead()))
                file = new File(System.getProperty("user.dir") + File.separator + file.getPath());

            if (file.exists() && file.canRead()) {
                fileId2raf[fileId] = new InputReader(file, null, null, true);
                numberOfOpenFiles++;
            } else {
                if (!warned.contains(file.getPath())) {
                    System.err.println("Warning: Can't open file to read: " + file.getPath());
                    warned.add(file.getPath());
                }
                return null;
            }
        }
        fileId2lastUsed[fileId] = System.currentTimeMillis();
        InputReader raf = fileId2raf[fileId];

        if (location.getPosition() >= raf.length())
            throw new IOException("Location out of range: " + location.getPosition() + " >= " + raf.length());

        raf.seek(location.getPosition());
        if (locationManager.getTextStoragePolicy() == TextStoragePolicy.Reference) {
            byte[] bytes = new byte[location.getSize()];
            raf.read(bytes, 0, bytes.length);
            return new String(bytes, 0, bytes.length, "UTF-8");
        } else // must be dump file or on-board dump file
        {
            raf.seek(location.getPosition());
            return raf.readString();
        }
    }

    /**
     * gets the header and sequence for a read
     *
     * @param location
     * @return header and sequence
     * @throws IOException
     */
    public Pair<String, String> getHeaderAndSequence(Location location) throws IOException {
        Pair<String, String> headerAndSequence = new Pair<>();
        getHeaderAndSequence(location, headerAndSequence);
        return headerAndSequence;
    }

    /**
     * gets the header and sequence for a read
     *
     * @param location
     * @return header and sequence
     * @throws IOException
     */
    private void getHeaderAndSequence(Location location, Pair<String, String> headerAndSequence) throws IOException {
        String string = getText(location);

        if (string == null) {
            headerAndSequence.setFirst(">Unknown");
            headerAndSequence.setSecond("Unknown");
        } else {
            int eol = string.indexOf('\n'); // header is first line
            if (eol <= 0) {
                headerAndSequence.setFirst(string);
                headerAndSequence.setSecond(null);
            } else {
                headerAndSequence.setFirst(string.substring(0, eol));
                headerAndSequence.setSecond(string.substring(eol + 1));
            }
        }
    }
}
