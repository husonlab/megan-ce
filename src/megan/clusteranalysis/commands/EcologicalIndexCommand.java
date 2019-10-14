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
package megan.clusteranalysis.commands;

import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.indices.DistancesManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * the ecological index
 * Daniel Huson, 6.2010
 */
abstract class EcologicalIndexCommand extends CommandBase {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set index=");
        String method = np.getWordMatchesIgnoringCase(DistancesManager.getAllNames());
        np.matchIgnoreCase(";");

        ClusterViewer viewer = getViewer();
        viewer.setEcologicalIndex(method);
        executeImmediately("sync;");
    }

    /**
     * get Command Syntax.... First two tokens are used to identify the command
     *
     * @return usage
     */
    public String getSyntax() {
        return "set index=<index-name>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        final ClusterViewer viewer = getViewer();

        final String method = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Set Ecological Index", "Set Ecological Index", JOptionPane.QUESTION_MESSAGE,
                ProgramProperties.getProgramIcon(), DistancesManager.getAllNames(), viewer.getEcologicalIndex());
        if (method != null)
            executeImmediately("set index=" + method + ";");
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
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getViewer().getParentViewer() != null && getViewer().getParentViewer().hasComparableData()
                && getViewer().getParentViewer().getSelectedNodes().size() > 0;
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
