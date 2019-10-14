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
import javafx.scene.control.ChoiceDialog;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.NodeShape;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Optional;

/**
 * draw selected nodes as circles
 * Daniel Huson, 3.2013
 */
public class SetSampleShapeCommand extends CommandBase implements ICommand {
    /**
     * apply
     *
     * @param np
     * @throws Exception
     */
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
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

    public boolean isApplicable() {
        return true;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Shape...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the sample shape";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("BlueTriangle16.gif");
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
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        final SamplesViewer viewer = (SamplesViewer) getViewer();

        final Collection<String> selected = viewer.getSamplesTableView().getSelectedSamples();

        if (selected.size() > 0) {
            String sample = selected.iterator().next();
            String shapeLabel = viewer.getSampleAttributeTable().getSampleShape(sample);
            NodeShape nodeShape = NodeShape.valueOfIgnoreCase(shapeLabel);
            if (nodeShape == null)
                nodeShape = NodeShape.Oval;
            final NodeShape nodeShape1 = nodeShape;

            Runnable runnable = () -> {
                final ChoiceDialog<NodeShape> dialog = new ChoiceDialog<>(nodeShape1, NodeShape.values());
                dialog.setTitle("MEGAN choice");
                dialog.setHeaderText("Choose shape to represent sample(s)");
                dialog.setContentText("Shape:");

                final Optional<NodeShape> result = dialog.showAndWait();
                result.ifPresent(shape -> execute("set nodeShape=" + shape + " sample='" + Basic.toString(selected, "' '") + "';"));
            };
            if (Platform.isFxApplicationThread())
                runnable.run();
            else
                Platform.runLater(runnable);
        }
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
