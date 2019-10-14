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
package megan.samplesviewer.commands.attributes;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

/**
 * * make a new attribute
 * * Daniel Huson, 19.2015
 */
public class NewAttributeCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "new attribute=<name> position=<number>;";
    }


    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("new attribute=");
        String attribute = np.getWordRespectCase();
        np.matchIgnoreCase("position=");
        int position = np.getInt();
        np.matchIgnoreCase(";");

        final SamplesViewer viewer = ((SamplesViewer) getViewer());

        viewer.getSamplesTableView().addNewColumn(position, attribute);
    }

    public void actionPerformed(ActionEvent event) {
        final SamplesViewer viewer = ((SamplesViewer) getViewer());
        final int index;
        final String selectedAttribute = viewer.getSamplesTableView().getASelectedAttribute();
        if (selectedAttribute != null)
            index = viewer.getSamplesTableView().getAttributes().indexOf(selectedAttribute);
        else
            index = viewer.getSamplesTableView().getSampleCount();

        String name = null;
            if (Platform.isFxApplicationThread()) {
                TextInputDialog dialog = new TextInputDialog("Attribute");
                dialog.setTitle("New attribute");
                dialog.setHeaderText("Enter attribute name:");

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    name = result.get().trim();
                }
            } else if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                name = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter new attribute name", "Untitled");
            }

        if (name != null)
            executeImmediately("new attribute='" + name + "' position=" + index + ";");
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer;
    }

    public String getName() {
        return "New Column...";
    }


    public String getDescription() {
        return "Create a new attribute (column) in the data table";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/ColumnInsertAfter16.gif");

    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_J, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
