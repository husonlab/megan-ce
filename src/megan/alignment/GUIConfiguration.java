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
package megan.alignment;

import jloda.swing.window.MenuConfiguration;
import megan.chart.data.ChartCommandHelper;
import megan.classification.data.ClassificationCommandHelper;

/**
 * configuration for menu and toolbar
 * Daniel Huson, 7.2010
 */
class GUIConfiguration {

    /**
     * get the menu configuration
     *
     * @return menu configuration
     */
    public static MenuConfiguration getMenuConfiguration() {
        MenuConfiguration menuConfig = new MenuConfiguration();
        menuConfig.defineMenuBar("File;Edit;Options;Layout;Window;Help;");

        menuConfig.defineMenu("File", "New...;|;Open...;@Open Recent;|;Open From Server...;|;Compare...;|;Import From BLAST...;|;Export Image...;Export Legend...;@Export;|;Page Setup...;Print...;|;Close;|;Quit;");
        menuConfig.defineMenu("Export", "Export Alignment...;Export Consensus...;Export Reference...;|;Overlap Graph...;Gene-Centric Assembly...;");

        menuConfig.defineMenu("Open Recent", ";");

        menuConfig.defineMenu("Edit", "Cut;Copy;Paste;|;Copy Alignment;Copy Consensus;Copy Reference;Copy Read Names;|;Select All;Select None;From Previous Alignment;|;Find...;Find Again;|;Find Read...;|;@Preferences;");

        menuConfig.defineMenu("Preferences", "Set Minimum Number of Reads...;");

        menuConfig.defineMenu("Options", "Move Up;Move Down;|;Translate...;|;Chart Diversity...;"
                + "Show Insertions;Contract Gaps;|;" +
                "Show Nucleotides;Show Amino Acids;|;Show Reference;Show Consensus;Show Unaligned;|;Set Amino Acid Colors...;|;" +
                "Matches Vs Reference;Mismatches Vs Reference;|;Matches Vs Consensus;Mismatches Vs Consensus;");

        menuConfig.defineMenu("Layout", "As Mapping;By Start;By Name;By Contigs;Unsorted;|;@Expand/Contract;|;Zoom To Fit;|;Expand To Height;Reset Zoom;");

        menuConfig.defineMenu("Expand/Contract", "Expand Horizontal Alignment;Contract Horizontal Alignment;Expand Vertical Alignment;Contract Vertical Alignment;");

        menuConfig.defineMenu("Window", "Close All Other Windows...;|;Reset Window Location;Set Window Size...;|;Message Window...;|;" +
                "Inspector Window...;|;Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Samples Viewer...;|;" +
                ChartCommandHelper.getOpenChartMenuString() + "|;Chart Microbial Attributes...;|;");

        menuConfig.defineMenu("Help", "About...;How to Cite...;|;Community Website...;Reference Manual...;|;Check For Updates...;");
        return menuConfig;
    }

    /**
     * gets the toolbar configuration
     *
     * @return toolbar configuration
     */
    public static String getToolBarConfiguration() {
        return "Open...;Print...;Export Image...;|;Find...;|;" +
                "Expand Horizontal Alignment;Contract Horizontal Alignment;Expand Vertical Alignment;Contract Vertical Alignment;|;" +
                "Zoom To Fit;|;Expand To Height;Reset Zoom;|;Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Samples Viewer...;|;Chart Diversity...;|;" +
                "Contract Gaps;Show Reference;Show Consensus;Show Unaligned;|;" +
                "Show Nucleotides;Show Amino Acids;|;By Start;As Mapping;";
    }


}
