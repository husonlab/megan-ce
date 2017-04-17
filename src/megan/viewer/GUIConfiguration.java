/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.gui.MenuConfiguration;
import jloda.util.ProgramProperties;
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
        menuConfig.defineMenuBar("File;Edit;Select;Options;Layout;Tree;Window;Help;");

        menuConfig.defineMenu("File", "New...;|;Open...;@Open Recent;|;Open From Server...;|;Compare...;|;Import From BLAST...;@Import;Meganize DAA File...;|;Save As...;|;"
                + "Export Image...;Export Legend...;@Export;|;Page Setup...;Print...;|;Extract To New Document...;Extract Reads...;|;Properties...;|;Close;|;Quit;");

        menuConfig.defineMenu("Server", "Set Server Credentials...;;|;Add User...;Add Metadata...;");
        menuConfig.defineMenu("Open Recent", ";");
        menuConfig.defineMenu("Export", "CSV Format...;BIOM1 Format...;STAMP Format...;|;Metadata...;|;Tree...;|;Export Reads in GFF Format...;Export Read Lengths and Coverage...;|;Reads...;Matches...;Alignments...;Overlap Graph...;Gene-Centric Assembly...;|;MEGAN Summary File...;");
        menuConfig.defineMenu("Import", "Import CSV Format...;Import BIOM1 Format...;|;Import Metadata...;");

        menuConfig.defineMenu("Edit", "Cut;Copy;Copy Image;Copy Legend;Paste;|;Edit Node Label;Edit Edge Label;Description...;|;Format...;|;Find...;Find Again;|;Colors...;|;@Preferences;");

        menuConfig.defineMenu("Preferences", "Show Notifications;|;@Accession Parsing;@Fix Taxon Mapping;@Taxon Disabling;|;Use Alternative Taxonomy...;Use Default NCBI Taxonomy;|;Set Search URL...;");

        menuConfig.defineMenu("Accession Parsing", "First Word Is Accession;Set Accession Tags;");
        menuConfig.defineMenu("Taxon Disabling", "List Disabled...;|;Disable...;Enable...;|;Enable All;");

        menuConfig.defineMenu("Select", "All Nodes;None;|;From Previous Window;|;All Leaves;All Internal Nodes;Subtree;Leaves Below;Nodes Above;|;Invert;|;Select By Rank;|;Scroll To Selected;");

        menuConfig.defineMenu("Layout", "Show Legend;|;Increase Font Size;Decrease Font Size;|;@Expand/Contract;|;Layout Labels;|;Scale Nodes By Assigned;Scale Nodes By Summarized;"
                + "Set Max Node Height...;|;Zoom To Selection;|;Fully Contract;Fully Expand;|;"
                + "Draw Circles;Draw Pies;Draw Coxcombs;Draw Bars;Draw Heatmaps;|;Linear Scale;Sqrt Scale;Log Scale;|;Rounded Cladogram;Cladogram;Rounded Phylogram;Phylogram;|;Use Magnifier;|;Draw Leaves Only;");

        menuConfig.defineMenu("Expand/Contract", "Expand Horizontal;Contract Horizontal;Expand Vertical;Contract Vertical;");

        menuConfig.defineMenu("Options", "Change LCA Parameters...;Set Number Of Reads...;|;Project Assignments To Rank...;Use Read Weights for Assignments;|;List Summary...;List Paths...;|;" +
                "Compute Core Biome...;|;Shannon-Weaver Index...;Simpson-Reciprocal Index...;|;Open NCBI Web Page...;Inspect...;");

        menuConfig.defineMenu("Tree", "Collapse;Collapse To Top;Collapse All Others;Collapse at Level...;Rank...;|;" +
                "Uncollapse;Uncollapse Subtree;Uncollapse All;|;Show Names;Show IDs;Show Number of Reads Assigned;" +
                "Show Number of Reads Summarized;|;Node Labels On;Node Labels Off;|;Show Intermediate Labels;");

        menuConfig.defineMenu("Window", "Close All Other Windows...;Show Info...;|;Reset Window Location;Set Window Size...;|;Message Window...;|;" +
                "Inspector Window...;Show Alignment...;|;Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Samples Viewer...;Groups Viewer...;" +
                "|;Chart...;|;Chart Microbial Attributes...;|;Cluster Analysis...;|;Rarefaction Analysis...;|;");

        menuConfig.defineMenu("Help", "About...;How to Cite...;|;Community Website...;Reference Manual...;" + ProgramProperties.getIfEnabled("usingInstall4j", "|;Check For Updates...;"));

        return menuConfig;
    }

    /**
     * gets the toolbar configuration
     *
     * @return toolbar configuration
     */
    public static String getToolBarConfiguration() {
        return "Open...;Print...;Export Image...;|;Find...;|;" +
                "Expand Vertical;Contract Vertical;Expand Horizontal;Contract Horizontal;|;Fully Contract;Fully Expand;Scroll To Selected;|;Rounded Cladogram;Cladogram;Rounded Phylogram;Phylogram;|;" +
                "Colors...;|;" +
                "Collapse;Collapse To Top;Uncollapse;Uncollapse Subtree;Rank...;|;Draw Circles;Draw Pies;Draw Coxcombs;Draw Bars;Draw Heatmaps;|;Linear Scale;Sqrt Scale;Log Scale;|;" +
                "Chart...;|;Inspect...;Show Alignment...;Extract Reads...;Rarefaction Analysis...;|;" +
                "Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Cluster Analysis...;Samples Viewer...;|;Show Legend;|;";
    }

    /**
     * get the configuration of the node popup menu
     *
     * @return config
     */
    public static String getNodePopupConfiguration() {
        return "Inspect...;Show Alignment...;Extract Reads...;Gene-Centric Assembly...;Correlate To Attributes...;|;Edit Node Label;Copy Node Label;|;Collapse;|;Uncollapse;Uncollapse Subtree;|;" +
                "Node Labels On;Node Labels Off;|;Open Web Page...;Web Search...;";
    }

    public static String getJTreePopupConfiguration() {
        return "Inspect...;Show Alignment...;Extract Reads...;Gene-Centric Assembly...;|;Copy Node Label;|;Open Web Page...;Web Search...;";
    }

    public static String getJTablePopupConfiguration() {
        return "Inspect...;Show Alignment...;Extract Reads...;Gene-Centric Assembly...;|;Copy Node Label;|;Open Web Page...;Web Search...;";
    }

    /**
     * get the configuration of the edge popup menu
     *
     * @return config
     */
    public static String getEdgePopupConfiguration() {
        return "Edit Edge Label;Copy Edge Label;";
    }


    /**
     * gets the canvas popup configuration
     *
     * @return config
     */
    public static String getPanelPopupConfiguration() {
        return "Fully Contract;Fully Expand;|;Rounded Cladogram;Cladogram;Rounded Phylogram;Phylogram;|;Copy Image;Export Image...;";
    }

    public static String getSeriesListPopupConfiguration() {
        return "";
    }
}
