/*
 * LocationManager.java Copyright (C) 2024 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.data;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * manages locations of read and match texts
 * Daniel Huson, 10.2010
 */
public class LocationManager {
    private final Vector<File> files = new Vector<>();
    private final Vector<Long> fileSizes = new Vector<>();
    private final Map<File, Integer> file2id = new HashMap<>();
    private final TextStoragePolicy textStoragePolicy;

    /**
     * constructor
     *
	 */
    public LocationManager(TextStoragePolicy textStoragePolicy) {
        this.textStoragePolicy = textStoragePolicy;
    }

    /**
     * constructor
     *
     * @param fileName          file to add
     */
    public LocationManager(TextStoragePolicy textStoragePolicy, String fileName) {
        this.textStoragePolicy = textStoragePolicy;
        if (fileName != null)
            addFile(new File(fileName));
    }

    /**
     * gets the id of the file, adds it if not already present
     *
     * @return id
     */
    public int getFileId(File file) {
        Integer id = file2id.get(file);
        if (id == null) {
            id = file2id.size();
            file2id.put(file, id);
            files.add(id, file);
            fileSizes.add(id, file.length());
            // System.err.println("Added file, id: " + id + ", file: " + files.get(id));
        }
        return id;
    }

    /**
     * add a file, if not already present
     *
	 */
    public void addFile(File file) {
        getFileId(file);
    }

    /**
     * add a file, if not already present and explicitly set its size
     *
	 */
    public void addFile(File file, Long fileSize) {
        int fileId = getFileId(file);
        if (fileSize != null)
            fileSizes.set(fileId, fileSize);
    }

    /**
     * get the file associated with the given id
     *
	 */
    public File getFile(int fileId) {
        return files.get(fileId);
    }

    /**
     * get all files
     *
     * @return files
     */
    public Vector<File> getFiles() {
        return files;
    }

    /**
     * get all file names
     *
     * @return file names
     */
    public String[] getFileNames() {
        String[] fileNames = new String[files.size()];
        for (int i = 0; i < files.size(); i++)
            fileNames[i] = files.get(i).getPath();
        return fileNames;
    }

    /**
     * gets all the file sizes
     *
     * @return file sizes
     */
    public Long[] getFileSizes() {
        Long[] array = new Long[files.size()];
        for (int i = 0; i < fileSizes.size(); i++)
            array[i] = fileSizes.get(i);
        return array;
    }

    /**
     * get the location type
     *
     * @return type
     */
    public TextStoragePolicy getTextStoragePolicy() {
        return textStoragePolicy;
    }

    /**
     * get as string
     *
     * @return string
     */
    public String toString() {
        StringWriter w = new StringWriter();
        w.write("Storage: " + TextStoragePolicy.getDescription(textStoragePolicy) + "\n");
        for (int i = 0; i < files.size(); i++) {
            w.write(" " + i + " -> " + files.get(i).getPath() + "\n");

        }
        return w.toString();
    }

    /**
     * get mapping of files to ids
     *
     * @return files to ids
     */
    public Map<File, Integer> getMapping() {
        return file2id;
    }
}
