/*
 * ScrollToNodeCommand.java Copyright (C) 2023 Daniel H. Huson
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
package megan.commands;

import jloda.graph.Node;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * scroll to a specific node
 * Daniel Huson, 8.2011
 */
public class ScrollToNodeCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Scroll To...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Scroll to a specific node";
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
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("scrollTo node=");
        String name = np.getWordRespectCase();
        np.matchWordIgnoreCase(";");

        if (getViewer() instanceof ViewerBase) {
            ViewerBase viewerBase = (ViewerBase) getViewer();
            if (name.equalsIgnoreCase("selected")) {
                List<String> labels = viewerBase.getSelectedNodeLabels(true);
                if (labels.size() > 0) {
                    name = labels.get(0);
                } else
                    return;
            }

            for (Node v = viewerBase.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                String label = viewerBase.getLabel(v);
                if (label != null && label.equals(name)) {
                    viewerBase.scrollToNode(v);
                    break;
                }
            }
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
        return "scrollTo node={<name>|selected};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getGraph().getNumberOfNodes() > 0;
    }

    /**
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        String input = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter label of node to scroll to", "None");
        if (input != null) {
            input = input.trim();
            if (input.length() > 0)
                execute("scrollTo node='" + input + "';");
        }
    }
}
