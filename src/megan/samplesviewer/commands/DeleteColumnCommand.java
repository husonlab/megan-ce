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
package megan.samplesviewer.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.fx.Dialogs;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * * delete command
 * * Daniel Huson, 9.2015
 */
public class DeleteColumnCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "delete attribute=<name> [<name>...];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("delete attribute=");

        Set<String> attributes = new HashSet<>();
        while (!np.peekMatchIgnoreCase(";")) {
            String attribute = np.getWordRespectCase();
            attributes.add(attribute);
        }
        np.matchIgnoreCase(";");

        if (attributes.size() > 0) {
            final SamplesViewer viewer = ((SamplesViewer) getViewer());
            viewer.getSamplesTableView().deleteColumns(attributes.toArray(new String[0]));
        }
    }

    public void actionPerformed(ActionEvent event) {
        final SamplesViewer viewer = ((SamplesViewer) getViewer());
        final Collection<String> attributes = viewer.getSamplesTableView().getSelectedAttributes();

        if (attributes.size() > 0) {
            final String message = "Confirm delete column '" + attributes.iterator().next() + "'" + (attributes.size() > 1 ? " (and " + (attributes.size() - 1) + " others)" : "");

            if (Dialogs.showConfirmation(getViewer().getFrame(), "Confirm delete", message)) {
                final StringBuilder buf = new StringBuilder();
                buf.append("delete attribute=");
                for (String attributeName : attributes) {
                    buf.append(" '").append(attributeName).append("'");
                }
                buf.append(";");
                executeImmediately(buf.toString());
            }
        }
    }


    public boolean isApplicable() {
        final SamplesViewer samplesViewer = ((SamplesViewer) getViewer());
        return samplesViewer != null && samplesViewer.getSamplesTableView().getCountSelectedAttributes() > 0;
    }

    public String getName() {
        return "Delete Column(s)...";
    }


    public String getDescription() {
        return "Delete selected columns";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/ColumnDelete16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
