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
import jloda.graph.Node;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.GraphView;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * set size
 * Daniel Huson, 4.2011
 */
public class IncreaseFontCommand extends CommandBase implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Increase Font Size";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the font size";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set fontSize=");
        String input = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        int newSize = 0;
        boolean increase = false;
        boolean decrease = false;
        if (Basic.isInteger(input) && Integer.parseInt(input) >= 0)
            newSize = Integer.parseInt(input);
        else if (input.equalsIgnoreCase("increase"))
            increase = true;
        else if (input.equalsIgnoreCase("decrease"))
            decrease = true;
        else
            throw new IOException("Illegal font size: " + input);

        boolean changed = false;

        final GraphView viewer;
        if (getViewer() instanceof ClusterViewer)
            viewer = ((ClusterViewer) getViewer()).getGraphView();
        else if (getViewer() instanceof GraphView)
            viewer = (GraphView) getViewer();
        else return;

        final Set<Node> nodes = new HashSet<>();
        if (viewer.getSelectedNodes().size() == 0 && viewer.getSelectedEdges().size() == 0) {
            for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext())
                nodes.add(v);
        } else
            nodes.addAll(viewer.getSelectedNodes());

        for (Node v : nodes) {
            if (viewer.getLabel(v) != null) {
                Font font = viewer.getFont(v);
                int size = font.getSize();
                if (increase) {
                    size = Math.max(size + 1, (int) (size * 1.1));
                } else if (decrease) {
                    if (size >= 2)
                        size = Math.max(1, Math.min(size - 1, (int) (size / 1.1)));
                } else {
                    size = newSize;
                }
                if (size != font.getSize()) {
                    font = new Font(font.getFamily(), font.getStyle(), size);
                    viewer.setFont(v, font);
                    ProgramProperties.put(ProgramProperties.DEFAULT_FONT, font.getFamily(), font.getStyle(), size > 0 ? size : 6);
                    changed = true;
                }
            }
        }
        for (Edge e : viewer.getSelectedEdges()) {
            if (viewer.getLabel(e) != null) {
                Font font = viewer.getFont(e);
                int size = font.getSize();
                if (increase) {
                    size = Math.max(size + 1, (int) (size * 1.2));
                } else if (decrease) {
                    if (size >= 2)
                        size = Math.max(1, Math.min(size - 1, (int) (size / 1.1)));
                } else {
                    size = newSize;
                }
                if (size != font.getSize()) {
                    font = new Font(font.getFamily(), font.getStyle(), size);
                    viewer.setFont(e, font);
                    ProgramProperties.put(ProgramProperties.DEFAULT_FONT, font.getFamily(), font.getStyle(), size > 0 ? size : 6);
                    changed = true;
                }
            }
        }
        if (changed)
            viewer.repaint();
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        execute("set fontSize=increase;");
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
        return "set fontSize={<number>|increase|decrease};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        if (getViewer() instanceof ViewerBase)
            return ((ViewerBase) getViewer()).getGraph().getNumberOfNodes() > 0;
        else
            return getViewer() instanceof ClusterViewer && ((ClusterViewer) getViewer()).getGraphView() != null && ((ClusterViewer) getViewer()).getGraphView().getGraph().getNumberOfNodes() > 0;
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
