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
package megan.classification;

import jloda.util.*;
import megan.accessiondb.AccessAccessionAdapter;
import megan.classification.data.*;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * tracks mapping files for a named type of classification
 * <p>
 * Daniel Huson, 4.2015
 */
public class IdMapper {
    static public final int NOHITS_ID = -1;
    static public final String NOHITS_LABEL = "No hits";
    static public final int UNASSIGNED_ID = -2;
    static public final String UNASSIGNED_LABEL = "Not assigned";
    static public final int LOW_COMPLEXITY_ID = -3;
    static public final String LOW_COMPLEXITY_LABEL = "Low complexity";
    static public final int UNCLASSIFIED_ID = -4;
    static public final String UNCLASSIFIED_LABEL = "Unclassified";
    static public final int CONTAMINANTS_ID = -6; // -5 used by KEGG
    static public final String CONTAMINANTS_LABEL = "Contaminants";

    public static IString2IntegerMapFactory accessionMapFactory = new Accession2IdMapFactory();

    public enum MapType {Accession, Synonyms, MeganMapDB}

    private final String cName;

    private final EnumMap<MapType, String> map2Filename = new EnumMap<>(MapType.class);

    private final EnumSet<MapType> loadedMaps = EnumSet.noneOf(MapType.class);

    private final EnumSet<MapType> activeMaps = EnumSet.noneOf(MapType.class);

    final ClassificationFullTree fullTree;
    private final Name2IdMap name2IdMap;

    private boolean useTextParsing;

    private final Set<Integer> disabledIds = new HashSet<>();

    private IString2IntegerMap accessionMap = null;
    private String2IntegerMap synonymsMap = null;

    private final IdParser.Algorithm algorithm;

    /**
     * constructor
     *
     * @param name2IdMap
     */
    public IdMapper(String name, ClassificationFullTree fullTree, Name2IdMap name2IdMap) {
        this.cName = name;
        this.fullTree = fullTree;
        this.name2IdMap = name2IdMap;

        algorithm = (ProgramProperties.get(cName + "UseLCAToParse", name.equals(Classification.Taxonomy)) ? IdParser.Algorithm.LCA : IdParser.Algorithm.First_Hit);
    }

    /**
     * create tags for parsing header line
     *
     * @param cName
     * @return short tag
     */
    public static String[] createTags(String cName) {
        String shortTag = Classification.createShortTag(cName);
        String longTag = cName.toLowerCase() + "|";
        if (shortTag.equals(longTag))
            return new String[]{shortTag};
        else
            return new String[]{shortTag, longTag};
    }

    /**
     * load the named file of the given map type
     *
     * @param fileName
     * @param mapType
     * @param reload
     * @param progress
     * @throws CanceledException
     */
    public void loadMappingFile(String fileName, MapType mapType, boolean reload, ProgressListener progress) throws CanceledException {
        switch (mapType) {
            default:
            case Accession: {
                if (accessionMap == null || reload) {
                    if (accessionMap != null) {
                       closeAccessionMap();
                    }
                    try {
                        this.accessionMap = accessionMapFactory.create(name2IdMap, fileName, progress);
                        loadedMaps.add(mapType);
                        activeMaps.add(mapType);
                        map2Filename.put(mapType, fileName);
                    } catch (Exception e) {
                        if (e instanceof CanceledException)
                            throw (CanceledException) e;
                        Basic.caught(e);
                    }
                }
                break;
            }
            case Synonyms: {
                if (synonymsMap == null || reload) {
                    if (synonymsMap != null) {
                        try {
                            synonymsMap.close();
                        } catch (IOException e) {
                            Basic.caught(e);
                        }
                    }
                    final String2IntegerMap synonymsMap = new String2IntegerMap();
                    try {
                        synonymsMap.loadFile(name2IdMap, fileName, progress);
                        this.synonymsMap = synonymsMap;
                        loadedMaps.add(mapType);
                        activeMaps.add(mapType);
                        map2Filename.put(mapType, fileName);
                    } catch (Exception e) {
                        if (e instanceof CanceledException)
                            throw (CanceledException) e;
                        Basic.caught(e);
                    }
                }
            }
            case MeganMapDB: {
                if (accessionMap == null || reload) {
                    if (accessionMap != null) {
                        closeAccessionMap();
                    }
                    try {
                        this.accessionMap =new AccessAccessionAdapter(fileName,cName);
                        loadedMaps.add(mapType);
                        activeMaps.add(mapType);
                        map2Filename.put(mapType, fileName);
                    } catch (Exception e) {
                        Basic.caught(e);
                    }
                }
                break;
            }
        }
    }

    /**
     * is the named parsing method loaded
     *
     * @param mapType
     * @return true, if loaded
     */
    public boolean isLoaded(MapType mapType) {
        return loadedMaps.contains(mapType);
    }

    public boolean isActiveMap(MapType mapType) {
        return activeMaps.contains(mapType);
    }

    public void setActiveMap(MapType mapType, boolean state) {
        if (state)
            activeMaps.add(mapType);
        else
            activeMaps.remove(mapType);
    }

    public void setUseTextParsing(boolean useTextParsing) {
        this.useTextParsing = useTextParsing;
    }

    public boolean isUseTextParsing() {
        return useTextParsing;
    }

    /**
     * creates a new id parser for this mapper
     *
     * @return
     */
    public IdParser createIdParser() {
        // the follow code ensures that we use multiple accesses to the sqlite mapping database
        if(accessionMap instanceof AccessAccessionAdapter) {
            final IdMapper copy=new IdMapper(cName,fullTree,name2IdMap);
            copy.setActiveMap(MapType.MeganMapDB,true);
            try {
                copy.loadMappingFile(((AccessAccessionAdapter)accessionMap).getMappingDBFile(),MapType.MeganMapDB,false,new ProgressSilent());
            } catch (CanceledException e) {
                Basic.caught(e);
            }
            final IdParser idParser = new IdParser(copy);
            idParser.setAlgorithm(algorithm);
            return idParser;

        }
        else {
            final IdParser idParser = new IdParser(this);
            idParser.setAlgorithm(algorithm);
            return idParser;
        }
    }

    /**
     * get a  id from an accession
     *
     * @param accession
     * @return KO id or null
     */
    public Integer getIdFromAccession(String accession) throws IOException {
        if (isLoaded(MapType.Accession)) {
            return getAccessionMap().get(accession);
        }
        return null;
    }

    public String getMappingFile(MapType mapType) {
        return map2Filename.get(mapType);
    }

    public IString2IntegerMap getAccessionMap() {
        return accessionMap;
    }

    public String2IntegerMap getSynonymsMap() {
        return synonymsMap;
    }

    public boolean hasActiveAndLoaded() {
        return activeMaps.size() > 0 && loadedMaps.size() > 0;
    }

    public String getCName() {
        return cName;
    }

    public String[] getIdTags() {
        if (ProgramProperties.get(cName + "ParseIds", false)) {
            return ProgramProperties.get(cName + "Tags", createTags(cName)); // user can override tags using program property
        } else
            return new String[0];
    }

    public Name2IdMap getName2IdMap() {
        return name2IdMap;
    }

    public Set<Integer> getDisabledIds() {
        return disabledIds;
    }

    private void closeAccessionMap() {
        if (accessionMap != null) {
            try {
                accessionMap.close();
            } catch (IOException e) {
                Basic.caught(e);
            }
        }
        accessionMap=null;
    }

    public boolean isDisabled(int id) {
        return disabledIds.size() > 0 && disabledIds.contains(id);
    }
}
