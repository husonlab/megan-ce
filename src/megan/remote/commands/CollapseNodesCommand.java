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
package megan.remote.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class CollapseNodesCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        megan.remote.ServicePanel servicePanel = ((megan.remote.RemoteServiceBrowser) getViewer()).getServicePanel();
        if (servicePanel != null) {
            TreePath[] paths = servicePanel.getFileTree().getSelectionPaths();
            if (paths != null)
                servicePanel.collapse(paths);
            else servicePanel.collapse((DefaultMutableTreeNode) servicePanel.getFileTree().getModel().getRoot());
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "collapse;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
    }

    public String getName() {
        return "Collapse";
    }

    public static final String ALTNAME = "Collapse Remote Browser Nodes";

    public String getAltName() {
        return ALTNAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Collapse the selected nodes, or all, if none selected";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/AlignJustifyHorizontal16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
        megan.remote.RemoteServiceBrowser remoteServiceBrowser = (megan.remote.RemoteServiceBrowser) getViewer();

        return remoteServiceBrowser != null && remoteServiceBrowser.getServicePanel() != null && remoteServiceBrowser.getServicePanel().getFileTree().getModel().getRoot() != null
                && remoteServiceBrowser.getServicePanel().getFileTree().getModel().getChildCount(remoteServiceBrowser.getServicePanel().getFileTree().getModel().getRoot()) > 0;
    }
}
