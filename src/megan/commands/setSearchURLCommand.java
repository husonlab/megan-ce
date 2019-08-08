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
package megan.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class setSearchURLCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set searchURL={<URL>|default};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        // todo: fix so that number of reads can also be set for megan summary file
        np.matchIgnoreCase("set searchURL=");
        final String url = np.getWordFileNamePunctuation();
        np.matchRespectCase(";");
        if (url.equalsIgnoreCase("default"))
            ProgramProperties.put(ProgramProperties.SEARCH_URL, ProgramProperties.defaultSearchURL);
        else if (url.contains("%s"))
            ProgramProperties.put("SearchURL", url);
        else
            System.err.println("Set URL failed: must contain %s as placeholder for query");
    }

    public void actionPerformed(ActionEvent event) {
        String searchURL = ProgramProperties.get(ProgramProperties.SEARCH_URL, ProgramProperties.defaultSearchURL);
        searchURL = JOptionPane.showInputDialog(getViewer().getFrame(), "Set search URL (use %s for search term):", searchURL);
        if (searchURL != null) {
            if (searchURL.contains("%s") || searchURL.equalsIgnoreCase("default"))
                execute("set searchURL='" + searchURL + "';");
        } else
            NotificationsInSwing.showError(getViewer().getFrame(), "Search URL must contain %s as placeholder for query");
    }


    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Set Search URL...";
    }

    public String getDescription() {
        return "Set the URL used for searching the web";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }
}

