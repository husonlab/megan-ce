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
package megan.remote;

import jloda.swing.window.MenuConfiguration;

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

        menuConfig.defineMenu("File", "New...;|;Open Selected Files;@Open Recent;|;Open From URL...;|;Compare...;|;Compare Selected Files;|;Import From BLAST...;@Import;Meganize DAA File...;|;Close;|;Quit;");
        menuConfig.defineMenu("Open Recent", ";");

        menuConfig.defineMenu("Edit", "Cut;Copy;Paste;|;Select All;Select None;|;Find...;Find Again;");
        menuConfig.defineMenu("Options", "Open Server...;Close Remote Server...;");
        menuConfig.defineMenu("Layout", "Expand Remote Browser Nodes;Collapse Remote Browser Nodes;");

        menuConfig.defineMenu("Window", "Close All Other Windows...;|;Reset Window Location;Set Window Size...;|;Message Window...;|;");

        menuConfig.defineMenu("Help", "About...;How to Cite...;|;Community Website...;Reference Manual...;|;Check For Updates...;");

        return menuConfig;
    }


    /**
     * gets the toolbar configuration
     *
     * @return toolbar configuration
     */
    public static String getToolBarConfiguration() {
        return "Open Selected Files;|;Find...;|;Expand Remote Browser Nodes;Collapse Remote Browser Nodes;|;Server Info...;Close Remote Server...;";
    }


}
