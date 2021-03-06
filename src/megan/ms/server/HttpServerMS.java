package megan.ms.server;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import jloda.fx.util.ProgramExecutorService;
import jloda.util.Basic;
import jloda.util.ProgramProperties;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
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

        final var authenticator = userManager.createAuthenticator(null);
        final var adminAuthenticator = userManager.createAuthenticator(UserManager.ADMIN);

        // general info:
        createContext(path + "/help", new HttpHandlerMS(RequestHandler.getHelp()),null); // .setAuthenticator(authenticator);
        createContext(path + "/version", new HttpHandlerMS(RequestHandler.getVersion()),authenticator);

        // admin commands:
        createContext(path + "/admin/update", new HttpHandlerMS(RequestHandlerAdmin.update(path2database.values())),adminAuthenticator);
        createContext(path + "/admin/listUsers", new HttpHandlerMS(RequestHandlerAdmin.listUsers(userManager)),adminAuthenticator);
        createContext(path + "/admin/addUser", new HttpHandlerMS(RequestHandlerAdmin.addUser(userManager)),adminAuthenticator);
        createContext(path + "/admin/removeUser", new HttpHandlerMS(RequestHandlerAdmin.removeUser(userManager)),adminAuthenticator);
        createContext(path + "/admin/addRole", new HttpHandlerMS(RequestHandlerAdmin.addRole(userManager)),adminAuthenticator);
        createContext(path + "/admin/removeRole", new HttpHandlerMS(RequestHandlerAdmin.removeRole(userManager)),adminAuthenticator);
        createContext(path + "/admin/getLog", new HttpHandlerMS(RequestHandlerAdmin.getLog()),adminAuthenticator);
        createContext(path + "/admin/clearLog", new HttpHandlerMS(RequestHandlerAdmin.clearLog()),adminAuthenticator);

        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(ProgramExecutorService.getNumberOfCoresToUse());
        httpServer.setExecutor(threadPoolExecutor);
    }

    public void addDatabase(String path,  Database database, String role)  {
        if(!path.startsWith("/"))
            path="/"+path;

        path2database.put(path,database);

        final var authenticator = userManager.createAuthenticator(role);

        // general info:
         createContext(path + "/about", new HttpHandlerMS(RequestHandler.getAbout(this)),authenticator);
        createContext(path + "/version", new HttpHandlerMS(RequestHandler.getVersion()),authenticator);
        createContext(path + "/isReadOnly", new HttpHandlerMS( (c, p) -> "true".getBytes()),authenticator);
        createContext(path + "/list", new HttpHandlerMS(RequestHandler.getListDataset(database)),authenticator);

        // file info:
        createContext(path + "/getFileUid", new HttpHandlerMS(RequestHandler.getFileUid(database)),authenticator);
        createContext(path + "/getAuxiliary", new HttpHandlerMS(RequestHandler.getAuxiliaryData(database)),authenticator);
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
        String about = MeganServer.Version + "\n"
                + "Version: " + ProgramProperties.getProgramVersion() + "\n"
                + "Hostname: " + getAddress().getHostName() + "\n"
                + "IP address: " + getAddress().getHostAddress() + "\n"
                + "Port: " + getSocketAddress().getPort() + "\n"
                + "Known users: " + userManager.size() + "\n"
                + "Total requests: " + (HttpHandlerMS.getNumberOfRequests().get() + 1L) + "\n"
                + "Server started: " + (new Date(getStarted())) + "\n";
        about += "Help: http://" + getAddress().getHostAddress() + ":"+getSocketAddress().getPort() + defaultPath + "/help\n";
        return about;
    }
}
