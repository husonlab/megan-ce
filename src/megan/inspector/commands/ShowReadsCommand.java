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
package megan.inspector.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.ProgressListener;
import jloda.util.Single;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.core.Director;
import megan.core.Document;
import megan.data.FindSelection;
import megan.data.IReadBlockIterator;
import megan.inspector.InspectorWindow;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * find reads and display in inspector window
 * Daniel Huson, 11.2010
 */
public class ShowReadsCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show read=");
        String regExpression = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        final InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        ProgramProperties.put(MeganProperties.FINDREAD, regExpression);

        final Document doc = ((Director) getDir()).getDocument();

        final FindSelection findSelection = new FindSelection();
        findSelection.useReadName = true;

        final ProgressListener progress = doc.getProgressListener();
        progress.setTasks("Searching for reads", "");
        progress.setProgress(0);
        final Single<Boolean> canceled = new Single<>(false);
        final Single<Integer> matchesFound = new Single<>(0);
        try (IReadBlockIterator it = doc.getConnector().getFindAllReadsIterator(regExpression, findSelection, canceled)) {
            progress.setMaximum(it.getMaximumProgress());
            final ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.submit(() -> {
                try {
                    while (!canceled.get()) {
                        progress.setProgress(it.getProgress());
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (CanceledException ex) {
                    System.err.println("USER CANCELED EXECUTE");
                } finally {
                    canceled.set(true);
                    executor.shutdownNow();
                }
            });

            try {
                while (it.hasNext()) {
                    inspectorWindow.addTopLevelReadNode(it.next(), Classification.Taxonomy);
                    matchesFound.set(matchesFound.get() + 1);
                    progress.setSubtask(matchesFound.get() + " found");
                }
            } finally {
                canceled.set(true);
            }
        }

        System.err.println("Found: " + matchesFound.get());
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "show read=<regex>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        String regularExpression = ProgramProperties.get(MeganProperties.FINDREAD, "");
        regularExpression = JOptionPane.showInputDialog(inspectorWindow.getFrame(), "Enter regular expression for read names:", regularExpression);
        if (regularExpression != null && regularExpression.trim().length() != 0) {
            regularExpression = regularExpression.trim();
            ProgramProperties.put(MeganProperties.FINDREAD, regularExpression);
            execute("show read='" + regularExpression + "';");
        }
    }

    private static final String NAME = "Show Reads...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show all reads whose names match the given regular expression";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        return inspectorWindow != null;
    }
}
