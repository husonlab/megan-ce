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
package megan.importblast;

import jloda.swing.commands.CommandManager;
import jloda.util.ProgramProperties;
import megan.importblast.commands.*;
import megan.parsers.blast.BlastFileFormat;
import megan.parsers.blast.BlastModeUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * the files panel
 * Daniel Huson, 12.2012
 */
public class FilesPanel extends JPanel {
    /**
     * make the files panel
     *
     * @return files panel
     */
    public FilesPanel(final ImportBlastDialog dialog) {
        final CommandManager commandManager = dialog.getCommandManager();

        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(new JLabel("Specify input and output files"));
        topPanel.add(Box.createHorizontalGlue());
        add(topPanel, BorderLayout.NORTH);

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setMaximumSize(new Dimension(400, 400));

        {
            dialog.getBlastFileNameField().setToolTipText("Add BLAST (or DAA or SAM or RDP or Silva) file(s) to import");

            JPanel panel1 = new JPanel();
            panel1.setLayout(new BorderLayout());
            panel1.setBorder(BorderFactory.createTitledBorder("1. Specify the BLAST (or DAA or MAF or SAM or RDP or Silva) file(s) to import"));
            JPanel line1 = new JPanel();
            line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
            // line1.add(Box.createHorizontalGlue());
            line1.add(new JScrollPane(dialog.getBlastFileNameField()));
            line1.setMaximumSize(new Dimension(1000, 80));
            line1.setMinimumSize(new Dimension(100, 80));
            line1.add(commandManager.getButton(ChooseBlastFileCommand.ALTNAME));

            panel1.add(line1, BorderLayout.CENTER);

            JPanel oneLine = new JPanel();
            oneLine.setLayout(new BoxLayout(oneLine, BoxLayout.X_AXIS));

            oneLine.add(Box.createHorizontalGlue());

            JLabel formatLabel = new JLabel("Format:");
            formatLabel.setForeground(Color.GRAY);
            oneLine.add(formatLabel);
            oneLine.add(dialog.getFormatCBox());
            oneLine.add(Box.createHorizontalGlue());

            JLabel modeLabel = new JLabel("Mode:");
            modeLabel.setForeground(Color.GRAY);
            oneLine.add(modeLabel);
            oneLine.add(dialog.getAlignmentModeCBox());
            oneLine.add(Box.createHorizontalGlue());

            JPanel twoLines = new JPanel();
            twoLines.setLayout(new BoxLayout(twoLines, BoxLayout.Y_AXIS));
            twoLines.add(oneLine);
            twoLines.add(Box.createVerticalStrut(12));
            panel1.add(twoLines, BorderLayout.SOUTH);

            innerPanel.add(panel1);
        }

        final JPanel readsFilePanel = new JPanel();
        final JTextArea readFileNameField = dialog.getReadFileNameField();

        final AbstractButton longReadsCBox;
        {
            readFileNameField.setToolTipText("Add reads files (FastA or FastQ format) to import, not neccessary for DAA files");

            readsFilePanel.setLayout(new BorderLayout());
            readsFilePanel.setBorder(BorderFactory.createTitledBorder("2. Specify the READs file(s) (FastA/Q format) to import (if available)"));
            JPanel line2 = new JPanel();
            line2.setLayout(new BoxLayout(line2, BoxLayout.X_AXIS));
            // line2.add(Box.createHorizontalGlue());
            line2.setMaximumSize(new Dimension(1000, 80));
            line2.setMinimumSize(new Dimension(100, 80));
            line2.add(new JScrollPane(readFileNameField));

            readFileNameField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    dialog.setReadFileName(readFileNameField.getText());
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    insertUpdate(event);
                }

                public void changedUpdate(DocumentEvent event) {
                    insertUpdate(event);
                }
            });
            line2.add(commandManager.getButton(ChooseReadsFileCommand.ALTNAME));
            readsFilePanel.add(line2, BorderLayout.CENTER);

            JPanel oneLine = new JPanel();
            oneLine.setLayout(new BoxLayout(oneLine, BoxLayout.X_AXIS));
            oneLine.add(Box.createHorizontalGlue());
            oneLine.add(commandManager.getButton(SetPairedReadsCommand.NAME));
            oneLine.add(Box.createHorizontalGlue());
            longReadsCBox = commandManager.getButton(SetLongReadsCommand.NAME);
            oneLine.add(longReadsCBox);
            oneLine.add(Box.createHorizontalGlue());

            JPanel twoLines = new JPanel();
            twoLines.setLayout(new BoxLayout(twoLines, BoxLayout.Y_AXIS));
            twoLines.add(oneLine);
            twoLines.add(Box.createVerticalStrut(12));
            readsFilePanel.add(twoLines, BorderLayout.SOUTH);

            innerPanel.add(readsFilePanel);
        }

        dialog.getBlastFileNameField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                String blastFileName = dialog.getBlastFileNameField().getText().trim();
                dialog.setBlastFileName(blastFileName);
                int endOfLine = blastFileName.indexOf("\n");
                if (endOfLine != -1)
                    blastFileName = blastFileName.substring(0, endOfLine).trim();
                try {
                    String format = BlastFileFormat.detectFormat(dialog, blastFileName, false).toString();
                    dialog.getFormatCBox().setSelectedFormat(format);
                    String mode = BlastModeUtils.detectMode(dialog, blastFileName, false).toString();
                    dialog.getAlignmentModeCBox().setSelectedMode(mode);

                    boolean isDAA = format.equalsIgnoreCase("daa");
                    readsFilePanel.setEnabled(!isDAA);
                    readFileNameField.setEnabled(!isDAA);
                    if (isDAA)
                        readFileNameField.setText("");

                } catch (Exception e) {
                    dialog.getFormatCBox().setSelectedFormat("Unknown");
                }
                commandManager.updateEnableState();
            }

            public void removeUpdate(DocumentEvent event) {
                insertUpdate(event);
            }

            public void changedUpdate(DocumentEvent event) {
                insertUpdate(event);
            }
        });

        {
            final JPanel panel3 = new JPanel();
            panel3.setLayout(new BorderLayout());
            panel3.setBorder(BorderFactory.createTitledBorder("3. Specify a new MEGAN file"));
            final JPanel line3 = new JPanel();
            line3.setLayout(new BoxLayout(line3, BoxLayout.X_AXIS));
            final JTextField meganFileNameField = dialog.getMeganFileNameField();

            meganFileNameField.setToolTipText("Name MEGAN file to create");
            meganFileNameField.setBorder(BorderFactory.createEmptyBorder());
            line3.add(new JScrollPane(meganFileNameField));

            meganFileNameField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    dialog.setMeganFileName(meganFileNameField.getText());
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    insertUpdate(event);
                }

                public void changedUpdate(DocumentEvent event) {
                    insertUpdate(event);
                }
            });
            line3.add(commandManager.getButton(ChooseMeganFileCommand.ALTNAME));
            panel3.add(line3, BorderLayout.CENTER);

            final JPanel below3 = new JPanel();
            below3.setLayout(new BoxLayout(below3, BoxLayout.LINE_AXIS));
            below3.add(dialog.getMaxNumberOfMatchesPerReadLabel());
            dialog.getMaxNumberOfMatchesPerReadField().setMinimumSize(new Dimension(70, 30));
            dialog.getMaxNumberOfMatchesPerReadField().setMaximumSize(new Dimension(400, 30));
            dialog.getMaxNumberOfMatchesPerReadField().setText("" + ProgramProperties.get("MaxNumberMatchesPerRead", 100));
            dialog.getMaxNumberOfMatchesPerReadField().setToolTipText("Specify the maximum number of matches to save per read." +
                    " A small value reduces the size of the MEGAN file, but may exclude some important matches.");
            below3.add(dialog.getMaxNumberOfMatchesPerReadField());
            longReadsCBox.addActionListener(e -> dialog.getMaxNumberOfMatchesPerReadField().setEnabled(!longReadsCBox.isSelected()));

            below3.add(Box.createHorizontalGlue());

            dialog.getUseCompressionCBox().setText("Use Compression");
            dialog.getUseCompressionCBox().setToolTipText("Compress reads and matches in RMA file. Files are much smaller, but take slightly longer to generate.");
            dialog.getUseCompressionCBox().setSelected(ProgramProperties.get("UseCompressInRMAFiles", true));
            below3.add(dialog.getUseCompressionCBox());

            below3.add(Box.createHorizontalStrut(50));

            JPanel twoLines = new JPanel();
            twoLines.setLayout(new BoxLayout(twoLines, BoxLayout.Y_AXIS));
            twoLines.add(below3);
            twoLines.add(Box.createVerticalStrut(12));
            panel3.add(twoLines, BorderLayout.SOUTH);

            innerPanel.add(panel3);
        }

        {
            final JPanel panel4 = new JPanel();
            panel4.setLayout(new BorderLayout());
            panel4.setBorder(BorderFactory.createTitledBorder("4. Short description of sample"));
            JPanel line4 = new JPanel();
            line4.setLayout(new BoxLayout(line4, BoxLayout.X_AXIS));

            dialog.getShortDescriptionField().setToolTipText("Provide short description of sample");
            dialog.getShortDescriptionField().setMaximumSize(new Dimension(100000, 30));

            dialog.getShortDescriptionField().getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    String description = dialog.getShortDescriptionField().getText().trim();
                    dialog.setShortDescription(description);
                    commandManager.updateEnableState();
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    insertUpdate(event);
                }

                public void changedUpdate(DocumentEvent event) {
                    insertUpdate(event);
                }
            });
            line4.add(dialog.getShortDescriptionField());
            panel4.add(line4, BorderLayout.CENTER);
            innerPanel.add(panel4);
        }

        innerPanel.add(Box.createVerticalGlue());
        innerPanel.add(Box.createVerticalStrut(10));
        add(innerPanel, BorderLayout.CENTER);

    }
}
