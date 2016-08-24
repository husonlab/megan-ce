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
package megan.remote.client;

import megan.remote.IRemoteService;
import rusch.megan5client.RMADataset;
import rusch.megan5client.connector.Megan5ServerConnector;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * implements a remote service
 * <p/>
 * Created by huson on 10/3/14.
 */
public class RemoteService implements IRemoteService {
    private final String serverURL; // server URL, e.g. http://localhost:8080 or http://localhost:8080/Megan5Server
    private final String shortName; // local file prefix, e.g. localhost:8080:: localhost:8080/Megan5Server
    private final String info;

    private final List<String> files;

    private final Map<String, String> fileName2Description = new HashMap<>();

    /**
     * constructor
     *
     * @path path to root directory
     */
    public RemoteService(String serverURL, String user, String password) throws IOException {
        serverURL = serverURL.replace("http://", "").replaceAll("/$", "");
        if (!serverURL.contains(("/")))
            serverURL += "/MeganServer";
        this.shortName = serverURL;
        this.serverURL = "http://" + serverURL;

        Megan5ServerConnector connector = new Megan5ServerConnector(this.serverURL, user, password);
        String infoStr = connector.getInfo();
        info = (infoStr == null ? "" : infoStr.trim());

        final RMADataset[] datasets = connector.getAvailiableDatasets();
        files = new LinkedList<>();
        for (RMADataset dataSet : datasets) {
            String name = dataSet.getDatasetName();
            files.add(name);
            String description = dataSet.getDescription();
            if (description == null || description.equals("No description provided")) {
                description = name;
            }
            fileName2Description.put(name, description);

        }
        System.err.println("Node: " + serverURL + ", number of available files: " + getAvailableFiles().size());
    }

    /**
     * get a short name for the server
     *
     * @return short name
     */
    @Override
    public String getShortName() {
        return shortName;
    }

    /**
     * is this node available?
     *
     * @return availability
     */
    @Override
    public boolean isAvailable() {
        return true; // todo: fix
    }

    /**
     * get a list of available files and their unique ids
     *
     * @return list of available files in format path,id
     */
    @Override
    public List<String> getAvailableFiles() {
        return files;
    }

    /**
     * get the server URL
     *
     * @return server URL
     */
    @Override
    public String getServerURL() {
        return serverURL;
    }

    /**
     * gets the server and file name
     *
     * @param file
     * @return server and file
     */
    @Override
    public String getServerAndFileName(String file) {
        return shortName + "::" + file;
    }

    /**
     * gets the info string for a server
     *
     * @return info in html
     */
    @Override
    public String getInfo() {
        return info;
    }

    /**
     * get the description associated with a given file name
     *
     * @param fileName
     * @return description
     */
    public String getDescription(String fileName) {
        return fileName2Description.get(fileName);
    }
}
