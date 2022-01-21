/*
 * GeneItemCreator.java Copyright (C) 2022 Daniel H. Huson
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

package megan.genes;

import megan.accessiondb.AccessAccessionMappingDatabase;
import megan.classification.Classification;
import megan.classification.IdMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * class for creating gene items
 * Daniel Huson, 6.2018
 */
public class GeneItemCreator {
    private final String[] cNames;
    private final Map<String, Integer> rank;
    private final IdMapper[] idMappers;
    private final AccessAccessionMappingDatabase database;
    private final String[] shortTags;

    /**
     * constructor
     *
     * @param cNames
     */
    public GeneItemCreator(String[] cNames, IdMapper[] idMappers) {
        this.cNames = cNames.clone();
        this.idMappers = idMappers;
        this.database = null;

        rank = new HashMap<>();
        for (int i = 0; i < this.cNames.length; i++) {
            rank.put(this.cNames[i], i);
        }
        this.shortTags = new String[this.cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            shortTags[i] = Classification.createShortTag(cNames[i]);
        }
    }

    /**
     * constructor
     *
     * @param cNames
     */
    public GeneItemCreator(String[] cNames, AccessAccessionMappingDatabase database) {
        this.cNames = cNames.clone();
        this.database = database;

        this.idMappers = null;

        rank = new HashMap<>();
        for (int i = 0; i < this.cNames.length; i++) {
            rank.put(this.cNames[i], i);
        }
        this.shortTags = new String[this.cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            shortTags[i] = Classification.createShortTag(cNames[i]);
        }
    }

    public GeneItem createGeneItem() {
        return new GeneItem(this);
    }

    public Integer rank(String classificationName) {
        return rank.get(classificationName);
    }

    public String classification(int rank) {
        return cNames[rank];
    }

    public int numberOfClassifications() {
        return cNames.length;
    }

    public Iterable<String> cNames() {
        return () -> Arrays.asList(cNames).iterator();
    }

    /**
     * map the given accession to ids using the id mappers
     *
     * @param accession
     * @param ids
     * @throws IOException
     */
    public void map(String accession, int[] ids) throws IOException, SQLException {
        if (idMappers != null)
            for (int i = 0; i < idMappers.length; i++) {
                ids[i] = idMappers[i].getAccessionMap().get(accession);
            }
        else {
            for (int i = 0; i < cNames.length; i++) {
                ids[i] = database.getValue(cNames[i], accession);
            }
        }
    }

    public String getShortTag(int i) {
        return shortTags[i];
    }
}
