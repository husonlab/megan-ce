/*
 * Copyright (C) 2020. Daniel H. Huson
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

package megan.ms.server;

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.PeakMemoryUsageMonitor;
import jloda.util.ProgramProperties;
import megan.main.Megan6;

import java.io.File;

/**
 * Megan Server program
 * Daniel Huson, 8.2020
 */
public class MeganServer {
    public static String Version = "MeganServer0.1";

    /**
     * * main
     */
    public static void main(String[] args) {
        try {
            Basic.startCollectionStdErr();
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
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
        final ArgsOptions options = new ArgsOptions(args, this, "Serves MEGAN files over the web via HTTP");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2020 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input");
        final String inputDirectory = options.getOptionMandatory("-i", "inputDir", "Input directory", "");
        final String path = options.getOption("-cp", "commandPrefix", "Command path prefix", "megan6server");
        final boolean recursive = options.getOption("-r", "recurse", "Recursively visit all input subdirectories", true);
        final String[] inputFileExtensions = options.getOption("-e", "extensions", "Input file extensions", new String[]{".daa", ".rma", ".rma6", ".megan", ".megan.gz"});

        options.comment("Server");
        final int port = options.getOption("-p", "port", "Server port", 8001);

        final boolean allowGuestLogin = options.getOption("-g", "allowGuest", "Allow guest login (name: guest, pwd: guest)", false);

        options.comment(ArgsOptions.OTHER);
        String defaultPreferenceFile;
        if (ProgramProperties.isMacOS())
            defaultPreferenceFile = System.getProperty("user.home") + "/Library/Preferences/MeganServerUsers.def";
        else
            defaultPreferenceFile = System.getProperty("user.home") + File.separator + ".MeganServerUsers.def";

        final String usersFile = options.getOption("-u", "usersFile", "File containing list of users", defaultPreferenceFile);
        final int backlog = options.getOption("-mb", "maxBacklog", "Maximum number of requests in backlog", 100);
        final int pageTimeout = options.getOption("-pt", "pageTimeout", "Number of seconds to keep unused pages alive", 10000);
        Basic.setDebugMode(options.getOption("-d", "debug", "Debug mode", false));

        options.done();

        final UserManager userManager = new UserManager(usersFile);
        if (!userManager.hasAdmin())
            userManager.askForAdminPassword();

        if (allowGuestLogin) {
            userManager.addUser("guest", "guest", true);
            System.err.println("Guests can login with name: guest and pwd: guest");
        }

        final HttpServerMS server = new HttpServerMS(path, port,userManager, backlog, pageTimeout);
        final Database database = new Database(new File(inputDirectory), inputFileExtensions, recursive);
        server.addDatabase(path,database,null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Stopping http server...");
            server.stop();
            System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
            System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
        }));

        System.err.println("Starting http server...");
        server.start();
        System.err.println(server.getAbout());

        System.err.println("Server address:");
        System.err.println("http://" + server.getAddress().getHostAddress() + ":" + server.getSocketAddress().getPort() + path);
        System.err.println("http://" + server.getAddress().getHostName() + ":" + server.getSocketAddress().getPort() + path);
        System.err.println();

        server.rebuildDatabases();

        Thread.sleep(Long.MAX_VALUE);
    }
}
