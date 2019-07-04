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
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.remote.RemoteServiceBrowser;
import megan.remote.ServicePanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Set;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class OpenSelectedFilesCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        ServicePanel servicePanel = ((RemoteServiceBrowser) getViewer()).getServicePanel();
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
        return "openSelectedFiles;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        final RemoteServiceBrowser remoteServiceBrowser = (RemoteServiceBrowser) getViewer();

        final ServicePanel servicePanel = remoteServiceBrowser.getServicePanel();
        if (servicePanel != null) {
            final Collection<String> selectedFiles = remoteServiceBrowser.getServicePanel().getSelectedFiles();

            final StringBuilder buf = new StringBuilder();

            int count = 0;
            Set<String> openFiles = servicePanel.getCurrentlyOpenRemoteFiles();
            for (String fileName : selectedFiles) {
                if (openFiles.contains(fileName)) {
                    buf.append("toFront file='").append(fileName).append("';");
                } else {
                    buf.append("open file='").append(fileName).append("' readOnly=true;");
                    count++;
                }
            }
            if (count > 10) {
                if (JOptionPane.showConfirmDialog(remoteServiceBrowser.getFrame(), "Do you really want to open " + count + " new files?", "Confirm", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon()) == JOptionPane.NO_OPTION)
                    return;
            }
            execute(buf.toString());
        }
    }

    public String getName() {
        return "Open";
    }

    public static final String ALTNAME = "Open Selected Files";

    public String getAltName() {
        return ALTNAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Open all selected files";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Open16.gif");
    }


    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
        RemoteServiceBrowser remoteServiceBrowser = (RemoteServiceBrowser) getViewer();

        return remoteServiceBrowser != null && remoteServiceBrowser.getServicePanel() != null && remoteServiceBrowser.getServicePanel().getSelectedFiles().size() > 0;
    }
}
