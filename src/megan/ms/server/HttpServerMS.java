/*
 * HttpServerMS.java Copyright (C) 2023 Daniel H. Huson
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

package megan.ms.server;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import jloda.fx.util.ProgramExecutorService;
import jloda.swing.util.ProgramProperties;
import jloda.util.Basic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * HTTP server for megan server
 * Daniel Huson, 8.2020
 */
public class HttpServerMS {
    private final InetAddress address;
    private final HttpServer httpServer;
    private final Map<String,Database> path2database =new TreeMap<>();
    private final UserManager userManager;
    private final String defaultPath;
    private long started = 0L;
    private final int readsPerPage;

    private final ArrayList<HttpContext> contexts=new ArrayList<>();

    public HttpServerMS(String path, int port, UserManager userManager, int backlog, int readsPerPage, int pageTimeout) throws IOException {
        if(!path.startsWith("/"))
            path="/"+path;

        this.defaultPath =path;
        this.userManager = userManager;
        this.readsPerPage = readsPerPage;
        ReadIteratorPagination.setTimeoutSeconds(pageTimeout);

        address = InetAddress.getLocalHost();
        httpServer = HttpServer.create(new InetSocketAddress((InetAddress) null, port), backlog);

        final var adminAuthenticator = userManager.createAuthenticator(UserManager.ADMIN);

         // admin commands:
        createContext(path + "/admin/update", new HttpHandlerMS(RequestHandlerAdmin.update(path2database.values())),adminAuthenticator);
        createContext(path + "/admin/listUsers", new HttpHandlerMS(RequestHandlerAdmin.listUsers(userManager)),adminAuthenticator);
        createContext(path + "/admin/addUser", new HttpHandlerMS(RequestHandlerAdmin.addUser(userManager)),adminAuthenticator);
        createContext(path + "/admin/removeUser", new HttpHandlerMS(RequestHandlerAdmin.removeUser(userManager)),adminAuthenticator);
        createContext(path + "/admin/addRole", new HttpHandlerMS(RequestHandlerAdmin.addRole(userManager)),adminAuthenticator);
        createContext(path + "/admin/removeRole", new HttpHandlerMS(RequestHandlerAdmin.removeRole(userManager)),adminAuthenticator);
        createContext(path + "/admin/getLog", new HttpHandlerMS(RequestHandlerAdmin.getLog()),adminAuthenticator);
        createContext(path + "/admin/clearLog", new HttpHandlerMS(RequestHandlerAdmin.clearLog()),adminAuthenticator);
        createContext(path + "/admin/shutdown", new HttpHandlerMS(RequestHandlerAdmin.shutdown()),adminAuthenticator);

        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(ProgramExecutorService.getNumberOfCoresToUse());
        httpServer.setExecutor(threadPoolExecutor);
    }

    public void addDatabase(String path,  Database database, String role)  {
        if(!path.startsWith("/"))
            path="/"+path;

        path2database.put(path,database);

        // general info:
        var url="http://" + getAddress().getHostAddress() + ":" + getSocketAddress().getPort() +path;
        createContext(path + "/help", new HttpHandlerMS(RequestHandler.getHelp(url)),null); // .setAuthenticator(authenticator);


        final var authenticator = userManager.createAuthenticator(role);

        // general info:
        createContext(path + "/version", new HttpHandlerMS(RequestHandler.getVersion()),authenticator);
        createContext(path + "/about", new HttpHandlerMS(RequestHandler.getAbout(this)),authenticator);
        createContext(path + "/isReadOnly", new HttpHandlerMS( (c, p) -> "true".getBytes()),authenticator);
        createContext(path + "/list", new HttpHandlerMS(RequestHandler.getListDataset(database)),authenticator);

        // file info:
        createContext(path + "/getFileUid", new HttpHandlerMS(RequestHandler.getFileUid(database)),authenticator);
        createContext(path + "/getAuxiliary", new HttpHandlerMS(RequestHandler.getAuxiliaryData(database)),authenticator);
        createContext(path + "/getNumberOfReads", new HttpHandlerMS(RequestHandler.getNumberOfReads(database)),authenticator);
        createContext(path + "/getNumberOfMatches", new HttpHandlerMS(RequestHandler.getNumberOfMatches(database)),authenticator);
        // for backward compatibility:
        createContext(path + "/numberOfReads", new HttpHandlerMS(RequestHandler.getNumberOfReads(database)),authenticator);
        createContext(path + "/numberOfMatches", new HttpHandlerMS(RequestHandler.getNumberOfMatches(database)),authenticator);

        createContext(path + "/getClassificationNames", new HttpHandlerMS(RequestHandler.getClassifications(database)),authenticator);

        // access reads and matches
        createContext(path + "/getRead", new HttpHandlerMS(RequestHandler.getRead(database)),authenticator);
        createContext(path + "/getReads", new HttpHandlerMS(RequestHandler.getReads(database, getReadsPerPage())),authenticator);
        createContext(path + "/getReadsForClass", new HttpHandlerMS(RequestHandler.getReadsForMultipleClassIdsIterator(database, getReadsPerPage())),authenticator);
        createContext(path + "/getFindAllReadsIterator", new HttpHandlerMS(),authenticator);
        createContext(path + "/getNext", new HttpHandlerMS(RequestHandler.getNextPage(database)),authenticator);

        // access classifications
        createContext(path + "/getClassificationBlock", new HttpHandlerMS(RequestHandler.getClassificationBlock(database)),authenticator);
        createContext(path + "/getClassSize", new HttpHandlerMS(RequestHandler.getClassSize(database)),authenticator);


        // download a file
        createContext(path + "/download", RequestHandlerAdditional.getDownloadPageHandler(database),authenticator);
        createContext(path + "/getDescription", new HttpHandlerMS(RequestHandler.getDescription(database)),authenticator);
    }

    private void createContext(String path, HttpHandlerMS handler, BasicAuthenticator authenticator) {
        final HttpContext context=httpServer.createContext(path,handler);
        if(authenticator!=null)
            context.setAuthenticator(authenticator);
        contexts.add(context);
    }

    public ArrayList<HttpContext> getContexts() {
        return contexts;
    }

    public void rebuildDatabases() {
        try {
            userManager.readFile();
        } catch (IOException e) {
            Basic.caught(e);
        }
        for(var database:path2database.values())
            database.rebuild();
    }

    public long getStarted() {
        return started;
    }

    public void start() {
        started = System.currentTimeMillis();
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(1);
    }

    public InetAddress getAddress() {
        return address;
    }

    public InetSocketAddress getSocketAddress() {
        return httpServer.getAddress();
    }

    public HttpServer getHttpServer() {
        return httpServer;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public Map<String,Database> getPath2Database() {
        return path2database;
    }

    public int getReadsPerPage() {
        return readsPerPage;
    }

    public String getAbout() {
		return MeganServer.Version + "\n"
			   + "Version: " + ProgramProperties.getProgramVersion() + "\n"
			   + "Hostname: " + getAddress().getHostName() + "\n"
			   + "IP address: " + getAddress().getHostAddress() + "\n"
			   + "Port: " + getSocketAddress().getPort() + "\n"
			   + "Known users: " + userManager.size() + "\n"
			   + "Total requests: " + (HttpHandlerMS.getNumberOfRequests().get() + 1L) + "\n"
			   + "Server started: " + (new Date(getStarted())) + "\n";
    }
}
