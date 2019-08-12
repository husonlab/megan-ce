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
package megan.commands.format;

import jloda.graph.Edge;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.GraphView;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * set shape
 * Daniel Huson, 4.2011
 */
public class SetEdgeShapeCommand extends CommandBase implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Edge Shape";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the shape of selected edges";
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
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set edgeShape=");
        String shapeName = np.getWordMatchesIgnoringCase("angular straight curved none");
        np.matchIgnoreCase(";");

        byte shape;
        if (shapeName.equalsIgnoreCase("angular"))
            shape = EdgeView.ARC_LINE_EDGE;
        else if (shapeName.equalsIgnoreCase("straight"))
            shape = EdgeView.STRAIGHT_EDGE;
        else if (shapeName.equalsIgnoreCase("curved"))
            shape = EdgeView.QUAD_EDGE;
        else
            shape = 0;

        if (getViewer() instanceof GraphView) {
            boolean changed = false;
            GraphView viewer = (GraphView) getViewer();
            for (Edge e : viewer.getSelectedEdges()) {
                viewer.setShape(e, shape);
                changed = true;
            }
            if (changed) {
                viewer.repaint();
            }
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        String[] choices = new String[]{"angular", "straight", "curved", "none"};

        String result = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Set edge shape", "Set edge shape", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), choices, choices[0]);

        if (result != null)
            execute("set edgeShape=" + result + ";");
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
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set edgeShape={angular|straight|curved};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getViewer() instanceof GraphView &&
                ((GraphView) getViewer()).getSelectedEdges().size() > 0;
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
