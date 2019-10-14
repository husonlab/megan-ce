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
package megan.viewer;

import java.util.*;

/**
 * defines names and codes for taxonomic levels
 * Daniel Huson, 6.2007
 */
public class TaxonomicLevels {
    private final Map<Integer, String> id2name;
    private final Map<String, Integer> name2id;
    private final List<String> names = new LinkedList<>();

    public static final String Domain = "Domain";
    public static final String Kingdom = "Kingdom";
    public static final String Phylum = "Phylum";
    public static final String Class = "Class";
    public static final String Order = "Order";
    public static final String Family = "Family";
    private static final String Varietas = "Varietas";
    public static final String Genus = "Genus";
    private static final String Species_group = "Species_group";
    public static final String Species = "Species";
    private static final String Subspecies = "Subspecies";

    private final BitSet majorRanks = new BitSet();

    private static TaxonomicLevels instance;

    /**
     * get instance
     *
     * @return instance
     */
    private static TaxonomicLevels getInstance() {
        if (instance == null)
            instance = new TaxonomicLevels();
        return instance;
    }

    /**
     * constructor
     */
    private TaxonomicLevels() {
        name2id = new HashMap<>();
        id2name = new HashMap<>();
        // add all defined levels here:
        // addLevel((byte)0       ,"No rank");
        addLevel(127, Domain);
        majorRanks.set(127);

        addLevel(1, Kingdom);
        //majorRanks.set(1);  // don't use this because it will cause kingdoms to filled in

        addLevel(2, Phylum);
        majorRanks.set(2);

        addLevel(3, Class);
        majorRanks.set(3);

        addLevel(4, Order);
        majorRanks.set(4);

        addLevel(5, Family);
        majorRanks.set(5);

        addLevel(90, Varietas);

        addLevel(98, Genus);
        majorRanks.set(98);

        addLevel(99, Species_group);

        addLevel(100, Species);
        majorRanks.set(100);

        addLevel(101, Subspecies);
    }

    /**
     * is this a major KPCOFGS rank?
     *
     * @param rank
     * @return true, if major
     */
    public static boolean isMajorRank(int rank) {
        return getInstance().majorRanks.get(rank);
    }

    /**
     * get the next major rank
     *
     * @param rank
     * @return next major rank
     */
    public static int getNextRank(int rank) {
        if (rank == 100)
            return 0;
        if (rank == 127)
            return 1;
        int nextRank = getInstance().majorRanks.nextSetBit(rank + 1);
        return Math.max(nextRank, 0)
                ;
    }

    /* used to set up table
     */

    private void addLevel(Integer level, String name) {
        name2id.put(name, level);
        id2name.put(level, name);
        names.add(name);
    }

    /**
     * given a level name, returns the id
     *
     * @param name
     * @return level id     or null
     */
    public static Integer getId(String name) {
        Integer value = TaxonomicLevels.getInstance().name2id.get(name);
        return value == null ? 0 : value;
    }

    /**
     * given a level id, returns its name
     *
     * @param id
     * @return name
     */
    public static String getName(int id) {
        return TaxonomicLevels.getInstance().id2name.get(id);
    }

    /**
     * get all names
     *
     * @return names
     */
    public static List<String> getAllNames() {
        return TaxonomicLevels.getInstance().names;
    }

    /**
     * get all names of major ranks
     *
     * @return names
     */
    public static List<String> getAllMajorRanks() {
        ArrayList<String> list = new ArrayList<>();
        for (String name : TaxonomicLevels.getInstance().names) {
            if (isMajorRank(getId(name)))
                list.add(name);
        }
        return list;
    }

    public static int getSpeciesId() {
        return getId(Species);
    }

    public static int getSubspeciesId() {
        return getId(Subspecies);
    }

    public static int getGenusId() {
        return getId(Genus);
    }
}
