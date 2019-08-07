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

import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.data.IName2IdMap;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * maintains a mapping between  names and  ids
 * Daniel Huson, 4.2015
 */
public class Name2IdMap implements IName2IdMap {
    private final Map<String, Integer> name2id;
    private final Map<Integer, String> id2name;
    private final Map<Integer, String> id2toolTip;
    private final Map<Integer, Integer> id2rank;

    private final boolean allowUnderscoresInLookups;

    /**
     * constructor
     */
    public Name2IdMap() {
        this(100000);
    }

    /**
     * constructor
     */
    public Name2IdMap(int approximateSize) {
        name2id = new HashMap<>(approximateSize, 0.99f);
        id2name = new HashMap<>(approximateSize, 0.99f);
        id2toolTip = new HashMap<>(approximateSize, 0.99f);
        id2rank = new HashMap<>(approximateSize, 0.99f);

        allowUnderscoresInLookups = ProgramProperties.get("allow-underscores-in-lookup", true);
    }

    /**
     * get ids to names map
     *
     * @return id2names
     */
    public Map<Integer, String> getId2Name() {
        return id2name;
    }

    /**
     * get the id for a name
     *
     * @param name
     * @return id or 0
     */
    public int get(String name) {
        Integer result = name2id.get(name);
        if (result != null)
            return result;
        else if (allowUnderscoresInLookups) {
            result = name2id.get(name.replaceAll("_", " "));
            if (result != null)
                return result;
        }
        return 0;
    }

    /**
     * get name for id
     *
     * @param id
     * @return name or null
     */
    public String get(int id) {
        return id2name.get(id);
    }

    /**
     * remove an id from the map
     *
     * @param id
     */
    public void remove(int id) {
        String name = id2name.get(id);
        if (name != null)
            name2id.remove(name);
        id2name.remove(id);
    }

    /**
     * put a name and id
     *
     * @param name
     * @param id
     */
    public void put(String name, int id) {
        name2id.put(name, id);
        id2name.put(id, name);
    }

    /**
     * get the set of ids
     *
     * @return id set
     */
    public Collection<Integer> getIds() {
        return name2id.values();
    }

    /**
     * get the set of names
     *
     * @return names
     */
    public Collection<String> getNames() {
        return name2id.keySet();
    }

    /**
     * gets the size of the mapping
     *
     * @return size
     */
    public int size() {
        return name2id.size();
    }

    /**
     * load from file
     */
    public void loadFromFile(String fileName) throws IOException {
        System.err.print("Loading " + Basic.getFileNameWithoutPath(fileName) + ": ");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(ResourceManager.getFileAsStream(fileName)))) {
            String aLine;
            while ((aLine = r.readLine()) != null) {
                if (aLine.length() > 0 && !aLine.startsWith("#")) {
                    String[] tokens = Basic.split(aLine, '\t');
                    if (tokens.length >= 2) {
                        if (tokens[0].trim().length() == 0)
                            continue; // Silva has such lines...
                        int id = Integer.parseInt(tokens[0]);
                        String name = tokens[1];
                        name2id.put(name, id);
                        id2name.put(id, name);

                        boolean hasToolTip = tokens.length > 2 && tokens[tokens.length - 1].startsWith("\"");
                        int tokensLengthWithoutToolTip = (hasToolTip ? tokens.length - 1 : tokens.length);

                        Integer rank = null;
                        if (tokensLengthWithoutToolTip == 3 && Basic.isInteger(tokens[2])) { // just level
                            rank = Integer.parseInt(tokens[2]);
                        } else if (tokensLengthWithoutToolTip == 4) { // genome size, level
                            rank = Integer.parseInt(tokens[3]);
                        }
                        if (hasToolTip) {
                            String quotedToolTip = tokens[tokens.length - 1];
                            id2toolTip.put(id, quotedToolTip.substring(1, quotedToolTip.length() - 1));
                        }
                        if (rank != null)
                            id2rank.put(id, rank);
                    }
                }
            }
        } catch (NullPointerException ex) {
            throw new IOException("not found");
        }
        System.err.println(String.format("%,9d", id2name.size()));
    }

    /**
     * save mapping to file
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public void saveMappingToFile(String fileName) throws IOException {
        System.err.println("Writing name2id map to file: " + fileName);
        try (Writer w = new FileWriter(fileName)) {
            writeMapping(w);
        }
        System.err.println("Done (" + id2name.size() + " entries)");
    }

    /**
     * write the new mapping
     *
     * @param w
     */
    public void writeMapping(Writer w) throws IOException {
        w.write("# Mapping file, generated " + (new Date()) + "\n");
        for (Integer key : id2name.keySet()) {
            w.write(key + "\t" + id2name.get(key).replaceAll("\\s+"," ") + "\n");
        }
    }

    /**
     * get the rank of an item
     *
     * @param id
     * @return rank
     */
    public int getRank(int id) {
        Integer rank = id2rank.get(id);
        return rank == null ? 0 : rank;
    }

    /**
     * put the rank of an id
     *
     * @param id
     * @param rank
     */
    public void setRank(int id, int rank) {
        id2rank.put(id, rank);
    }

    public Map<Integer, Integer> getId2Rank() {
        return id2rank;
    }

    /**
     * gets the tooltip map
     *
     * @return tooltip map
     */
    public Map<Integer, String> getId2ToolTip() {
        return id2toolTip;
    }
}
