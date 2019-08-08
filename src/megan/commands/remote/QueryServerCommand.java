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
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Pair;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.remote.client.RemoteServiceManager;
import rusch.megan5client.RMADataset;
import rusch.megan5client.connector.Megan5ServerConnector;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class QueryServerCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "queryServer url=<url> query={countFiles|listFiles};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("queryServer url=");
        final String url = np.getWordFileNamePunctuation().replace("http://", "").replace("/MeganServer", "").replace("/$", "");
        np.matchIgnoreCase("query=");
        String query = np.getWordMatchesIgnoringCase("numberOfFiles listFiles");
        np.matchIgnoreCase(";");

        Pair<String, String> credentials = RemoteServiceManager.getCredentials(url);

        if (credentials == null) {
            NotificationsInSwing.showError(getViewer().getFrame(), "Server unknown to MEGAN (please first add it): " + url);
        } else {
            final String fullURL = RemoteServiceManager.getServerURL(url);
            final Megan5ServerConnector connector = new Megan5ServerConnector(fullURL, credentials.get1(), credentials.get2());
            if (query.equalsIgnoreCase("countFiles")) {
                Director.showMessageWindow();
                System.out.println("Server=" + url + ", number of files=" + connector.getAvailiableDatasets().length);
            } else if (query.equalsIgnoreCase("listFiles")) {
                Director.showMessageWindow();
                System.out.println("Server=" + url + ", available files:\n");
                RMADataset[] datasets = connector.getAvailiableDatasets();
                for (RMADataset dataset : datasets) {
                    System.out.println(dataset.getDatasetName());
                }
            }
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
        return "Query a known server";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return false;
    }
}

