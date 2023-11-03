/*
 * MeganServer.java Copyright (C) 2023 Daniel H. Huson
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

import jloda.fx.util.ProgramExecutorService;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ProgramProperties;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.PeakMemoryUsageMonitor;
import jloda.util.UsageException;
import megan.daa.connector.DAAConnector;
import megan.rma6.RMA6Connector;

import java.io.File;

/**
 * Megan Server program
 * Daniel Huson, 8.2020
 */
public class MeganServer {
    public static final String Version = "MeganServer0.1";

    /**
     * * main
     */
    public static void main(String[] args) {
        try {
            Basic.startCollectionStdErr();
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
            ProgramProperties.setProgramName("MeganServer");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new MeganServer()).run(args);
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run
     */
    private void run(String[] args) throws Exception {
        // we keep references to read blocks, so must not reuse readblocks:
        RMA6Connector.reuseReadBlockInGetter=false;
        DAAConnector.reuseReadBlockInGetter=false;

        final ArgsOptions options = new ArgsOptions(args, this, "Serves MEGAN files over the web via HTTP");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2023. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input");
        final String inputDirectory = options.getOptionMandatory("-i", "input", "Input directory", "");
        final boolean recursive = options.getOption("-r", "recurse", "Recursively visit all input subdirectories", true);
        final String[] inputFileExtensions = options.getOption("-x", "extensions", "Input file extensions", new String[]{".daa", ".rma", ".rma6", ".megan", ".megan.gz"});

        options.comment("Server");
        final String endpoint = options.getOption("-e", "endpoint", "Endpoint name", "megan6server");

        final int port = options.getOption("-p", "port", "Server port", 8001);

        final boolean allowGuestLogin = options.getOption("-g", "allowGuest", "Allow guest login (name: guest, pwd: guest)", false);

        options.comment(ArgsOptions.OTHER);
        String defaultPreferenceFile;
        if (ProgramProperties.isMacOS())
            defaultPreferenceFile = System.getProperty("user.home") + "/Library/Preferences/MeganServerUsers.def";
        else
            defaultPreferenceFile = System.getProperty("user.home") + File.separator + ".MeganServerUsers.def";

        final String usersFile = options.getOption("-u", "usersFile", "File containing list of users", defaultPreferenceFile);
        final int backlog = options.getOption("-bl", "backlog", "Set the socket backlog", 100);
        final int pageTimeout = options.getOption("-pt", "pageTimeout", "Number of seconds to keep pending pages alive", 10000);
        final int readsPerPage=options.getOption("-rpp","readsPerPage","Number of reads per page to serve",100);

        ProgramExecutorService.setNumberOfCoresToUse(options.getOption("-t", "threads", "Number of threads", 8));
        Basic.setDebugMode(options.getOption("-d", "debug", "Debug mode", false));

        options.done();

        if(endpoint.length()==0)
            throw new UsageException("--endpoint: must have positive length");

        final UserManager userManager = new UserManager(usersFile);
        if (!userManager.hasAdmin())
            userManager.askForAdminPassword();

        if (allowGuestLogin) {
            userManager.addUser("guest", "guest", true);
            System.err.println("Guests can login with name: guest and password: guest");
        }

        final HttpServerMS server = new HttpServerMS(endpoint, port,userManager, backlog, readsPerPage,pageTimeout);
        final Database database = new Database(new File(inputDirectory), inputFileExtensions, recursive);
        server.addDatabase(endpoint,database,null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Stopping http server...");
            System.err.println(server.getAbout());
            server.stop();
            System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
            System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
        }));

        System.err.println("Starting http server...");
        server.start();
        System.err.println(server.getAbout());

        System.err.println("Server address:");
        System.err.println("http://" + server.getAddress().getHostAddress() + ":" + server.getSocketAddress().getPort() + "/"+endpoint);
        System.err.println("http://" + server.getAddress().getHostName() + ":" + server.getSocketAddress().getPort() +"/"+ endpoint);
        System.err.println("Help: http://" + server.getAddress().getHostAddress() + ":"+server.getSocketAddress().getPort() + endpoint + "/help");
        System.err.println();

        server.rebuildDatabases();

        Thread.sleep(Long.MAX_VALUE);
    }
}
