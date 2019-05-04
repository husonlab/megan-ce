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
package megan.commands.remote;

import jloda.swing.commands.ICommand;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.remote.client.RemoteServiceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddServerCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "addServer url=<url> [user=<user>] [password=<password>];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("addServer url=");
        final String url = np.getWordFileNamePunctuation().replace("http://", "").replace("/MeganServer", "").replace("/$", "");
        String user = "";
        if (np.peekMatchIgnoreCase("user")) {
            np.matchIgnoreCase("user=");
            user = np.getWordFileNamePunctuation();
        }
        String password = "";
        if (np.peekMatchIgnoreCase("password")) {
            np.matchIgnoreCase("password=");
            password = np.getWordFileNamePunctuation();
        }
        np.matchIgnoreCase(";");

        RemoteServiceManager.ensureCredentialsHaveBeenLoadedFromProperties();
        if (ProgramProperties.get("SaveRemoteCrendentials", true))
            RemoteServiceManager.saveCredentials(url, user, password);

    }

    public void actionPerformed(ActionEvent event) {
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return null;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Add a MEGAN server to the persistent list of known servers";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return false;
    }
}

