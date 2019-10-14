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
package megan.samplesviewer.commands.samples;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.SampleAttributeTable;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

/**
 * * rename command
 * * Daniel Huson, 9.2012
 */
public class RenameSampleCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "rename sample=<name> newName=<name>;";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("rename sample=");
        final String sampleName = np.getWordRespectCase();
        np.matchIgnoreCase("newName=");
        String newName = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        (((Director) getDir()).getDocument()).renameSample(sampleName, newName);
    }

    public void actionPerformed(ActionEvent event) {
        SamplesViewer viewer = (SamplesViewer) getViewer();
        String sampleName = viewer.getSamplesTableView().getASelectedSample();
        String newName = null;
        if (Platform.isFxApplicationThread()) {
            TextInputDialog dialog = new TextInputDialog(sampleName);
            dialog.setTitle("Rename sample");
            dialog.setHeaderText("Enter new sample name:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                newName = result.get().trim();
            }
        } else if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            newName = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter new sample name", sampleName);
        }

        if (newName != null && !newName.equals(sampleName)) {
            if (viewer.getSampleAttributeTable().getSampleOrder().contains(newName)) {
                int count = 1;
                while (viewer.getSampleAttributeTable().getSampleOrder().contains(newName + "." + count))
                    count++;
                newName += "." + count;
            }
            execute("rename sample='" + sampleName + "' newName='" + newName + "';" +
                    "labelBy attribute='" + SampleAttributeTable.SAMPLE_ID + "'  samples=all;");
        }
    }

    public boolean isApplicable() {
        SamplesViewer viewer = (SamplesViewer) getViewer();
        return !viewer.getDocument().getMeganFile().hasDataConnector() && (viewer.getSamplesTableView().getCountSelectedSamples() == 1);
    }

    public String getName() {
        return "Rename...";
    }

    public String getAltName() {
        return "Rename Sample...";
    }


    public String getDescription() {
        return "Rename selected sample";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }
}
