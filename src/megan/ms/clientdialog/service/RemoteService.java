/*
 * RemoteService.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.ms.clientdialog.service;

import megan.ms.client.ClientMS;
import megan.ms.clientdialog.IRemoteService;
import megan.ms.server.MeganServer;

import java.io.IOException;
import java.util.*;

/**
 * implements a remote service
 * <p/>
 * Created by huson on 10/3/14, 7.2021
 */
public class RemoteService implements IRemoteService {
    private final String serverURL;
    private final ClientMS clientMS;
    private final Set<String> files=new TreeSet<>();
    private final boolean serverSupportsGetDescription;

    private final String about;

    private final Map<String, String> fileName2Description = new HashMap<>();

    /**
     * constructor
     *
     * @path path to root directory
     */
    public RemoteService(String serverURL, String user, String passwordHash) throws IOException {
        serverURL = serverURL.replaceAll("/$", "");
        if (!serverURL.contains(("/")))
            serverURL += "/megan6server";
        this.serverURL = serverURL;

        clientMS = new ClientMS(this.serverURL, null, 0, user, passwordHash, 100);

        final String remoteVersion = clientMS.getAsString("version");
        if (!remoteVersion.startsWith("MeganServer"))
            throw new IOException("Failed to confirm MeganServer at remote site");
        if (!remoteVersion.equals(MeganServer.Version))
            throw new IOException("Incompatible version numbers: client=" + MeganServer.Version + " server=" + remoteVersion);

        about = clientMS.getAsString("about");

        serverSupportsGetDescription =clientMS.getAsString("help").contains("getDescription");

        System.err.println(about);

        var directories=new HashSet<String>();
        directories.add(".");
        for(var fileRecord:clientMS.getFileRecords()) {
            var name=fileRecord.getName();
            files.add(name);
            if(serverSupportsGetDescription) {
                try {
                    var description = clientMS.getAsString("getDescription?file=" + fileRecord.getName());
                    if (!description.isBlank())
                        fileRecord.setDescription(description);
                } catch (IOException ignored) {
                }
            }
            fileName2Description.put(name, fileRecord.getDescription());
            if(name.contains("/"))
                directories.add(name.substring(0,name.lastIndexOf("/")));
        }
        if(serverSupportsGetDescription) {
            for (var directory : directories) {
                fileName2Description.put(directory, directory);
                try {
                    var description = clientMS.getAsString("getDescription?file=" + directory);
                    if (!description.isBlank())
                        fileName2Description.put(directory, description);
                } catch (IOException ignored) {
                }
            }
        }
        System.err.println("Server: " + serverURL + ", number of available files: " + getAvailableFiles().size());
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
    public Collection<String> getAvailableFiles() {
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
        return serverURL + "::" + file;
    }

    /**
     * gets the info string for a server
     *
     * @return info in html
     */
    @Override
    public String getInfo() {
        try {
            return clientMS.getAsString("about");
        } catch (IOException ignored) {
        }
        return "";
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

    public ClientMS getClientMS() {
        return clientMS;
    }

    public String getAbout() {
        return about;
    }
}


