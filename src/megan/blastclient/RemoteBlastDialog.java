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

package megan.blastclient;

import jloda.swing.director.IDirectableViewer;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.FastaFileFilter;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.Single;
import megan.core.Director;
import megan.core.MeganFile;
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
import java.util.Objects;

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

        final MeganFile meganFile = dir.getDocument().getMeganFile();
        final String lastDir = (meganFile.isMeganServerFile() ? ProgramProperties.get("RemoteBlastDir", System.getProperty("user.dir")) : new File(meganFile.getFileName()).getParent());
        final File lastOpenFile = new File(lastDir, ProgramProperties.get("RemoteBlastFile", Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-" + Basic.toCleanName(queryName) + ".fasta")));

        final boolean needToSaveReads = (providedReadsFile == null);

        final JDialog dialog = new JDialog(viewer.getFrame(), "Setup Remote NCBI Blast - MEGAN", true);
        dialog.setLocationRelativeTo(viewer.getFrame());
        dialog.setSize(500, 180);
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


        final JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        topPanel.add(rightPanel);

        final JTextField fileNameField = new JTextField(needToSaveReads ? lastOpenFile.getPath() : providedReadsFile);
        {
            final JLabel readsFileLabel = new JLabel("Reads file:");
            readsFileLabel.setMinimumSize(labelDim);
            readsFileLabel.setMaximumSize(labelDim);
            leftPanel.add(readsFileLabel);

            final JPanel aLine = new JPanel();
            aLine.setLayout(new BoxLayout(aLine, BoxLayout.X_AXIS));
            aLine.setMinimumSize(minLineDim);
            aLine.setMaximumSize(maxLineDim);

            fileNameField.setEditable(needToSaveReads);
            aLine.add(fileNameField);
            final JButton browseButton = new JButton(new AbstractAction("Browse...") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final File readsFile = ChooseFileDialog.chooseFileToSave(viewer.getFrame(), new File(fileNameField.getText()), new FastaFileFilter(), new FastaFileFilter(), null, "Save READS file", ".fasta");
                    if (readsFile != null) {
                        fileNameField.setText(readsFile.getPath());
                    }
                }
            });
            browseButton.setEnabled(needToSaveReads);
            aLine.add(browseButton);
            rightPanel.add(aLine);

            fileNameField.setToolTipText("Reads will be saved to this file");
        }

        final JCheckBox longReadsCheckBox = new JCheckBox();
        {
            final JLabel longReadsLabel = new JLabel("Long reads:");
            longReadsLabel.setMinimumSize(labelDim);
            longReadsLabel.setMaximumSize(labelDim);
            leftPanel.add(longReadsLabel);

            final JPanel aLine = new JPanel();
            aLine.setLayout(new BoxLayout(aLine, BoxLayout.X_AXIS));
            aLine.setMinimumSize(minLineDim);
            aLine.setMaximumSize(maxLineDim);
            aLine.add(longReadsCheckBox);
            longReadsCheckBox.setSelected(dir.getDocument().isLongReads());
            rightPanel.add(aLine);

            longReadsCheckBox.setToolTipText("Are these long reads that are expected to contain more than one gene?");
        }

        final JComboBox<RemoteBlastClient.BlastProgram> blastModeCBox = new JComboBox<>();
        {
            final JLabel blastModeLabel = new JLabel("Blast Mode:");
            blastModeLabel.setMinimumSize(labelDim);
            blastModeLabel.setMaximumSize(labelDim);
            leftPanel.add(blastModeLabel);

            final JPanel aLine = new JPanel();
            aLine.setLayout(new BoxLayout(aLine, BoxLayout.X_AXIS));
            aLine.setMinimumSize(minLineDim);
            aLine.setMaximumSize(maxLineDim);

            blastModeCBox.setEditable(false);

            final String previousMode = ProgramProperties.get("RemoteBlastMode", RemoteBlastClient.BlastProgram.blastn.toString());
            for (RemoteBlastClient.BlastProgram mode : RemoteBlastClient.BlastProgram.values()) {
                blastModeCBox.addItem(mode);
                if (mode.toString().equalsIgnoreCase(previousMode))
                    blastModeCBox.setSelectedItem(mode);
            }

            aLine.add(blastModeCBox);
            aLine.add(Box.createHorizontalGlue());
            rightPanel.add(aLine);

            blastModeCBox.setToolTipText("BLAST mode to be run at NCBI");
        }

        final JComboBox<String> blastDataBaseCBox = new JComboBox<>();
        {
            final JLabel blastDBLabel = new JLabel("Blast DB:");
            blastDBLabel.setMinimumSize(labelDim);
            blastDBLabel.setMaximumSize(labelDim);
            leftPanel.add(blastDBLabel);

            final JPanel aLine = new JPanel();
            aLine.setLayout(new BoxLayout(aLine, BoxLayout.X_AXIS));
            aLine.setMinimumSize(minLineDim);
            aLine.setMaximumSize(maxLineDim);

            blastDataBaseCBox.setEditable(false);
            aLine.add(blastDataBaseCBox);
            aLine.add(Box.createHorizontalGlue());
            rightPanel.add(aLine);

            blastModeCBox.addItemListener(e -> {
                blastDataBaseCBox.removeAllItems();
                for (String item : RemoteBlastClient.getDatabaseNames(RemoteBlastClient.BlastProgram.valueOfIgnoreCase(blastModeCBox.getSelectedItem().toString()))) {
                    blastDataBaseCBox.addItem(item);
                }
            });
            final String previousDB = ProgramProperties.get("RemoteBlastDB", "nr");

            for (String item : RemoteBlastClient.getDatabaseNames((RemoteBlastClient.BlastProgram) Objects.requireNonNull(blastModeCBox.getSelectedItem()))) {
                blastDataBaseCBox.addItem(item);
                if (item.equalsIgnoreCase(previousDB))
                    blastDataBaseCBox.setSelectedItem(item);
            }

            blastDataBaseCBox.setToolTipText("Blast database to be used at NCBI");
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
                    ProgramProperties.put("RemoteBlastDir", (new File(readsFile)).getParent());


                    if (readsFile.length() > 0) {
                        if (needToSaveReads) {
                            int maxNumberRemoteBlastReads = ProgramProperties.get("MaxNumberRemoteBlastReads", 100);
                            final Collection<Pair<String, String>> pairs = readsProvider.getReads(maxNumberRemoteBlastReads + 1);
                            if (pairs.size() > maxNumberRemoteBlastReads) {
                                System.err.println("Number of reads (" + readsProvider.isReadsAvailable() + ") exceeds MaxNumberRemoteBlastReads (" + maxNumberRemoteBlastReads + ")");
                                System.err.println("Use 'setprop MaxNumberRemoteBlastReads=X' to change limit to X");
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
                        if (blastDataBaseCBox.getSelectedItem() != null)
                            ProgramProperties.put("RemoteBlastDB", blastDataBaseCBox.getSelectedItem().toString());
                        if (blastModeCBox.getSelectedItem() != null)
                            ProgramProperties.put("RemoteBlastMode", blastModeCBox.getSelectedItem().toString());

                        result.set("remoteBlastNCBI readsFile='" + fileNameField.getText().trim() + "' longReads=" + longReadsCheckBox.isSelected() + " blastMode=" + blastModeCBox.getSelectedItem() + " blastDB='" + blastDataBaseCBox.getSelectedItem() + "';");
                    }
                } finally {
                    dialog.setVisible(false);
                }
            }
        });

        fileNameField.addActionListener(e -> applyButton.setEnabled(fileNameField.getText().trim().length() > 0));

        bottomPanel.add(applyButton);
        dialog.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
        return result.get();
    }
}
