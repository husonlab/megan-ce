/*
 * RemoteServiceManager.java Copyright (C) 2024 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.ms.clientdialog.service;

import jloda.swing.util.ProgramProperties;
import jloda.util.Pair;
import megan.ms.Utilities;
import megan.ms.clientdialog.IRemoteService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * remote service manager
 * <p/>
 * Daniel Huson, 2014, 10.2020
 */
public class RemoteServiceManager {
    public static final String LOCAL = "Local::";

    public static final String DEFAULT_MEGAN_SERVER = "http://maira.cs.uni-tuebingen.de:8001/megan6server";

    private static final Map<String, IRemoteService> url2node = new HashMap<>();

    private static final Map<String, Pair<String, String>> server2Credentials = new HashMap<>();
    private static boolean loaded = false; // have the credentials been loaded from the properties file?

    /**
     * create a new node
     *
     * @return node or null
	 */
    public static IRemoteService createService(String remoteURL, String user, String password) throws IOException {
        final IRemoteService clientNode;
        if (remoteURL.startsWith(LOCAL)) {
            clientNode = new LocalService(remoteURL.replaceAll(LOCAL, ""), ".rma", ".rma6", ".daa", ".megan", "megan.gz");
        } else
            clientNode = new RemoteService(remoteURL, user, password);
        if (url2node.containsKey(clientNode.getServerURL()))
            System.err.println("Server is already open: " + clientNode.getServerURL());
        url2node.put(clientNode.getServerURL(), clientNode);
        if (ProgramProperties.get("SaveRemoteCredentials", true))
            saveCredentials(clientNode.getServerURL(), user, password);
        return clientNode;
    }

    /**
     * remove the node
     *
	 */
    public static void removeNode(String url) {
        url2node.remove(url);
    }

    /**
     * does  node for this URL exist
     *
     * @return true, if node for URL exists
     */
    public static boolean hasNode(String url) {
        return url2node.containsKey(url);
    }

    /**
     * get the client node
     *
     * @return node
     */
    public static IRemoteService get(String url) {
        return url2node.get(url);
    }

    /**
     * get the server URL
     *
     * @return full file URL as required by connector
     */
    public static String getServerURL(String serverFileName) {
        var pos = serverFileName.indexOf(("::"));
        if (pos > 0)
            return serverFileName.substring(0, pos);
        else
            return serverFileName;
    }

    /**
     * get the remote file path
     *
     * @return remote file path
     */
    public static String getFilePath(String serverFileName) {
        if (isRemoteFile(serverFileName)) {
            var pos = serverFileName.indexOf("::");
            return serverFileName.substring(pos + "::".length());
        } else
            return null;
    }

    /**
     * get the remote user
     *
     * @return remote user
     */
    public static String getUser(String serviceName) {
        var credentials = getCredentials(serviceName);
        if (credentials != null)
            return credentials.getFirst();
        else
            return "guest";
    }

    /**
     * get remote password
     *
     * @return password
     */
    public static String getPasswordHash(String serviceName) {
        final Pair<String, String> credentials = getCredentials(serviceName);
        if (credentials != null)
            return credentials.getSecond();
        else
            return "guest";
    }

    /**
     * does this file name have the syntax of a remote file?
     *
     * @return true, if local name of a remote file
     */
    private static boolean isRemoteFile(String fileName) {
        return !fileName.startsWith(LOCAL) && fileName.contains("::");
    }

    /**
     * get credentials for given server, if known
     *
     * @return credentials
     */
    public static Pair<String, String> getCredentials(String serviceName) {
        return server2Credentials.get(serviceName);
    }

    /**
     * save credentials for given server
     *
	 */
    public static void saveCredentials(String serviceName, String user, String passwordHash) {
        server2Credentials.put(serviceName, new Pair<>(user, passwordHash));
        saveCredentialsToProperties();
    }

    /**
     * load all saved credentials from properties
     */
    public static void ensureCredentialsHaveBeenLoadedFromProperties() {
        if (!loaded) {
            final var credentials = ProgramProperties.get("MeganServers", new String[0]);
            for (var line : credentials) {
                var tokens = line.split("::");
                if (tokens.length > 0) {
                    server2Credentials.put(tokens[0], new Pair<>(tokens.length > 1 ? tokens[1] : "", (tokens.length > 2 ? tokens[2] : "")));
                }
            }
            loaded = true;
        }
    }

    /**
     * save all credentials to properties
     */
    private static void saveCredentialsToProperties() {
        final var list = new LinkedList<String>();

        var toDelete=new ArrayList<String>();
        for (var server : server2Credentials.keySet()) {
            if (server.toLowerCase().contains("informatik.uni-tuebingen.de")) {
                System.err.println("Removed defunct server address: " + server);
                toDelete.add(server);
            }
        }
        toDelete.forEach(server2Credentials.keySet()::remove);

        for (var server : server2Credentials.keySet()) {
                var pair = server2Credentials.get(server);
                list.add(server + "::" + pair.getFirst() + "::" + pair.getSecond());
        }
        ProgramProperties.put("MeganServers", list.toArray(new String[0]));
    }

    /**
     * remove credentials for given URL
     *
	 */
    public static void removeCredentials(String url) {
        server2Credentials.remove(url);
    }

    public static Collection<String> getServers() {
        ensureCredentialsHaveBeenLoadedFromProperties();
        return server2Credentials.keySet();
    }

    public static void ensureDefaultService() {
        var user = "guest";
        var passwordHash = Utilities.computeBCryptHash("guest".getBytes());
        saveCredentials(DEFAULT_MEGAN_SERVER, user, passwordHash);
    }
}
