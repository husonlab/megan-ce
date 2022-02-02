/*
 * CheckForUpdate.java Copyright (C) 2022 Daniel H. Huson
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

import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.ApplicationDisplayMode;
import com.install4j.api.update.UpdateChecker;
import com.install4j.api.update.UpdateDescriptor;
import com.install4j.api.update.UpdateDescriptorEntry;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.ProgramProperties;

import javax.swing.*;

/**
 * check for update
 * Daniel Huson, 3.2020
 */
public class CheckForUpdate {
    public static final String programURL = "https://software-ab.informatik.uni-tuebingen.de/download/megan6";
	public static final String updaterApplicationId = "1691242905";
    public static String updates = "updates.xml";

    /**
     * check for update, download and install, if present
     */
    public static void apply() {
        final boolean verbose = ProgramProperties.get("verbose-check-for-update", false);
        try {
            final ApplicationDisplayMode applicationDisplayMode = ProgramProperties.isUseGUI() ? ApplicationDisplayMode.GUI : ApplicationDisplayMode.CONSOLE;
            final String url = programURL + "/" + updates;
            if (verbose)
                System.err.println("URL: " + url);

            final UpdateDescriptor updateDescriptor = UpdateChecker.getUpdateDescriptor(url, applicationDisplayMode);
            final UpdateDescriptorEntry possibleUpdate = updateDescriptor.getPossibleUpdateEntry();

            if (verbose)
                System.err.println("Possible update: " + possibleUpdate);
            if (possibleUpdate == null) {
                NotificationsInSwing.showInformation("Installed version is up-to-date");
            } else {
                System.err.println("New version available: " + programURL + "/" + possibleUpdate.getFileName());
                if (ProgramProperties.isUseGUI()) {
                    final Runnable runnable = () -> {
                        if (verbose)
                            System.err.println("Launching : update installer (id=" + updaterApplicationId + ")...");

                        ApplicationLauncher.launchApplicationInProcess(updaterApplicationId, null,
                                new ApplicationLauncher.Callback() {
                                    public void exited(int exitValue) {
                                        if (verbose)
                                            System.err.println("Exit value: " + exitValue);
                                    }

                                    public void prepareShutdown() {
                                        ProgramProperties.store();
                                    }
                                },
                                ApplicationLauncher.WindowMode.FRAME, null);
                    };
                    SwingUtilities.invokeLater(runnable);
                }
            }
        } catch (Exception e) {
            Basic.caught(e);
            NotificationsInSwing.showInformation("Failed to check for updates: " + e);
        }
    }
}
