/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.samplesviewer.commands.attributes;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

/**
 * * rename command
 * * Daniel Huson, 9.2105
 */
public class RenameAttributeCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "rename attribute=<name> newName=<name>;";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("rename attribute=");
        final String attribute = np.getWordRespectCase();
        np.matchIgnoreCase("newName=");
        String newName = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        final SamplesViewer viewer = (SamplesViewer) getDir().getViewerByClass(SamplesViewer.class);
        viewer.getSamplesTable().renameAttribute(attribute, newName);
    }

    public void actionPerformed(ActionEvent event) {
        SamplesViewer viewer = (SamplesViewer) getViewer();
        String attribute = viewer.getSamplesTable().getSelectedAttributes().iterator().next();
        String newName = null;
        if (Platform.isFxApplicationThread()) {
            TextInputDialog dialog = new TextInputDialog(attribute);
            dialog.setTitle("Rename attribute");
            dialog.setHeaderText("Enter new attribute name:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                newName = result.get().trim();
            }
        } else if (SwingUtilities.isEventDispatchThread()) {
            newName = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter new attribute name", attribute);
        }

        if (newName != null && !newName.equals(attribute)) {
            if (viewer.getSampleAttributeTable().getAttributeOrder().contains(newName)) {
                int count = 1;
                while (viewer.getSampleAttributeTable().getAttributeOrder().contains(newName + "." + count))
                    count++;
                newName += "." + count;
            }
            execute("rename attribute='" + attribute + "' newName='" + newName + "';");
        }
    }

    public boolean isApplicable() {
        SamplesViewer viewer = (SamplesViewer) getViewer();
        return !viewer.getDocument().getMeganFile().hasDataConnector() && (viewer.getSamplesTable().getSelectedColumns().size() == 1);
    }

    public String getName() {
        return "Rename...";
    }

    public String getAltName() {
        return "Rename Attribute...";
    }


    public String getDescription() {
        return "Rename selected attribute";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
