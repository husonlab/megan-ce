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
package megan.remote.client;

import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.remote.IRemoteService;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.util.*;

/**
 * remote service manager
 * <p/>
 * Created by huson on 10/3/14.
 */
public class RemoteServiceManager {
    private static final String LOCAL = "Local::";

    private static final String DEFAULT_MEGAN_SERVER = "meganserver2.informatik.uni-tuebingen.de/Public";

    private static final Map<String, IRemoteService> url2node = new HashMap<>();

    private static final Map<String, Pair<String, String>> server2Credentials = new HashMap<>();
    private static boolean loaded = false; // have the credentials been loaded from the properties file?

    /**
     * create a new node
     *
     * @param remoteURL
     * @return node or null
     * @throws java.io.IOException
     */
    public static IRemoteService createService(String remoteURL, String user, String password) throws IOException {
        final IRemoteService clientNode;
        if (remoteURL.startsWith(LOCAL)) {
            clientNode = new LocalService(remoteURL.replaceAll(LOCAL, ""));
        } else
            clientNode = new RemoteService(remoteURL, user, password);
        if (url2node.containsKey(clientNode.getShortName()))
            System.err.println("Warning: node already exists: " + clientNode.getShortName());
        url2node.put(clientNode.getShortName(), clientNode);
        if (ProgramProperties.get("SaveRemoteCrendentials", true))
            saveCredentials(clientNode.getShortName(), user, password);
        return clientNode;
    }

    /**
     * remove the node
     *
     * @param url
     */
    public static void removeNode(String url) {
        url2node.remove(url);
    }

    /**
     * does  node for this URL exist
     *
     * @param url
     * @return true, if node for URL exists
     */
    public static boolean hasNode(String url) {
        return url2node.containsKey(url);
    }

    /**
     * get the client node
     *
     * @param url
     * @return node
     */
    public static IRemoteService get(String url) {
        return url2node.get(url);
    }

    /**
     * get the short server name associated with this local file name
     *
     * @param localFileName
     * @return short name
     */
    private static String getServerShortName(String localFileName) {
        int pos = localFileName.indexOf(("::"));
        if (pos > 0)
            return localFileName.substring(0, pos);
        else
            return localFileName;
    }

    /**
     * get the server URL
     *
     * @param localFileName
     * @return full file URL as required by connector
     */
    public static String getServerURL(String localFileName) {
        ensureCredentialsHaveBeenLoadedFromProperties();
        String shortName = getServerShortName(localFileName);
        return "http://" + shortName;
    }

    /**
     * get the remote file path
     *
     * @param localFileName
     * @return remote file path
     */
    public static String getFilePath(String localFileName) {
        if (isRemoteFile(localFileName)) {
            int pos = localFileName.indexOf("::");
            return localFileName.substring(pos + "::".length());
        } else
            return null;
    }

    /**
     * get the remote user
     *
     * @param localFileName
     * @return remote user
     */
    public static String getUser(String localFileName) {
        localFileName = localFileName.replaceAll("/MeganServer$", "");

        if (isRemoteFile(localFileName)) {
            final Pair<String, String> credentials = getCredentials(getServerShortName(localFileName));
            if (credentials != null)
                return credentials.get1();
        }
        return null;
    }

    /**
     * get remote password
     *
     * @param localFileName
     * @return password
     */
    public static String getPassword(String localFileName) {
        localFileName = localFileName.replaceAll("/MeganServer$", "");

        if (isRemoteFile(localFileName)) {
            final Pair<String, String> credentials = getCredentials(getServerShortName(localFileName));
            if (credentials != null)
                return credentials.get2();
        }
        return null;
    }

    /**
     * does this file name have the syntax of a remote file?
     *
     * @param localFileName
     * @return true, if local name of a remote file
     */
    private static boolean isRemoteFile(String localFileName) {
        return localFileName.contains("::");
    }

    /**
     * get credentials for given server, if known
     *
     * @param server
     * @return credentials
     */
    public static Pair<String, String> getCredentials(String server) {
        server = server.replaceAll("/MeganServer$", "");
        return server2Credentials.get(server);
    }

    /**
     * save credentials for given server
     *
     * @param server
     * @param user
     * @param password
     */
    public static void saveCredentials(String server, String user, String password) {
        server = server.replaceAll("/MeganServer$", "");

        server2Credentials.put(server, new Pair<>(user, password));
        saveCredentialsToProperties();
    }

    /**
     * load all saved credentials from properties
     */
    public static void ensureCredentialsHaveBeenLoadedFromProperties() {
        if (!loaded) {
            final String[] credentials = ProgramProperties.get("MeganServerCredentials", new String[0]);
            for (String line : credentials) {
                String[] tokens = line.split("::");
                if (tokens.length > 0) {
                    server2Credentials.put(tokens[0],
                            new Pair<>(tokens.length > 1 ? tokens[1] : "",
                                    Basic.toString(Base64.decodeBase64((tokens.length > 2 ? tokens[2] : "")))));
                }
            }
            loaded = true;
        }
    }

    /**
     * save all credentials to properties
     */
    private static void saveCredentialsToProperties() {
        List<String> list = new LinkedList<>();

        for (String server : server2Credentials.keySet()) {
            Pair<String, String> pair = server2Credentials.get(server);
            String user = pair.get1();
            String encodedPassword = Base64.encodeBase64String(pair.get2().getBytes());
            list.add(server + "::" + user + "::" + encodedPassword);
        }
        ProgramProperties.put("MeganServerCredentials", list.toArray(new String[0]));
    }

    /**
     * remove credentials for given URL
     *
     * @param url
     */
    public static void removeCredentials(String url) {
        server2Credentials.remove(url);
    }

    public static Collection<String> getServers() {
        ensureCredentialsHaveBeenLoadedFromProperties();
        return server2Credentials.keySet();
    }

    public static void setupDefaultService() {
        final String remoteServices = ProgramProperties.get("RemoteServers", "");
        if (!remoteServices.contains(DEFAULT_MEGAN_SERVER)) {
            final String user = "guest";
            final String encodedPassword = Base64.encodeBase64String("guest".getBytes());

            if (remoteServices.length() == 0)
                ProgramProperties.put("RemoteServers", DEFAULT_MEGAN_SERVER);
            else
                ProgramProperties.put("RemoteServers", remoteServices + "%%%" + DEFAULT_MEGAN_SERVER);

            final List<String> credentials = new LinkedList<>(Arrays.asList(ProgramProperties.get("MeganServerCredentials", new String[0])));
            credentials.add(DEFAULT_MEGAN_SERVER + "::" + user + "::" + encodedPassword);
            ProgramProperties.put("MeganServerCredentials", credentials.toArray(new String[0]));
        }
    }
}
