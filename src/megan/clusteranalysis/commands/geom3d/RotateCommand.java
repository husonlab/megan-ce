/*
 * RotateCommand.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.clusteranalysis.commands.geom3d;

import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.commands.CommandBase;
import megan.clusteranalysis.pcoa.geom3d.Matrix3D;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * rotate command
 */
public class RotateCommand extends CommandBase implements ICommand {
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
        ClusterViewer viewer = getViewer();
        np.matchIgnoreCase("rotate axis=");
        String axis = np.getWordMatchesIgnoringCase("x y z");
        np.matchIgnoreCase("angle=");
        double angle = np.getDouble();
        Matrix3D matrix = viewer.getPcoaTab().getTransformation3D();
        if (axis.equalsIgnoreCase("x")) {
            matrix.rotateX(angle);
        } else if (axis.equalsIgnoreCase("y")) {
            matrix.rotateY(angle);
        } else if (axis.equalsIgnoreCase("z")) {
            matrix.rotateZ(angle);
        }
        viewer.getPcoaTab().updateTransform(true);
        if (viewer.getPcoaTab().isShowGroupsAsConvexHulls())
            viewer.getPcoaTab().computeConvexHullsAndEllipsesForGroups(viewer.getGroup2Nodes());
        np.matchIgnoreCase(";");
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        String input = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter axis and angle");
        if (input != null) {
            char axis = input.charAt(0);
            double angle = Basic.parseDouble(input.substring(1));
            executeImmediately("rotate axis=" + axis + " angle=" + angle + ";");
        }
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return false;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "rotate axis={x|y|z} angle=<number>;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Rotate...";
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Three-dimensional rotation";
    }
}
