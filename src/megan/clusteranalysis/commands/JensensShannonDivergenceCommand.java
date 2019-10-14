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

import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.indices.JensenShannonDivergence;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * JensensShannonDivergenceCommand
 * Daniel Huson, 7.2014
 */
public class JensensShannonDivergenceCommand extends CommandBase implements ICheckBoxCommand {
    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        ClusterViewer viewer = getViewer();
        return viewer.getEcologicalIndex().equalsIgnoreCase(JensenShannonDivergence.NAME);
    }

    private static final String NAME = "Use JSD";

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }

    /**
     * get description to be used as a tool-tip
     *
     * @return description
     */
    public String getDescription() {
        return "Use square root of Jensen-Shannon divergence (see Arumugam et al. 2011)";
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
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        execute("set index=" + JensenShannonDivergence.NAME + ";");
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
    public boolean isApplicable() {
        return getViewer().getParentViewer() != null && getViewer().getParentViewer().hasComparableData()
                && getViewer().getParentViewer().getSelectedNodes().size() > 0;
    }

    @Override
    public void apply(NexusStreamParser np) throws Exception {

    }

    @Override
    public String getSyntax() {
        return null;
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}
