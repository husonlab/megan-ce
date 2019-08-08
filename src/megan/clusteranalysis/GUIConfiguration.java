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
package megan.clusteranalysis;

import jloda.swing.window.MenuConfiguration;
import megan.classification.data.ClassificationCommandHelper;

/**
 * configuration for menu and toolbar
 * Daniel Huson, 7.2010
 */
public class GUIConfiguration {

    /**
     * get the menu configuration
     *
     * @return menu configuration
     */
    public static MenuConfiguration getMenuConfiguration() {
        MenuConfiguration menuConfig = new MenuConfiguration();
        menuConfig.defineMenuBar("File;Edit;Select;Layout;Options;PCoA;Window;Help;");

        menuConfig.defineMenu("File", "New...;|;Open...;@Open Recent;|;Open From Server...;|;Compare...;|;Import From BLAST...;@Import;Meganize DAA File...;|;Save As...;|;"
                + "Export Image...;Export Legend...;Export Distances...;|;Page Setup...;Print...;|;Close;|;Quit;");
        menuConfig.defineMenu("Open Recent", ";");

        menuConfig.defineMenu("Import", "Import Text (CSV) Format...;Import BIOM Format...;|;Import Metadata...;");

        menuConfig.defineMenu("Edit", "Copy;Copy Image;Copy Legend;Paste;|;Format...;Set Node Shape...;Set Color...;|;Set Axes Linewidth and Color...;Set BiPlot Linewidth and Color...;Set TriPlot Linewidth and Color...;Set Groups Linewidth and Color...;|;Group Nodes;Ungroup All;|;Find...;Find Again;|;Colors...;");

        menuConfig.defineMenu("Node Shape", "Circle;Square;Triangle;Diamond;");

        menuConfig.defineMenu("Select", "Select All;Select None;Invert Selection;|;From Previous Window;");
        menuConfig.defineMenu("Layout", "Show Legend;|;Increase Font Size;Decrease Font Size;|;@Expand/Contract;|;Zoom to Fit;Set Scale...;|;Flip Horizontally;Flip Vertically;@Rotate;|;" +
                "Show Groups;Show Groups As Convex Hulls;|;Use Colors;Show Labels;Set Node Radius...;");

        menuConfig.defineMenu("Rotate", "Rotate Left;Rotate Right;Rotate Up;Rotate Down;");
        menuConfig.defineMenu("Expand/Contract", "Expand Horizontal;Contract Horizontal;Expand Vertical;Contract Vertical;");

        menuConfig.defineMenu("Options", "Apply Triangulation Test...;|;PCoA Tab;UPGMA Tree Tab;NJ Tree Tab;Network Tab;Matrix Tab;|;Use JSD;Use Bray-Curtis;Use Euclidean;|;Use Unweighted Uniform UniFrac;Use Weighted Uniform UniFrac;|;Use Chi-Square;Use Kulczynski;Use Hellinger;Use Goodall;|;Sync;");


        menuConfig.defineMenu("PCoA", "PCoA Tab;|;PC1 vs PC2;PC1 vs PC3;PC2 vs PC3;PCi vs PCj...;|;PC1 PC2 PC3;PCi PCj PCk...;|;Show Axes;|;Show BiPlot;BiPlot Size...;|;Show TriPlot;TriPlot Size...;");

        menuConfig.defineMenu("Window", "Message Window...;|;Samples Viewer...;Groups Viewer...;|;");

        menuConfig.defineMenu("Help", "About...;How to Cite...;|;Community Website...;Reference Manual...;|;Check For Updates...;");
        return menuConfig;
    }

    /**
     * gets the toolbar configuration
     *
     * @return toolbar configuration
     */
    public static String getToolBarConfiguration() {
        return "Open...;Print...;Export Image...;|;Find...;|;Expand Vertical;Contract Vertical;Expand Horizontal;Contract Horizontal;|;Zoom to Fit;|;"
                + "PC1 vs PC2;PC1 vs PC3;PC2 vs PC3;PCi vs PCj...;|;PC1 PC2 PC3;PCi PCj PCk...;|;" +
                "Color Samples By Attribute;Shape Samples By Attribute;Label Samples By Attribute;Group Samples By Attribute;Ungroup All;|;"
                + "Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Samples Viewer...;|;Show Legend;|;Sync;";

    }

    /**
     * get the configuration of the node popup menu
     *
     * @return config
     */
    public static String getNodePopupConfiguration() {
        return "Correlate To Attributes...;|;Copy Node Label;Edit Node Label;|;Set Node Shape...;Set Color...;";
    }

    /**
     * get the configuration of the edge popup menu
     *
     * @return config
     */
    public static String getEdgePopupConfiguration() {
        return null;
    }

    /**
     * gets the canvas popup configuration
     *
     * @return config
     */
    public static String getPanelPopupConfiguration() {
        return "Copy Image;Export Image...;";
    }
}
