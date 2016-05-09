/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.util.*;
import megan.data.IName2IdMap;
import megan.viewer.TaxonomyData;

import java.io.*;
import java.util.HashMap;

/**
 * a loadable string to integer map. Integers may have a prefix
 * Daniel Huson, 12.2012
 */
public class LoadableString2IntegerMap extends HashMap<String, Integer> implements Closeable {

    /**
     * load a file of synonyms
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public void loadFile(IName2IdMap label2id, String fileName, boolean taxonomy, ProgressListener progressListener) throws IOException, CanceledException {
        System.err.println("Loading map from file: " + fileName);
        FileInputIterator it = new FileInputIterator(new InputStreamReader(ResourceManager.getFileAsStream(fileName)), fileName);
        it.setSkipCommentLines(true);
        it.setSkipEmptyLines(true);
        progressListener.setProgress(0);
        progressListener.setMaximum(it.getMaximumProgress());

        try {
            while (it.hasNext()) {
                String aLine = it.next();
                String[] tokens = aLine.split("\t");

                if (tokens.length >= 2) {
                    String label = tokens[0];
                    String token = Basic.skipToNumber(tokens[1]);
                    Integer id = null;

                    if (token != null) {
                        if (label2id != null) {
                            id = label2id.get(token);
                        }
                        if (id == null || id == 0) {
                            if (Basic.isInteger(token))
                                id = Integer.parseInt(token);
                            else if (taxonomy)
                                id = TaxonomyData.getName2IdMap().get(token);
                        }
                    }
                    if (id != null && id != 0)
                        put(label, id);
                    else
                        System.err.println("Line " + it.getLineNumber() + ": invalid id: " + tokens[1]);
                } else {
                    throw new IOException("Loading synonyms-to-id file, line: " + it.getLineNumber() +
                            ": expected two entries separated by a tab,  got: <" + aLine + ">");
                }
                progressListener.setProgress(it.getProgress());
            }
        } finally {
            it.close();
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
