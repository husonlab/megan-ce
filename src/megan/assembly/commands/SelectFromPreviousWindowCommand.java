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
package megan.assembly.commands;

import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.NodeSet;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.ProjectManager;
import jloda.swing.graphview.GraphView;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.assembly.OverlapGraphViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

/**
 * select from previous window
 * Daniel Huson, 5.2015
 */
public class SelectFromPreviousWindowCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        if (getParent() instanceof OverlapGraphViewer) {
            final OverlapGraphViewer overlapGraphViewer = (OverlapGraphViewer) getParent();
            final GraphView graphView = overlapGraphViewer.getGraphView();
            final NodeArray<String> node2ReadNameMap = overlapGraphViewer.getNode2ReadNameMap();

            final Set<String> previousSelection = ProjectManager.getPreviouslySelectedNodeLabels();
            if (previousSelection.size() > 0) {
                Graph graph = graphView.getGraph();

                NodeSet toSelect = new NodeSet(graph);
                for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
                    String label = node2ReadNameMap.get(v);

                    if (label != null && previousSelection.contains(label))
                        toSelect.add(v);
                }
                if (toSelect.size() > 0) {
                    graphView.setSelected(toSelect, true);
                    graphView.repaint();
                }
            }
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "select what=previous;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("select what=previous;");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getAltName() {
        return "From Previous Alignment";
    }

    public String getName() {
        return "Select From Previous Window";
    }

    public String getDescription() {
        return "Select from previous window";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Empty16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
