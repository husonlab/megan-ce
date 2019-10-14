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
package megan.timeseriesviewer;

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
        menuConfig.defineMenuBar("File;Edit;Options;Window;Help;");

        menuConfig.defineMenu("File", "New...;|;Open...;@Open Recent;|;Open From Server...;|;Compare...;|;Import From BLAST...;@Import;Meganize DAA File...;|;Close;|;Quit;");
        menuConfig.defineMenu("Open Recent", ";");
        menuConfig.defineMenu("Import", "Import Text (CSV) Format...;Import BIOM Format...;|;Import Metadata...;");

        menuConfig.defineMenu("Edit", "Cut;Copy;Paste;|;Select All;Select None;|;Find...;Find Again;|;");

        menuConfig.defineMenu("Options", "Compare Samples...;Compare Subjects...;Compare Time Points...;");

        menuConfig.defineMenu("Window", "Close All Other Windows...;|;Reset Window Location;Set Window Size...;|;Message Window...;|;" +
                "Inspector Window...;|;Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Sample Viewers...;|;" +
                ChartCommandHelper.getOpenChartMenuString() + "|;Chart Microbial Attributes...;|;");

        menuConfig.defineMenu("Help", "About...;How to Cite...;|;Community Website...;Reference Manual...;|;Check For Updates...;");
        return menuConfig;
    }

    /**
     * gets the tool bar configuration
     *
     * @return configuration
     */
    public static String getToolBarConfiguration() {
        return "Open...;|;Find...;|;Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Sample Viewers...;";
    }

    /**
     * gets the bottom tool bar configuration
     *
     * @return configuration
     */
    public static String getBottomToolBarConfiguration() {
        return "Compare Subjects...;Compare Time Points...;Compare Samples...;";
    }


    /**
     * gets the row header configuration
     *
     * @return configuration
     */
    public static String getSubjectPopupConfiguration() {
        return "";
    }

    /**
     * gets the column header configuration
     *
     * @return configuration
     */
    public static String getSubjectColumnHeaderPopupConfiguration() {
        return "";
    }

    /**
     * gets the row header configuration
     *
     * @return configuration
     */
    public static String getDataPopupConfiguration() {
        return "Select All;Select None;|;Compare Samples...;Compare Subjects...;Compare Time Points...;";
    }

    /**
     * gets the column header configuration
     *
     * @return configuration
     */
    public static String getDataColumnHeaderPopupConfiguration() {
        return "";
    }


}
