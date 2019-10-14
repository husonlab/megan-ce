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
package megan.commands.select;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.ProjectManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.groups.GroupsViewer;
import megan.viewer.ClassificationViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * * selection command
 * * Daniel Huson, 11.2010
 */
public class SelectAllNodesCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "select nodes={all|none|leaves|internal|previous|subtree|leavesBelow|nodesAbove|intermediate|invert}";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {

        np.matchIgnoreCase("select nodes=");
        String what = np.getWordMatchesIgnoringCase("all none leaves internal previous subTree leavesBelow subLeaves nodesAbove intermediate invert positiveAssigned");
        np.matchRespectCase(";");

        final ViewerBase viewer;
        if (getViewer() instanceof ViewerBase)
            viewer = (ViewerBase) getViewer();
        else if (getViewer() instanceof ClusterViewer && ((ClusterViewer) getViewer()).getGraphView() instanceof ViewerBase)
            viewer = (ViewerBase) ((ClusterViewer) getViewer()).getGraphView();
        else if (getViewer() instanceof ClusterViewer && ((ClusterViewer) getViewer()).getTabbedIndex() == ClusterViewer.MATRIX_TAB_INDEX && what.equalsIgnoreCase("previous")) {
            ((ClusterViewer) getViewer()).getMatrixTab().selectByLabels(ProjectManager.getPreviouslySelectedNodeLabels());
            return;
        } else if (getViewer() instanceof GroupsViewer) {
            switch (what.toLowerCase()) {
                case "previous":
                    ((GroupsViewer) getViewer()).selectFromPrevious(ProjectManager.getPreviouslySelectedNodeLabels());
                    break;
                case "all":
                    ((GroupsViewer) getViewer()).getGroupsPanel().selectAll();
                    break;
                case "none":
                    ((GroupsViewer) getViewer()).getGroupsPanel().selectNone();
                    break;
            }
            return;
        } else
            return;

        if (what.equalsIgnoreCase("all"))
            viewer.selectAllNodes(true);
        else if (what.equals("none"))
            viewer.selectAllNodes(false);
        else if (what.equals("leaves"))
            viewer.selectAllLeaves();
        else if (what.equals("internal"))
            viewer.selectAllInternal();
        else if (what.equals("previous"))
            viewer.selectNodesByLabels(ProjectManager.getPreviouslySelectedNodeLabels(), true);
        else if (what.equalsIgnoreCase("subTree"))
            viewer.selectSubTreeNodes();
        else if (what.equals("subLeaves") || what.equals("leavesBelow"))
            viewer.selectLeavesBelow();
        else if (what.equals("nodesAbove"))
            viewer.selectNodesAbove();
        else if (what.equals("intermediate"))
            viewer.selectAllIntermediateNodes();
        else if (what.equals("invert"))
            viewer.invertNodeSelection();
        else if (what.equals("positiveAssigned")) {
            if (viewer instanceof ClassificationViewer)
                ((ClassificationViewer) viewer).selectNodesPositiveAssigned();
            else
                NotificationsInSwing.showWarning("select nodes=" + what + ": not implemented for this type of viewer");
        }
        System.err.println("Number of nodes selected: " + viewer.getNumberSelectedNodes());
        viewer.repaint();

    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("select nodes=all;");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "All Nodes";
    }

    public String getDescription() {
        return "Select nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Empty16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
