/*
 * UnweightedTaxonomicUniFracCommand.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.clusteranalysis.commands;

import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.indices.UniFrac;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * UnweightedTaxonomicUniFrac command
 * Daniel Huson, 11.2017
 */
public class UnweightedTaxonomicUniFracCommand extends CommandBase implements ICheckBoxCommand {
    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        ClusterViewer viewer = getViewer();
        return viewer.getEcologicalIndex().equalsIgnoreCase(UniFrac.UnweightedUniformUniFrac);
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Use Unweighted Uniform UniFrac";
    }

    /**
     * get description to be used as a tool-tip
     *
     * @return description
     */
    public String getDescription() {
        return "Use the unweighted uniform UniFrac metric.\n" +
                "For any two samples, this is the proportion of ranked taxonomic nodes on which exactly one sample has a zero count (Lozupone and Knight, 2005)";
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
	 */
    public void actionPerformed(ActionEvent ev) {
        execute("set index=" + UniFrac.UnweightedUniformUniFrac + ";");
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
    public void apply(NexusStreamParser np) {

    }

    @Override
    public String getSyntax() {
        return null;
    }

    @Override
    public boolean isCritical() {
        return true;
    }

    @Override
    public boolean isApplicable() {
        return getViewer().getParentViewer() != null && getViewer().getParentViewer() instanceof MainViewer
                && getViewer().getParentViewer().getSelectedNodes().size() > 0;
    }
}
