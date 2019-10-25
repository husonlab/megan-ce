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

package megan.commands.additional;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ProgressDialog;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import megan.blastclient.BlastService;
import megan.blastclient.RemoteBlastClient;
import megan.blastclient.RemoteBlastDialog;
import megan.core.Director;
import megan.core.Document;
import megan.importblast.ImportBlastDialog;
import megan.util.IReadsProvider;
import megan.util.MeganFileFilter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * analyse a sequence on NCBI
 * Daniel Huson, 3/2017
 */
public class RunBlastOnNCBICommand extends CommandBase implements ICommand {
    private static BlastService blastService;
    private static boolean serviceIsRunning = false;

    public String getSyntax() {
        return "remoteBlastNCBI readsFile=<file-name> [longReads={false|true}] [blastMode={blastn|blastx|blastp}] [blastDB={nr|<name>}];";
    }

    /**
     * apply the command
     *
     * @param np
     * @throws Exception
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("remoteBlastNCBI readsFile=");
        final File readsFile = new File(np.getWordFileNamePunctuation());
        final boolean longReads;
        if (np.peekMatchIgnoreCase("longReads")) {
            np.matchIgnoreCase("longReads=");
            longReads = np.getBoolean();
        } else
            longReads = false;

        final String blastMode;
        if (np.peekMatchIgnoreCase("blastMode")) {
            np.matchIgnoreCase("blastMode=");
            blastMode = np.getWordMatchesIgnoringCase(Basic.toString(RemoteBlastClient.BlastProgram.values(), " "));
        } else
            blastMode = "blastn";
        final String blastDB;
        if (np.peekMatchIgnoreCase("blastDB")) {
            np.matchIgnoreCase("blastDB=");
            blastDB = np.getWordMatchesIgnoringCase(Basic.toString(RemoteBlastClient.getDatabaseNames(blastMode), " "));
        } else
            blastDB = "nr";
        np.matchIgnoreCase(";");

        getDir().notifyLockInput();

        if (blastService == null) {
            blastService = new BlastService();
        }

        if (serviceIsRunning && ProgramProperties.isUseGUI()) {
            if (JOptionPane.showConfirmDialog(getViewer().getFrame(), "A remote BLAST is currently running, kill it?", "Kill current remote job?", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
                getDir().executeImmediately("close what=current;", ((Director) getDir()).getCommandManager());
                return;
            }
        }

        blastService.setProgram(RemoteBlastClient.BlastProgram.valueOf(blastMode));

        final ArrayList<Pair<String, String>> queries = new ArrayList<>();
        try (FastAFileIterator it = new FastAFileIterator(readsFile.getPath())) {
            while (it.hasNext()) {
                queries.add(it.next());
            }
        }

        blastService.setQueries(queries);
        blastService.setDatabase(blastDB);

        final Document doc = ((Director) getDir()).getDocument();
        doc.setProgressListener(new ProgressDialog("Blasting at NCBI", "Running", getViewer().getFrame()));
        doc.getProgressListener().setMaximum(1000);

        final ChangeListener<Number> progressChangeListener = (observable, oldValue, newValue) -> {
            try {
                if (doc.getProgressListener() != null)
                    doc.getProgressListener().setProgress(Math.round(1000 * newValue.doubleValue()));
            } catch (CanceledException e) {
                blastService.cancel();
            }
        };

        final ChangeListener<String> messageChangeListener = (observable, oldValue, newValue) -> {
            System.err.println(newValue);
            if (doc.getProgressListener() != null)
                doc.getProgressListener().setSubtask(newValue);
        };

        // if user cancels progress listener, cancels service
        final Thread thread = new Thread(() -> {
            while (serviceIsRunning) {
                if (doc.getProgressListener().isUserCancelled()) {
                    Platform.runLater(() -> {
                        if (blastService.isRunning())
                            blastService.cancel();
                    });
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        });
        thread.start();

        Platform.runLater(() -> {
            blastService.setOnRunning(event -> SwingUtilities.invokeLater(() -> {
                serviceIsRunning = true;
                getCommandManager().updateEnableState(NAME);
            }));

            blastService.messageProperty().addListener(messageChangeListener);
            blastService.progressProperty().addListener(progressChangeListener);
            blastService.restart();

            blastService.setOnSucceeded(event -> {
                blastService.progressProperty().removeListener(progressChangeListener);
                blastService.messageProperty().removeListener(messageChangeListener);

                final String result = blastService.getValue();

                SwingUtilities.invokeLater(() -> {
                    try {
                        if (result != null && result.length() > 0) {
                            try {
                                final File blastFile = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), Basic.replaceFileSuffix(readsFile, "." + blastMode), new MeganFileFilter(), new MeganFileFilter(), null, "Save BLAST file", "." + blastMode);
                                if (blastFile != null) {
                                    try (BufferedWriter w = new BufferedWriter(new FileWriter(blastFile))) {
                                        w.write(result);
                                    }
                                    System.err.println("Alignments written to: " + blastFile.getPath());
                                    if (ProgramProperties.isUseGUI()) {
                                        final int result1 = JOptionPane.showConfirmDialog(null, "Import remotely BLASTED file into MEGAN?", "Import - MEGAN", JOptionPane.YES_NO_CANCEL_OPTION);
                                        getDir().notifyUnlockInput();
                                        if (result1 == JOptionPane.YES_OPTION) {
                                            final ImportBlastDialog importBlastDialog = new ImportBlastDialog(getViewer().getFrame(), (Director) getDir(), "Import Blast File");
                                            importBlastDialog.getBlastFileNameField().setText(blastFile.getPath());
                                            importBlastDialog.getReadFileNameField().setText(readsFile.getPath());
                                            importBlastDialog.setLongReads(longReads);
                                            importBlastDialog.getMeganFileNameField().setText(Basic.replaceFileSuffix(blastFile.getPath(), "-" + blastMode + ".rma6"));
                                            importBlastDialog.updateView(IDirector.ALL);
                                            final String command = importBlastDialog.showAndGetCommand();
                                            if (command != null) {
                                                getDir().notifyUnlockInput();
                                                getDir().execute(command, getViewer().getCommandManager());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                NotificationsInSwing.showError("Create RMA file failed: " + ex.getMessage());
                                getDir().notifyUnlockInput();
                                getDir().executeImmediately("close what=current;", ((Director) getDir()).getCommandManager());
                            }
                        } else {
                            NotificationsInSwing.showInformation("No hits found");
                            getDir().notifyUnlockInput();
                            getDir().executeImmediately("close what=current;", ((Director) getDir()).getCommandManager());
                        }
                    } finally {
                        serviceIsRunning = false;
                        if (doc.getProgressListener() != null)
                            doc.getProgressListener().close();
                    }
                });
            });
            blastService.setOnFailed(event -> {
                blastService.progressProperty().removeListener(progressChangeListener);
                blastService.messageProperty().removeListener(messageChangeListener);
                NotificationsInSwing.showError("Remote blast failed: " + blastService.getException());

                SwingUtilities.invokeLater(() -> {
                    try {
                        doc.getProgressListener().close();
                        getDir().notifyUnlockInput();
                        getDir().executeImmediately("close what=current;", ((Director) getDir()).getCommandManager());
                    } finally {
                        serviceIsRunning = false;
                    }
                });

            });
            blastService.setOnCancelled(event -> {
                blastService.progressProperty().removeListener(progressChangeListener);
                blastService.messageProperty().removeListener(messageChangeListener);
                if (serviceIsRunning)
                    NotificationsInSwing.showWarning("Remote blast canceled");

                SwingUtilities.invokeLater(() -> {
                    if (serviceIsRunning) {
                        try {
                            getDir().notifyUnlockInput();
                            getDir().executeImmediately("close what=current;", ((Director) getDir()).getCommandManager());
                        } finally {
                            serviceIsRunning = false;
                            if (doc.getProgressListener() != null)
                                doc.getProgressListener().close();
                        }
                    }
                });
            });
        });
    }

    public void actionPerformed(ActionEvent event) {
        if (isApplicable()) {
            final IReadsProvider readProvider = ((IReadsProvider) getViewer());
            if (readProvider.isReadsAvailable()) {
                final Pair<String, String> first = readProvider.getReads(1).iterator().next();
                final String commandString = RemoteBlastDialog.apply(getViewer(), (Director) getDir(), readProvider, null, Basic.toCleanName(first.get1()));

                if (commandString != null) {
                    final Director newDir = Director.newProject();
                    newDir.getMainViewer().getFrame().setVisible(true);
                    newDir.getMainViewer().setDoReInduce(true);
                    newDir.getMainViewer().setDoReset(true);
                    newDir.executeImmediately(commandString, newDir.getMainViewer().getCommandManager());
                    getCommandManager().updateEnableState(NAME);
                }
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof IReadsProvider && (blastService == null || !serviceIsRunning) && ((IReadsProvider) getViewer()).isReadsAvailable();
    }

    private static final String NAME = "BLAST on NCBI...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Remotely BLAST sequence on NCBI website";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
