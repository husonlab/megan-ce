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
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.dialogs.compare.CompareWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

/**
 * compare command
 * Daniel Huson, 11.2010
 */
public class CompareSelectedFilesCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        final megan.remote.RemoteServiceBrowser remoteServiceBrowser = (megan.remote.RemoteServiceBrowser) getViewer();

        final megan.remote.ServicePanel servicePanel = remoteServiceBrowser.getServicePanel();
        if (servicePanel != null) {
            final Collection<String> selectedFiles = remoteServiceBrowser.getServicePanel().getSelectedFiles();
            if (selectedFiles.size() > 1) {
                CompareWindow compareWindow = new CompareWindow(getViewer().getFrame(), remoteServiceBrowser.getDir(), selectedFiles);
                if (!compareWindow.isCanceled()) {
                    final Director newDir = Director.newProject();
                    newDir.getMainViewer().getFrame().setVisible(true);
                    newDir.getMainViewer().setDoReInduce(true);
                    newDir.getMainViewer().setDoReset(true);
                    final String command = compareWindow.getCommand();
                    if (command != null)
                        newDir.execute(command, newDir.getCommandManager());
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
        return "compare;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    /**
     * get id of open file
     *
     * @param fileName
     * @return
     */
    private int getPID(String fileName) {
        for (final IDirector iDir : ProjectManager.getProjects()) {
            Director dir = (Director) iDir;
            if (dir.getDocument().getMeganFile().getFileName().equals(fileName))
                return dir.getID();
        }
        return -1;
    }

    public String getName() {
        return "Compare";
    }

    public static final String ALTNAME = "Compare Selected Files";

    public String getAltName() {
        return ALTNAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Open all selected files in a comparison document";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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

        return remoteServiceBrowser != null && remoteServiceBrowser.getServicePanel() != null && remoteServiceBrowser.getServicePanel().getSelectedFiles().size() > 1;
    }
}
