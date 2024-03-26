/*
 * Megan6.java Copyright (C) 2024 Daniel H. Huson
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
package megan.main;

import javafx.embed.swing.JFXPanel;
import jloda.fx.util.ResourceManagerFX;
import jloda.swing.commands.CommandManager;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.NodeView;
import jloda.swing.message.MessageWindow;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ProgramProperties;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.About;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.PeakMemoryUsageMonitor;
import jloda.util.StringUtils;
import megan.chart.data.ChartCommandHelper;
import megan.classification.data.ClassificationCommandHelper;
import megan.core.Director;
import megan.util.ClassificationRegistration;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * MEGAN community version
 * Daniel Huson   5.2015
 */
public class Megan6 {
    /**
     * runs MEGAN6
     */
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
                try {
                    if (MessageWindow.getInstance() != null)
                        MessageWindow.getInstance().setVisible(false);
                    System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
                    System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
                }
                catch(Exception ignored){}
        }));
        PeakMemoryUsageMonitor.start();

        try {

            {
				ResourceManager.insertResourceRoot(megan.resources.Resources.class);

				// need to read properties so that registration can add external files directory
				if (ProgramProperties.isMacOS())
					MeganProperties.initializeProperties(System.getProperty("user.home") + "/Library/Preferences/Megan.def");
				else
					MeganProperties.initializeProperties(System.getProperty("user.home") + File.separator + ".Megan.def");

				ClassificationRegistration.register(true);
			}

			ensureInitFXInSwingProgram();
			NotificationsInSwing.setTitle("MEGAN6");

            //run application
            (new Megan6()).parseArguments(args);

        } catch (Throwable th) {
            //catch any exceptions and the like that propagate up to the top level
            if (th.getMessage()==null || !th.getMessage().equals("Help")) {
                System.err.println("MEGAN fatal error:" + "\n" + th);
                Basic.caught(th);
                System.exit(1);
            }
            if (!ArgsOptions.hasMessageWindow())
                System.exit(1);
        }
    }

    public static String[] processOpenFileArgs(String[] args) {
        try {
            if (args.length == 1 && !args[0].startsWith("-")) {// assume this is a single file name or URL
                var fileName = args[0];
                if (fileName.startsWith("megan:"))
                    fileName = StringUtils.convertPercentEncoding(fileName.substring("megan:".length()));
                return new String[]{"-f", fileName};
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return args;
    }

    /**
     * parse command line arguments and launch program
     *
	 */
    private void parseArguments(String[] args) throws Exception {
        Basic.startCollectionStdErr();
        args = processOpenFileArgs(args);

        GraphView.defaultNodeView.setShape(NodeView.OVAL_NODE);

        ResourceManager.insertResourceRoot(megan.resources.Resources.class);
        ResourceManagerFX.addResourceRoot(Megan6.class, "megan.resources");
        CommandManager.getGlobalCommands().addAll(ClassificationCommandHelper.getGlobalCommands());
        CommandManager.getGlobalCommands().addAll(ChartCommandHelper.getChartDrawerCommands());

        ProgramProperties.setProgramName(Version.NAME);
        ProgramProperties.setProgramVersion(Version.SHORT_DESCRIPTION);

        ProgramProperties.setProgramLicence("Copyright (C) 2024 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.\n" +
                                            "This is free software, licensed under the terms of the GNU General Public License, Version 3.\n" +
                                            "Sources available at: https://github.com/husonlab/megan-ce");

        ProgramProperties.setUseGUI(true);

        final var options = new ArgsOptions(args, this, "MEGAN MetaGenome Analyzer Community Edition");
        if (options.isDoHelp()) {
            Basic.restoreSystemErr(System.out); // send system err to system out
            System.err.println(Basic.stopCollectingStdErr());
        }
        options.setAuthors("Daniel H. Huson");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense(ProgramProperties.getProgramLicence());

        final var meganFiles = options.getOption("-f", "files", "MEGAN RMA file(s) to open", new String[0]);

        final var propertiesFile = options.getOption("-p", "propertiesFile", "Properties file",megan.main.Megan6.getDefaultPropertiesFile());

        final var showMessageWindow = !options.getOption("+w", "hideMessageWindow", "Hide message window", false);
        final var silentMode = options.getOption("-S", "silentMode", "Silent mode", false);
        Basic.setDebugMode(options.getOption("-d", "debug", "Debug mode", false));
        options.done();

        if (silentMode) {
            Basic.hideSystemErr();
            Basic.hideSystemOut();
            Basic.stopCollectingStdErr();
        }

        MeganProperties.initializeProperties(propertiesFile);

        final var treeFile = ProgramProperties.get(MeganProperties.TAXONOMYFILE, MeganProperties.DEFAULT_TAXONOMYFILE);

        About.setVersionStringOffset(205,140);
        About.setAbout("megan6.png", ProgramProperties.getProgramVersion(), JDialog.DISPOSE_ON_CLOSE,0.25f);
        About.getAbout().showAbout();

        SwingUtilities.invokeLater(() -> {
            try {
                final var newDir = Director.newProject();
                final var viewer = newDir.getMainViewer();
                viewer.getFrame().setVisible(true);
                if (MessageWindow.getInstance() == null) {
                    MessageWindow.setInstance(new MessageWindow(ProgramProperties.getProgramIcon(), "Messages - MEGAN", viewer.getFrame(), false));
                    MessageWindow.getInstance().getTextArea().setFont(new Font("Monospaced", Font.PLAIN, 12));
                }
                if (showMessageWindow)
                    Director.showMessageWindow();

                Basic.restoreSystemOut(System.err); // send system out to system err
                System.err.println(Basic.stopCollectingStdErr());

                MeganProperties.notifyListChange("RecentFiles");
                newDir.executeOpen(treeFile, meganFiles, null);

            } catch (Exception e) {
                Basic.caught(e);
            }
        });
    }

    public static void ensureInitFXInSwingProgram() {
        final var jframe = new JFrame("Not used");
        jframe.add(new JFXPanel());
    }

    public static String getDefaultPropertiesFile() {
        if (ProgramProperties.isMacOS())
            return System.getProperty("user.home") + "/Library/Preferences/MEGAN.def";
        else
            return System.getProperty("user.home") + File.separator + ".MEGAN.def";
    }
}
