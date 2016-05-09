/*
 *  Copyright (C) 2015 Daniel H. Huson
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

package megan.classification;

import jloda.util.*;
import megan.io.String2IntegerDiskBasedHashTable;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * maps accession strings to ids
 * Daniel Huson, 3.2016
 */
public class Accession2IdMap implements Closeable {
    private String[] tags = new String[0];
    private boolean attemptFirstWord = false;

    private final String2IntegerDiskBasedHashTable table;

    private final Map<String, Integer> map;

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     * @throws CanceledException
     */
    public Accession2IdMap(String fileName) throws IOException, CanceledException {
        this(fileName, new ProgressPercentage());
    }

    /**
     * constructor. If fileName ends on .abin, assumes is disk-based hash table, else assumes is text file containing tab-separated accession id pairs
     *
     * @param fileName
     * @throws IOException
     */
    public Accession2IdMap(String fileName, ProgressListener progress) throws IOException, CanceledException {
        if (fileName.endsWith(".abin")) {
            table = new String2IntegerDiskBasedHashTable(fileName);
            System.err.println(String.format("Size=%,12d", table.size()));
            map = null;
        } else {
            table = null;
            map = new HashMap<>();
            try (FileInputIterator it = new FileInputIterator(fileName)) {
                progress.setSubtask("Loading file: " + fileName);
                progress.setMaximum(it.getMaximumProgress());
                progress.setProgress(it.getProgress());
                while (it.hasNext()) {
                    String[] tokens = Basic.split(it.next(), '\t');
                    if (tokens.length == 2)
                        map.put(tokens[0], Integer.parseInt(tokens[1]));
                    progress.setProgress(it.getProgress());
                }
            }
            if (progress instanceof ProgressPercentage)
                ((ProgressPercentage) progress).reportTaskCompleted();
        }
    }

    @Override
    public void close() throws IOException {
        if (table != null)
            table.close();
        if (map != null)
            map.clear();
    }

    public int get(String accession) throws IOException {
        if (table != null)
            return table.get(accession);
        else if (map != null) {
            Integer result = map.get(accession);
            if (result != null)
                return result;
        }
        return 0;
    }

    /**
     * parses a line and returns the id
     *
     * @param aLine
     * @return id or 0
     * @throws IOException
     */
    public int parseHeaderLine(String aLine) throws IOException {
        if (attemptFirstWord) {
            int a = 0;
            while (a < aLine.length()) {
                if (aLine.charAt(a) == '>' || Character.isWhitespace(aLine.charAt(a)))
                    a++;
                else
                    break;
            }
            int b = a + 1;
            while (b < aLine.length()) {
                if (Character.isLetterOrDigit(aLine.charAt(b)) || aLine.charAt(b) == '_')
                    b++;
                else
                    break;
            }
            if (b - a > 3) {
                int value = table != null ? table.get(aLine.substring(a, b)) : map.get(aLine.substring(a, b));
                if (value != 0)
                    return value;
            }
        }
        if (tags.length > 0) {
            for (String tag : tags) {
                int b;
                for (int a = aLine.indexOf(tag); a != -1; a = aLine.indexOf(tag, b + 1)) {
                    a += tag.length();
                    b = a + 1;
                    while (b < aLine.length()) {
                        if (Character.isLetterOrDigit(aLine.charAt(b)) || aLine.charAt(b) == '_')
                            b++;
                        else
                            break;
                    }
                    int value = table != null ? table.get(aLine.substring(a, b)) : map.get(aLine.substring(a, b));
                    if (value != 0)
                        return value;
                }
            }
        }
        return 0;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public boolean isAttemptFirstWord() {
        return attemptFirstWord;
    }

    public void setAttemptFirstWord(boolean attemptFirstWord) {
        this.attemptFirstWord = attemptFirstWord;
    }
}
