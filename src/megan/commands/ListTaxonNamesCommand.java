/*
 *  Copyright (C) 2015 Daniel H. Huson
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

import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * list taxon names
 * Daniel Huson, 11.2017
 */
public class ListTaxonNamesCommand extends CommandBase implements ICommand {
    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "list taxa=<taxon-id ...> [title=<string>];";

    }

    /**
     * parses and applies the command
     *
     * @param np
     * @throws Exception
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("list taxa=");
        final ArrayList<Integer> taxonIds = new ArrayList<>();
        String title = null;
        while (!np.peekMatchIgnoreCase(";")) {
            if (np.peekMatchIgnoreCase("title")) {
                np.matchIgnoreCase("title=");
                title = np.getWordFileNamePunctuation();
                break;
            } else {
                taxonIds.add(np.getInt(1, Integer.MAX_VALUE));
            }
        }
        np.matchIgnoreCase(";");

        if (title != null)
            System.out.println(title + "(" + taxonIds.size() + "):");
        for (Integer taxId : taxonIds) {
            System.out.println("[" + taxId + "] " + TaxonomyData.getName2IdMap().get(taxId));
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
    }

    /**
     * /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return null;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Lists the names of taxa";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }
}
