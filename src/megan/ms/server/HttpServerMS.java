package megan.ms.server;

import com.sun.net.httpserver.HttpServer;
import jloda.util.ProgramProperties;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * HTTP server for megan server
 * Daniel Huson, 8.2020
 */
public class HttpServerMS {
    private final InetAddress address;
    private final HttpServer httpServer;
    private final String commandPrefix;
    private final Database database;
    private final UserManager userManager;
    private long started = 0L;

    public HttpServerMS(int port, String prefix, UserManager userManager, Database database, int backlog, int pageTimeout) throws IOException {
        this.commandPrefix = prefix;
        this.userManager=userManager;
        this.database = database;

        if (prefix.length() > 0 && !prefix.startsWith("/"))
            prefix = "/" + prefix;

        ReadIteratorPagination.setTimeoutSeconds(pageTimeout);

        address = InetAddress.getLocalHost();
        httpServer = HttpServer.create(new InetSocketAddress((InetAddress) null, port), backlog);

        final var authenticator = userManager.getAuthenticator();
        final var adminAuthenicator = userManager.getAdminAuthenticator();

        // general info:
        httpServer.createContext(prefix + "/help", new HttpHandlerMS(RequestHandler.getHelp())); // .setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/version", new HttpHandlerMS(RequestHandler.getVersion())).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/about", new HttpHandlerMS(RequestHandler.getAbout(this))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/isReadOnly", new HttpHandlerMS((RequestHandler)(c, p) -> "true".getBytes())).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/list", new HttpHandlerMS(RequestHandler.getListDatasets(database))).setAuthenticator(authenticator);

        // file info:
        httpServer.createContext(prefix + "/getFileUid", new HttpHandlerMS(RequestHandler.getFileUid(database))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getAuxiliary", new HttpHandlerMS(RequestHandler.getAuxiliaryData(database))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getNumberOfReads", new HttpHandlerMS(RequestHandler.getNumberOfReads(database))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getNumberOfMatches", new HttpHandlerMS(RequestHandler.getNumberOfMatches(database))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getClassificationNames", new HttpHandlerMS(RequestHandler.getClassifications(database))).setAuthenticator(authenticator);

        // access reads and matches
        httpServer.createContext(prefix + "/getRead", new HttpHandlerMS(RequestHandler.getRead(database))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getReads", new HttpHandlerMS(RequestHandler.getReads(database))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getReadsForClass", new HttpHandlerMS(RequestHandler.getReadsForMultipleClassIdsIterator(database))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getFindAllReadsIterator", new HttpHandlerMS()).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getNext", new HttpHandlerMS(RequestHandler.getNextPage(database))).setAuthenticator(authenticator);

        // access classifications
        httpServer.createContext(prefix + "/getClassificationBlock", new HttpHandlerMS(RequestHandler.getClassificationBlock(database))).setAuthenticator(authenticator);
        httpServer.createContext(prefix + "/getClassSize", new HttpHandlerMS(RequestHandler.getClassSize(database))).setAuthenticator(authenticator);

        // download a file
        httpServer.createContext(prefix + "/download", RequestHandlerAdditional.getDownloadPageHandler(database)).setAuthenticator(authenticator);

        // admin commands:
        httpServer.createContext(prefix + "/admin/listUsers", new HttpHandlerMS(RequestHandlerAdmin.listUsers(userManager))).setAuthenticator(adminAuthenicator);
        httpServer.createContext(prefix + "/admin/updateDatasets", new HttpHandlerMS(RequestHandlerAdmin.recompute(database))).setAuthenticator(adminAuthenicator);
        httpServer.createContext(prefix + "/admin/addUser", new HttpHandlerMS(RequestHandlerAdmin.addUser(userManager))).setAuthenticator(adminAuthenicator);
        httpServer.createContext(prefix + "/admin/removeUser", new HttpHandlerMS(RequestHandlerAdmin.removeUser(userManager))).setAuthenticator(adminAuthenicator);
        httpServer.createContext(prefix + "/admin/getLog", new HttpHandlerMS(RequestHandlerAdmin.getLog())).setAuthenticator(adminAuthenicator);
        httpServer.createContext(prefix + "/admin/clearLog", new HttpHandlerMS(RequestHandlerAdmin.clearLog())).setAuthenticator(adminAuthenicator);

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        httpServer.setExecutor(threadPoolExecutor);
    }

    public Database getDatabase() {
        return database;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public long getStarted() {
        return started;
    }

    public void start() {
        started = System.currentTimeMillis();
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(10);
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

    public String getAbout() {
        String about = ProgramProperties.getProgramName() + "\n"
                + "Version: " + ProgramProperties.getProgramVersion() + "\n"
                + "Serving: " + database.getInfo() + "\n"
                + "Hostname: " + getAddress().getHostName() + "\n"
                + "IP address: " + getAddress().getHostAddress() + "\n"
                + "Port: " + getSocketAddress().getPort() + "\n"
                + "Known users: " + userManager.size() + "\n"
                + "Total requests: " + (HttpHandlerMS.getNumberOfRequests().get() + 1L) + "\n"
                + "Server started: " + (new Date(getStarted())) + "\n";

        if (database.getLastRebuild() > 0 )
            about += "Latest rebuild: " + new Date(database.getLastRebuild()) + "\n";

        about += "Help: http://" + getAddress().getHostAddress() + ":8001" + getCommandPrefix() + "/help\n";
        return about;
    }}
