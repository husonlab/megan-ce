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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.fx.NotificationsInSwing;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * * make a new attribute
 * * Daniel Huson, 19.2015
 */
public class ExpandAttributesCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "expand attribute=<name>;";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("expand attribute=");
        final String attribute = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        final Director dir = (Director) getDir();
        final Document doc = dir.getDocument();
        final SamplesViewer samplesViewer = (SamplesViewer) getDir().getViewerByClass(SamplesViewer.class);


        if (samplesViewer != null) {
            // todo: sync to document...
            // ((SamplesViewer) getViewer()).getSamplesTable().syncFromDocument();
        }

        final int count = doc.getSampleAttributeTable().expandAttribute(attribute, true);

        if (count > 0 && samplesViewer != null)
            samplesViewer.getSamplesTable().syncFromDocument();

        if (count == 0)
            NotificationsInSwing.showWarning(getViewer().getFrame(), "Expand attribute failed");
        else
            NotificationsInSwing.showInformation(getViewer().getFrame(), "Expand " + attribute + "' added " + count + " columns");
    }

    public void actionPerformed(ActionEvent event) {
        final SamplesViewer viewer = ((SamplesViewer) getViewer());
        final String attributeName = viewer.getSamplesTable().getASelectedColumn();

        final Set<Object> states = new TreeSet<>();
        states.addAll(viewer.getSampleAttributeTable().getSamples2Values(attributeName).values());

        boolean ok = false;
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("MEGAN Expand Dialog");
            alert.setHeaderText("Expanding '" + attributeName + "' will add " + states.size() + " new columns");
            alert.setContentText("Proceed?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                ok = true;
                // ... user chose OK
            }
        } else if (SwingUtilities.isEventDispatchThread()) {
            int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "Expanding '" + attributeName + "' will add " + states.size() + " new columns, proceed?");
            if (result == JOptionPane.YES_OPTION)
                ok = true;
        }
        if (ok)
            execute("expand attribute='" + attributeName + "';");
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTable().getSelectedColumns().size() == 1;
    }

    public String getName() {
        return "Expand...";
    }


    public String getDescription() {
        return "Expands an attribute by adding one 0/1 column per value";
    }

    public ImageIcon getIcon() {
        return null; // ResourceManager.getIcon("sun/toolbarButtonGraphics/table/ColumnInsertAfter16.gif");

    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
