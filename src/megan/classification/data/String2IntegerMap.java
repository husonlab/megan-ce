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
package megan.classification.data;

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.FileLineIterator;
import jloda.util.ProgressListener;
import megan.data.IName2IdMap;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * a loadable string to integer map. Integers may have a prefix
 * Daniel Huson, 7.2016
 */
public class String2IntegerMap extends HashMap<String, Integer> implements Closeable {

    /**
     * load a file of synonyms
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public void loadFile(IName2IdMap label2id, String fileName, ProgressListener progressListener) throws IOException, CanceledException {
        System.err.println("Loading map from file: " + fileName);

        try (FileLineIterator it = new FileLineIterator(fileName)) {
            it.setSkipCommentLines(true);
            it.setSkipEmptyLines(true);
            progressListener.setProgress(0);
            progressListener.setMaximum(it.getMaximumProgress());

            while (it.hasNext()) {
                String aLine = it.next();
                String[] tokens = aLine.split("\t");

                if (tokens.length >= 2) {
                    final Integer id;
                    if (Basic.isInteger(tokens[1])) {
                        id = Basic.parseInt(tokens[1]);
                    } else {
                        id = label2id.get(tokens[1]);
                    }
                    if (id != 0)
                        put(tokens[0], id);
                    else
                        System.err.println("Line " + it.getLineNumber() + ": invalid id: " + tokens[1]);
                } else {
                    throw new IOException("Loading synonyms-to-id file, line: " + it.getLineNumber() +
                            ": expected two entries separated by a tab,  got: <" + aLine + ">");
                }
                progressListener.setProgress(it.getProgress());
            }
        } finally {
            System.err.println("Lines loaded: " + size());
        }
    }

    /**
     * save to a file
     *
     * @param fileName
     * @throws IOException
     */
    public void save(String fileName) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName), 1000000)) {
            for (String key : keySet()) {
                Integer value = get(key);
                if (value != null)
                    w.write(key + "\t" + value + "\n");
            }
        }
    }

    /**
     * has the table been loaded
     *
     * @return
     */
    public boolean isLoaded() {
        return size() > 0;
    }

    @Override
    public void close() throws IOException {
        clear();
    }
}
