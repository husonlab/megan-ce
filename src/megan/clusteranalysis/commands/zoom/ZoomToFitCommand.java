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
package megan.clusteranalysis.commands.zoom;

import jloda.swing.commands.ICommand;
import jloda.swing.graphview.GraphView;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.commands.CommandBase;
import megan.clusteranalysis.gui.ITab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


/**
 * zoom to fit
 * Daniel Huson, 6.2010
 */
public class ZoomToFitCommand extends CommandBase implements ICommand {
    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "zoom what=<{fit|full|selection}>;";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("zoom what=");
        String what = np.getWordMatchesIgnoringCase("fit full selection");
        np.matchIgnoreCase(";");

        ClusterViewer viewer = getViewer();
        if (viewer.getSelectedComponent() instanceof ITab) {
            ITab tab = ((ITab) viewer.getSelectedComponent());
            if (what.equalsIgnoreCase("fit"))
                tab.zoomToFit();
            else if (what.equalsIgnoreCase("selection")) {
                tab.zoomToSelection();
            } else if (what.equalsIgnoreCase("full")) { // doesn't work for PCoA3D
                GraphView graphView = viewer.getGraphView();
                graphView.fitGraphToWindow();
                graphView.trans.setScaleY(1);
            }
        }
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Zoom to Fit";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Zoom";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        execute("zoom what=fit;");
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}
