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
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * close remote server connection
 * Daniel Huson, 10.2014
 */
public class CloseRemoteServerCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("detach remoteServer=");
        final String url = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        if (((megan.remote.RemoteServiceBrowser) getViewer()).closeRemoteService(url))
            System.err.println("Service closed: " + url);
        else
            NotificationsInSwing.showError(getViewer().getFrame(), "Failed to close service: " + url);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "detach remoteServer=<url>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        final megan.remote.RemoteServiceBrowser remoteServiceBrowser = (megan.remote.RemoteServiceBrowser) getViewer();
        if (remoteServiceBrowser != null) {
            String url = remoteServiceBrowser.getURL();

            if (url.length() > 0) {
                execute("detach remoteServer=" + url + ";");
            }
        }
    }

    private static final String NAME = "Close";

    public String getName() {
        return NAME;
    }

    private static final String ALT_NAME = "Close Remote Server...";

    public String getAltName() {
        return ALT_NAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Close remote server";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Close16.gif");
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
        return getViewer() != null && getViewer() instanceof megan.remote.RemoteServiceBrowser && ((megan.remote.RemoteServiceBrowser) getViewer()).isServiceSelected();
    }
}
