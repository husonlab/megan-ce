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
package megan.clusteranalysis.commands.zoom;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.GraphView;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.gui.ITab;
import megan.clusteranalysis.gui.PCoATab;

import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * Set scale command
 * Daniel Huson, 6.2010
 */
public class SetScaleCommand extends CommandBase implements ICommand {
    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set scaleFactor=<number>;";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set scaleFactor=");
        double scale = np.getDouble(0.000001, 100000);
        np.matchIgnoreCase(";");

        final ClusterViewer viewer = (ClusterViewer) getViewer();

        if (viewer.getSelectedComponent() instanceof ITab) {
            if (viewer.getSelectedComponent() instanceof PCoATab)
                scale /= PCoATab.COORDINATES_SCALE_FACTOR;
            GraphView graphView = viewer.getGraphView();
            graphView.centerGraph();
            graphView.trans.setScale(scale, scale);
        }
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Scale...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the scale factor";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/AlignCenter16.gif");
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
        final ClusterViewer viewer = (ClusterViewer) getViewer();
        if (viewer.getSelectedComponent() instanceof ITab) {
            final GraphView graphView = viewer.getGraphView();
            double scale = graphView.trans.getScaleX();
            if (viewer.getSelectedComponent() instanceof PCoATab)
                scale *= PCoATab.COORDINATES_SCALE_FACTOR;

            final String result = JOptionPane.showInputDialog(viewer.getFrame(), "Set scale", Basic.removeTrailingZerosAfterDot(String.format("%.4f", scale)));
            if (result != null && Basic.isDouble(result) && Basic.parseDouble(result) > 0) {
                execute("set scaleFactor=" + Basic.parseDouble(result) + ";");
            }
        }
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getViewer() instanceof ClusterViewer && ((ClusterViewer) getViewer()).getSelectedComponent() instanceof ITab;
    }

    public boolean isCritical() {
        return true;
    }
}
