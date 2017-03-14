/*
 *  Copyright (C) 2017 Daniel H. Huson
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

package megan.blastclient;

import jloda.gui.ChooseFileDialog;
import jloda.gui.director.IDirectableViewer;
import jloda.util.*;
import megan.core.Director;
import megan.fx.NotificationsInSwing;
import megan.util.IReadsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * displays a dialog to setup remote blast run on NCBI
 * Daniel Huson, 3/14/17.
 */
public class RemoteBlastDialog {
    /**
     * displays a configuration screen and sets up the command string
     *
     * @param viewer
     * @param dir
     * @param readsProvider
     * @param providedReadsFile if not null, we use this file and ignore the readsProvider
     * @param queryName
     * @return command string or null
     */
    public static String apply(final IDirectableViewer viewer, Director dir, final IReadsProvider readsProvider, final String providedReadsFile, String queryName) {
        if (readsProvider == null && providedReadsFile == null)
            return null; // no reads provided...

        final String lastDir = ProgramProperties.get("RemoteBlastDir", System.getProperty("user.dir"));
        final File lastOpenFile = new File(lastDir, ProgramProperties.get("RemoteBlastFile", dir.getDocument().getTitle() + "-" + Basic.toCleanName(queryName) + ".fasta"));

        final boolean needToSaveReads = (providedReadsFile == null);

        final JDialog dialog = new JDialog(viewer.getFrame(), "Setup Remote NCBI Blast - MEGAN", true);
        dialog.setLocationRelativeTo(viewer.getFrame());
        dialog.setSize(500, 150);
        dialog.getContentPane().setLayout(new BorderLayout());
        final Single<String> result = new Single<>();

        final Dimension labelDim = new Dimension(100, 30);
        final Dimension minLineDim = new Dimension(100, 30);
        final Dimension maxLineDim = new Dimension(10000, 30);

        final JPanel topPanel = new JPanel();
        topPanel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        dialog.getContentPane().add(topPanel, BorderLayout.NORTH);

        final JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        topPanel.add(leftPanel);

        final JLabel readsFileLabel = new JLabel("Reads file:");
        readsFileLabel.setMinimumSize(labelDim);
        readsFileLabel.setMaximumSize(labelDim);
        leftPanel.add(readsFileLabel);
        final JLabel blastModeLabel = new JLabel("Blast Mode:");
        blastModeLabel.setMinimumSize(labelDim);
        blastModeLabel.setMaximumSize(labelDim);
        leftPanel.add(blastModeLabel);
        final JLabel blastDBLabel = new JLabel("Blast DB:");
        blastDBLabel.setMinimumSize(labelDim);
        blastDBLabel.setMaximumSize(labelDim);
        leftPanel.add(blastDBLabel);

        final JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        topPanel.add(rightPanel);

        final JPanel line1 = new JPanel();
        line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
        line1.setMinimumSize(minLineDim);
        line1.setMaximumSize(maxLineDim);

        final JTextField fileNameField = new JTextField(needToSaveReads ? lastOpenFile.getPath() : providedReadsFile);
        fileNameField.setEditable(needToSaveReads);
        line1.add(fileNameField);
        final JButton browseButton = new JButton(new AbstractAction("Browse...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final File readsFile = ChooseFileDialog.chooseFileToSave(viewer.getFrame(), new File(fileNameField.getText()), new FastaFileFilter(), new FastaFileFilter(), null, "Save READS file", ".fasta");
                if (readsFile != null)
                    fileNameField.setText(readsFile.getPath());
            }
        });
        browseButton.setEnabled(needToSaveReads);
        line1.add(browseButton);
        rightPanel.add(line1);

        final String previousMode = ProgramProperties.get("RemoteBlastMode", RemoteBlastClient.BlastProgram.blastn.toString());

        final JPanel line2 = new JPanel();
        line2.setLayout(new BoxLayout(line2, BoxLayout.X_AXIS));
        line2.setMinimumSize(minLineDim);
        line2.setMaximumSize(maxLineDim);

