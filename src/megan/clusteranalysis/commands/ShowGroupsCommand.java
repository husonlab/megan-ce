/*
 * ShowGroupsCommand.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.swing.director.IDirector;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * show groups as ellipses
 * Daniel Huson, 9.2016
 */
public class ShowGroupsCommand extends CommandBase implements ICheckBoxCommand {
    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        ClusterViewer viewer = getViewer();
        return viewer.getPcoaTab() != null && viewer.getPcoaTab().isShowGroupsAsEllipses();
    }

    /**
     * parses the given command and executes it
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set showGroups=");
        final boolean show = np.getBoolean();
        final String style;
        if (np.peekMatchIgnoreCase("style")) {
            np.matchIgnoreCase("style=");
            style = np.getWordMatchesIgnoringCase("ellipses convexHulls");
        } else
            style = "ellipses";
        np.matchIgnoreCase(";");

        final ClusterViewer viewer = getViewer();
        if (style.equalsIgnoreCase("ellipses"))
            viewer.getPcoaTab().setShowGroupsAsEllipses(show);
        else
            viewer.getPcoaTab().setShowGroupsAsConvexHulls(show);
        try {
            if (show)
                viewer.getPcoaTab().computeConvexHullsAndEllipsesForGroups(viewer.getGroup2Nodes());
            viewer.updateView(IDirector.ENABLE_STATE);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set showGroups={false|true} [style={ellipses|convexHulls}];";
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
        return getViewer().isPCoATab();
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Show Groups";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show groups";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null; //ResourceManager.getIcon("PC1vPC2_16.gif");
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
        executeImmediately("set showGroups=" + (!isSelected()) + " style=ellipses;");
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
