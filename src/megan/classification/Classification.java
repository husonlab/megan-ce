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

import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.ProgressListener;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.Name2IdMap;
import megan.core.Document;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.util.Map;

/**
 * data structures needed for parsing and representing a classification
 * Daniel Huson, 4.2015
 */
public class Classification {
    public static final String Taxonomy = "Taxonomy";

    private final String cName;
    private final ClassificationFullTree fullTree;
    private final Name2IdMap name2IdMap;

    private final IdMapper idMapper;

    /**
     * constructor
     *
     * @param cName
     */
    public Classification(String cName) {
        this.cName = cName;

        name2IdMap = new Name2IdMap();
        fullTree = new ClassificationFullTree(cName, name2IdMap);
        fullTree.setRoot(fullTree.newNode(-1));
        this.idMapper = new IdMapper(cName, fullTree, name2IdMap);
        if (cName.equals(Taxonomy)) {
            TaxonomyData.setTaxonomyClassification(this);
        }

    }

    /**
     * load the data
     *
     * @param treeFile
     * @param mapFile
     * @param progress
     */
    public void load(String treeFile, String mapFile, ProgressListener progress) {
        progress.setCancelable(false);

        try {
            progress.setSubtask("Loading " + mapFile);
            progress.setMaximum(2);
            progress.setProgress(0);

            Document.loadVersionInfo(cName + " tree", Basic.replaceFileSuffix(treeFile, ".info"));

            name2IdMap.loadFromFile(mapFile);

            progress.setProgress(1);
            progress.setSubtask("Loading " + treeFile);

            if (cName.equals(Classification.Taxonomy)) {
                if (name2IdMap.get(3554) != null && name2IdMap.get(3554).equals("Beta"))
                    name2IdMap.put("Beta <vulgaris>", 3554); // this is the cause of many false positives
            }
            fullTree.loadFromFile(treeFile);
            progress.setProgress(2);
        } catch (Exception e) {
            Basic.caught(e);
            NotificationsInSwing.showError(MainViewer.getLastActiveFrame(), "Failed to open files: " + treeFile + " and " + mapFile + ": " + e.getMessage());
        } finally {
            progress.setCancelable(true);
        }
    }

    /**
     * get classification name
     *
     * @return name
     */
    public String getName() {
        return cName;
    }

    /**
     * get mapper
     *
     * @return
     */
    public IdMapper getIdMapper() {
        return idMapper;
    }

    public ClassificationFullTree getFullTree() {
        return fullTree;
    }

    public Name2IdMap getName2IdMap() {
        return name2IdMap;
    }

    public Map<Integer, Integer> getId2Rank() {
        return name2IdMap.getId2Rank();
    }

    public Map<Integer, String> getId2ToolTip() {
        return name2IdMap.getId2ToolTip();
    }

    /**
     * create short tag for writing header line
     *
     * @param cName
     * @return short tag
     */
    public static String createShortTag(String cName) {
        if (cName.equalsIgnoreCase(Taxonomy))
            return "tax|";
        else if (cName.equalsIgnoreCase("interpro2go"))
            return "IPR|";
        else if (cName.equalsIgnoreCase("eggnog"))
            return "cog|";
        else
            return cName.toLowerCase() + "|";
    }

}
