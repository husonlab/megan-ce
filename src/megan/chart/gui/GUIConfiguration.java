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
package megan.chart.gui;

import jloda.swing.window.MenuConfiguration;
import megan.chart.data.ChartCommandHelper;
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
        menuConfig.defineMenuBar("File;Edit;Options;Layout;Chart;Window;Help;");

        menuConfig.defineMenu("File", "New...;|;Open...;@Open Recent;|;Open From Server...;|;Compare...;|;Import From BLAST...;@Import;Meganize DAA File...;|;Save As...;|;" +
                "Export Image...;Export Legend...;Export Data...;|;Page Setup...;Print...;|;Close;|;Quit;");
        menuConfig.defineMenu("Open Recent", ";");
        menuConfig.defineMenu("Import", "Import Text (CSV) Format...;Import BIOM Format...;|;Import Metadata...;");

        menuConfig.defineMenu("Edit", "Cut;Copy Label;Copy Image;Copy Legend;Paste;|;Select All;Select None;Select Top...;|;From Previous Window;|;Show All;Show Selected;|;" +
                "Hide Unselected;Hide Selected;|;Set Color...;|;Find...;Find Again;|;Colors...;");

        menuConfig.defineMenu("Options", "Set Title...;Set Series Label...;Set Classes Label...;Set Counts Label...;|;" +
                "Linear Scale;Sqrt Scale;Log Scale;Percentage Scale;Z-Score Scale;|;Use Percent of Total;|;Cluster Series;Cluster Classes;Cluster Attributes;|;Grid Lines;Gaps Between Bars;");

        menuConfig.defineMenu("Layout", "@Font;|;Show Legend;Show Values;|;Show x-Axis;Show y-Axis;|;Use Jitter;Rectangle Shape;Show Internal Labels;" +
                "Set Max Radius...;|;Labels Standard;Labels Up 45o;Labels Down 45o;Labels Up 90o;Labels Down 90o;|;" +
                "Expand Horizontal;Contract Horizontal;Expand Vertical;Contract Vertical;Zoom To Fit;|;Rotate Left;Rotate Right;|;Transpose;");

        menuConfig.defineMenu("Font", "Title Font...;X-Axis Font...;Y-Axis Font...;Legend Font...;Values Font...;|;Draw Font...;");

        menuConfig.defineMenu("Chart", ChartCommandHelper.getSetDrawerCommandString() + "|;Sync;");

        menuConfig.defineMenu("Window", "Close All Other Windows...;|;Reset Window Location;Set Window Size...;|;Message Window...;|;" +
                "Inspector Window...;|;Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Samples Viewer...;|;");

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
                "Expand Vertical;Contract Vertical;Expand Horizontal;Contract Horizontal;|;Zoom To Fit;|;Transpose;Colors...;|;" +
                ChartCommandHelper.getSetDrawerCommandString() + "|;" + "Linear Scale;Sqrt Scale;Log Scale;Percentage Scale;Z-Score Scale;|;" +
                "Labels Standard;Labels Up 45o;Labels Down 45o;Labels Up 90o;Labels Down 90o;|;" +
                "Main Viewer...;" + ClassificationCommandHelper.getOpenViewerMenuString() + "|;Samples Viewer...;Show Legend;|;";
    }

    /**
     * gets the toolbar configuration
     *
     * @return toolbar configuration
     */
    public static String getToolBar4DomainListConfiguration() {
        return "Sort Alphabetically;Sort Alphabetically Backward;Sort By Values (Down);Sort By Values (Up);Group Enabled Entries;Cluster;";
    }

    public static String getMainPanelPopupConfiguration() {
        return "Zoom To Fit;|;Copy Image;Export Image...;";
    }

    public static String getLegendPanelPopupConfiguration() {
        return "Copy Legend;Export Legend...;";
    }


    public static String getSeriesListPopupConfiguration() {
        return "Copy Label;|;Select All;Select None;|;Show Selected;Hide Selected;|;Show All;Hide All;|;Set Color...;";
    }

    public static String getClassesListPopupConfiguration() {
        return "Copy Label;|;Select All;Select None;|;Show Selected;Hide Selected;|;Show All;Hide All;|;Set Color...;";
    }

    public static String getAttributesListPopupConfiguration() {
        return "Copy Label;|;Select All;Select None;|;Show Selected;Hide Selected;|;Show All;Hide All;|;Set Color...;";
    }
}