        final JComboBox<RemoteBlastClient.BlastProgram> modeCBox = new JComboBox<>();
        modeCBox.setEditable(false);

        for (RemoteBlastClient.BlastProgram mode : RemoteBlastClient.BlastProgram.values()) {
            modeCBox.addItem(mode);
            if (mode.toString().equalsIgnoreCase(previousMode))
                modeCBox.setSelectedItem(mode);
        }

        line2.add(modeCBox);
        line2.add(Box.createHorizontalGlue());
        rightPanel.add(line2);

        final JPanel line3 = new JPanel();
        line3.setLayout(new BoxLayout(line3, BoxLayout.X_AXIS));
        line3.setMinimumSize(minLineDim);
        line3.setMaximumSize(maxLineDim);

        final JComboBox<String> dbCBox = new JComboBox<>();
        dbCBox.setEditable(false);
        line3.add(dbCBox);
        line3.add(Box.createHorizontalGlue());
        rightPanel.add(line3);

        modeCBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                dbCBox.removeAllItems();
                for (String item : RemoteBlastClient.getDatabaseNames(RemoteBlastClient.BlastProgram.valueOfIgnoreCase(modeCBox.getSelectedItem().toString()))) {
                    dbCBox.addItem(item);
                }
            }
        });
        final String previousDB = ProgramProperties.get("RemoteBlastDB", "nr");

        for (String item : RemoteBlastClient.getDatabaseNames((RemoteBlastClient.BlastProgram) modeCBox.getSelectedItem())) {
            dbCBox.addItem(item);
            if (item.equalsIgnoreCase(previousDB))
                dbCBox.setSelectedItem(item);
        }

        final JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        bottomPanel.add(Box.createHorizontalGlue());
        final JButton cancelButton = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        bottomPanel.add(cancelButton);
        final JButton applyButton = new JButton(new AbstractAction("Apply") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final String readsFile = fileNameField.getText().trim();

                    if (readsFile.length() > 0) {
                        if (needToSaveReads) {
                            int maxNumberRemoteBlastReads = ProgramProperties.get("MaxNumberRemoteBlastReads", 100);
                            final Collection<Pair<String, String>> pairs = readsProvider.getReads(maxNumberRemoteBlastReads + 1);
                            if (pairs.size() > maxNumberRemoteBlastReads) {
                                System.err.println("Number of reads (" + readsProvider.isReadsAvailable() + ") exceeds MaxNumberRemoteBlastReads (" + maxNumberRemoteBlastReads + ")");
                                System.err.println("Use 'setprop MaxNumberRemoteBlastReads=X' to change to limit to X");
                            }

                            int count = 0;
                            try (BufferedWriter w = new BufferedWriter(new FileWriter(readsFile))) {
                                for (Pair<String, String> pair : pairs) {
                                    w.write(String.format(">%s\n%s\n", pair.get1(), pair.get2()));
                                    if (++count == maxNumberRemoteBlastReads)
                                        break;
                                }
                            } catch (IOException ex) {
                                NotificationsInSwing.showError("Write file failed: " + ex.getMessage());
                                return;
                            }
                            System.err.println("Reads written to: " + readsFile);
                        }
                        ProgramProperties.put("RemoteBlastDir", (new File(readsFile)).getParent());
                        if (dbCBox.getSelectedItem() != null)
                            ProgramProperties.put("RemoteBlastDB", dbCBox.getSelectedItem().toString());
                        if (modeCBox.getSelectedItem() != null)
                            ProgramProperties.put("RemoteBlastMode", modeCBox.getSelectedItem().toString());

                        result.set("remoteBlastNCBI readsFile='" + fileNameField.getText().trim() + "' blastMode=" + modeCBox.getSelectedItem() + " blastDB='" + dbCBox.getSelectedItem() + "';");
                    }
                } finally {
                    dialog.setVisible(false);
                }
            }
        });

        fileNameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyButton.setEnabled(fileNameField.getText().trim().length() > 0);
            }
        });

        bottomPanel.add(applyButton);
        dialog.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
        return result.get();
    }
}
