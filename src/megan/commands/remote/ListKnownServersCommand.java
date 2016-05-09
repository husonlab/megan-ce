/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.gui.commands.ICommand;
import jloda.util.Pair;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.remote.client.RemoteServiceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ListKnownServersCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "listServers;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("listServers;");
        Director.showMessageWindow();
        System.err.println("Known servers (total=" + RemoteServiceManager.getServers().size() + "):");
        for (String server : RemoteServiceManager.getServers()) {
            Pair<String, String> pair = RemoteServiceManager.getCredentials(server);
            System.out.println(server + "\t" + pair.get1() + "\t" + pair.get2());
        }
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
        return "List all added servers";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return false;
    }
}

