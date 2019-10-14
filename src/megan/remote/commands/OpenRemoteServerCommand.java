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
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.remote.IRemoteService;
import megan.remote.client.LocalService;
import megan.remote.client.RemoteServiceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 12.2014
 */
public class OpenRemoteServerCommand extends CommandBase implements ICommand {
    private static String hiddenPassword;
    private static final String HIDDEN_PASSWORD = "******";
    private static final Object syncObject = new Object();

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("openServer url=");
        final String url = np.getWordFileNamePunctuation();

        String user = "";
        if (np.peekMatchIgnoreCase("user")) {
            np.matchIgnoreCase("user=");
            user = np.getWordRespectCase();
        }
        String password = "";
        if (np.peekMatchIgnoreCase("password")) {
            np.matchIgnoreCase("password=");
            password = np.getWordRespectCase();
            synchronized (syncObject) {
                if (password.equals(HIDDEN_PASSWORD) && hiddenPassword != null)
                    password = hiddenPassword;
                hiddenPassword = null;
            }
        }

        np.matchIgnoreCase(";");
        if (!((megan.remote.RemoteServiceBrowser) getViewer()).selectServiceTab(url)) {
            IRemoteService service = RemoteServiceManager.createService(url, user, password);
            if (service instanceof LocalService) {
                ((LocalService) service).rescan(((Director) getDir()).getDocument().getProgressListener());
            }
            if (service.isAvailable()) {
                ((megan.remote.RemoteServiceBrowser) getViewer()).addService(service);
                ((megan.remote.RemoteServiceBrowser) getViewer()).saveConfig();
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
        return "openServer url=<url> [user=<user>] [password=<hiddenPassword>];";
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
            String user = remoteServiceBrowser.getUser();
            String password = remoteServiceBrowser.getPasswd();

            String command = "openServer url='" + url + "'";
            if (user.length() > 0)
                command += " user='" + user + "'";
            if (password.length() > 0) {
                synchronized (syncObject) {
                    OpenRemoteServerCommand.hiddenPassword = password;
                }
                command += " password='" + HIDDEN_PASSWORD + "'";
            }
            command += ";";
            if (url.length() > 0) {
                execute(command);
            }
        }
    }

    private static final String NAME = "Open";

    public String getName() {
        return NAME;
    }

    public static final String ALT_NAME = "Open Server...";

    public String getAltName() {
        return ALT_NAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Open a remote server";
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
        if (getViewer() == null || !(getViewer() instanceof megan.remote.RemoteServiceBrowser))
            return false;
        final megan.remote.RemoteServiceBrowser remoteServiceBrowser = (megan.remote.RemoteServiceBrowser) getViewer();

        return !remoteServiceBrowser.isServiceSelected() && remoteServiceBrowser.getURL().length() > 0;
    }
}
