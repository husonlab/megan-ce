/*
 * ShowAllAlignmentsCommand.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.dialogs.lrinspector.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.dialogs.lrinspector.LRInspectorViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * * show all arrows
 * * Daniel Huson, 4.2017
 */
public class ShowAllAlignmentsCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final LRInspectorViewer viewer = (LRInspectorViewer) getViewer();
        if (viewer.getController() != null) {
            viewer.showAllAlignments();
        }
    }

    public String getSyntax() {
        return "show alignments=all;";
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() instanceof LRInspectorViewer && ((LRInspectorViewer) getViewer()).hasHiddenAlignments();
    }

    public String getName() {
        return "Show All Alignments";
    }

    public String getDescription() {
        return "Show all alignments";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Empty16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
