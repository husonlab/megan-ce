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

import jloda.util.Basic;
import jloda.util.ProgressListener;
import jloda.util.ProgressSilent;

import java.io.IOException;
import java.util.*;

/**
 * manages classification data
 * Daniel Huson, 4.2015
 */
public class ClassificationManager {
    private static final Set<String> allSupportedClassifications = new TreeSet<>();
    private static final Set<String> allSupportedClassificationsExcludingNCBITaxonomy = new TreeSet<>();

    private static final Map<String, Classification> name2classification = new TreeMap<>();

    private static final ArrayList<String> defaultClassificationsList = new ArrayList<>();
    private static final ArrayList<String> defaultClassificationsListExcludingNCBITaxonomy = new ArrayList<>();

    private static String meganMapDBFile;
    private static boolean useFastAccessionMappingMode;

    static {
        defaultClassificationsListExcludingNCBITaxonomy.add("INTERPRO2GO");
        defaultClassificationsListExcludingNCBITaxonomy.add("EGGNOG");
        defaultClassificationsListExcludingNCBITaxonomy.add("SEED");
        defaultClassificationsListExcludingNCBITaxonomy.add("KEGG");
        defaultClassificationsListExcludingNCBITaxonomy.add("VFDB");
        allSupportedClassificationsExcludingNCBITaxonomy.addAll(defaultClassificationsListExcludingNCBITaxonomy);

        defaultClassificationsList.addAll(defaultClassificationsListExcludingNCBITaxonomy);
        defaultClassificationsList.add(Classification.Taxonomy);
        allSupportedClassifications.addAll(defaultClassificationsList);
    }

    /**
     * gets the named classification, loading  the tree and mapping, if necessary
     * There is one static classification object per name
     *
     * @param name
     * @param load - create standard file names and load the files
     * @return classification
     */
    public static Classification get(String name, boolean load) {
        Classification classification = name2classification.get(name);
        if (classification == null) {
            synchronized (name2classification) {
                classification = name2classification.get(name);
                if (classification == null) {
                    if (load) {
                        final String treeFile = name.equals(Classification.Taxonomy) ? "ncbi.tre" : name.toLowerCase() + ".tre";
                        final String mapFile = name.equals(Classification.Taxonomy) ? "ncbi.map" : name.toLowerCase() + ".map";
                        classification = load(name, treeFile, mapFile, new ProgressSilent());
                    } else {
                        classification = new Classification(name);
                    }
                    name2classification.put(name, classification);
                }
            }
        }
        return classification;
    }

    /**
     * loads the named files and setups up the given classification (if not already present)
     *
     * @param name
     * @param treeFile
     * @param mapFile
     * @return classification
     */
    public static Classification load(String name, String treeFile, String mapFile, ProgressListener progress) {
        synchronized (name2classification) {
            Classification classification = name2classification.get(name);
            if (classification == null) {
                classification = new Classification(name);
                name2classification.put(name, classification);
            }
            classification.load(treeFile, mapFile, progress);
            return classification;
        }
    }

    /**
     * ensure that the tree and mapping for the named classification are loaded
     *
     * @param name
     */
    public static void ensureTreeIsLoaded(String name) {
        get(name, true);
    }

    public static Set<String> getAllSupportedClassifications() {
        return allSupportedClassifications;
    }

    public static Set<String> getAllSupportedClassificationsExcludingNCBITaxonomy() {
        return allSupportedClassificationsExcludingNCBITaxonomy;
    }

    public static ArrayList<String> getDefaultClassificationsList() {
        return defaultClassificationsList;
    }

    public static ArrayList<String> getDefaultClassificationsListExcludingNCBITaxonomy() {
        return defaultClassificationsListExcludingNCBITaxonomy;
    }

    public static String getIconFileName(String classificationName) {
        return Basic.capitalizeFirstLetter(classificationName.toLowerCase()) + "Viewer16.gif";
    }

    public static boolean isActiveMapper(String name, IdMapper.MapType mapType) {
        return name2classification.get(name) != null && get(name, true).getIdMapper().isActiveMap(mapType);
    }

    public static void setActiveMapper(String name, IdMapper.MapType mapType, boolean active) {
            if (active || name2classification.get(name) != null)
                get(name, true).getIdMapper().setActiveMap(mapType, active);
    }

    public static boolean hasTaxonomicRanks(String classificationName) {
        return name2classification.get(classificationName).getId2Rank().size() > 1;
    }

    /**
     * is the named parsing method loaded
     *
     * @param mapType
     * @return true, if loaded
     */
    public static boolean isLoaded(String name, IdMapper.MapType mapType) {
        return name2classification.get(name) != null && get(name, true).getIdMapper().isLoaded(mapType);
    }


    public static String getMapFileKey(String name, IdMapper.MapType mapType) {
        return name + mapType.toString() + "FileLocation";
    }

    public static String getWindowGeometryKey(String name) {
        return name + "WindowGeometry";
    }

    public static boolean isTaxonomy(String name) {
        return hasTaxonomicRanks(name); // todo: need to enforce that all labels are unique
    }

    public static String getMeganMapDBFile() {
        return meganMapDBFile;
    }

    public static void setMeganMapDBFile(String meganMapDBFile) throws IOException {
        if(meganMapDBFile!=null && !Basic.fileExistsAndIsNonEmpty(meganMapDBFile))
            throw new IOException("File not found or not readable: "+meganMapDBFile);
        ClassificationManager.meganMapDBFile = meganMapDBFile;
        if(meganMapDBFile!=null)
            setUseFastAccessionMappingMode(true);
    }

    public static boolean canUseMeganMapDBFile() {
        return getMeganMapDBFile()!=null && isUseFastAccessionMappingMode();
    }
    public static boolean isUseFastAccessionMappingMode() {
        return useFastAccessionMappingMode;
    }

    public static void setUseFastAccessionMappingMode(boolean useFastAccessionMappingMode) {
        ClassificationManager.useFastAccessionMappingMode = useFastAccessionMappingMode;
    }
}
