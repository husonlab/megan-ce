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
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * set size
 * Daniel Huson, 4.2011
 */
public class SetFontCommand extends CommandBase implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Font";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set font nodes or edges, e.g. arial-italic-12";
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
        np.matchIgnoreCase("set font=");
        String fontName = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        Font font = Font.decode(fontName);

        boolean changed = false;

        ViewerBase viewer = (ViewerBase) getViewer();
        Set<Node> nodes = new HashSet<>();
        if (viewer.getSelectedNodes().size() == 0 && viewer.getSelectedEdges().size() == 0) {
            for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext())
                nodes.add(v);
        } else
            nodes.addAll(viewer.getSelectedNodes());

        for (Node v : nodes) {
            viewer.setFont(v, font);
            changed = true;
        }
        for (Edge e : viewer.getSelectedEdges()) {
            viewer.setFont(e, font);
            changed = true;
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

        String fontName = ProgramProperties.get("Font", "Arial-PLAIN-12");
        fontName = JOptionPane.showInputDialog("Enter font", fontName);
        if (fontName != null) {
            execute("set font='" + fontName + "';");
            ProgramProperties.put("Font", fontName);
        }
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
        return "set font=<name-style-size>;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((ViewerBase) getViewer()).getGraph().getNumberOfNodes() > 0;
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
