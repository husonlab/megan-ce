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
package megan.dialogs.meganize;

import jloda.swing.commands.CommandManager;
import megan.dialogs.meganize.commands.ChooseDAAFilesCommand;
import megan.importblast.commands.SetLongReadsCommand;
import megan.parsers.blast.BlastFileFormat;
import megan.parsers.blast.BlastModeUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

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
    public FilesPanel(final MeganizeDAADialog dialog) {
        final CommandManager commandManager = dialog.getCommandManager();

        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(new JLabel("Specify Diamond Files to Meganize"));
        topPanel.add(Box.createHorizontalGlue());
        add(topPanel, BorderLayout.NORTH);

        final JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BorderLayout());
        innerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        final JPanel aPanel = new JPanel();
        aPanel.setLayout(new BoxLayout(aPanel, BoxLayout.Y_AXIS));
        {
            dialog.getBlastFileNameField().setToolTipText("Add Diamond DAA file(s) to meganize");

            final JPanel panel1 = new JPanel();
            panel1.setPreferredSize(new Dimension(10000, 250));
            panel1.setLayout(new BorderLayout());
            panel1.setBorder(BorderFactory.createTitledBorder("1. Diamond file(s) to meganize"));
            final JPanel line1 = new JPanel();
            line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
            // line1.add(Box.createHorizontalGlue());
            line1.add(new JScrollPane(dialog.getBlastFileNameField()));
            line1.add(commandManager.getButton(ChooseDAAFilesCommand.ALTNAME));

            panel1.add(line1, BorderLayout.CENTER);

            final JPanel oneLine = new JPanel();
            oneLine.setLayout(new BoxLayout(oneLine, BoxLayout.X_AXIS));

            oneLine.add(Box.createHorizontalGlue());

            final JLabel formatLabel = new JLabel("Format:");
            formatLabel.setForeground(Color.GRAY);
            oneLine.add(formatLabel);
            oneLine.add(dialog.getFormatCBox());
            oneLine.add(Box.createHorizontalGlue());

            final JLabel modeLabel = new JLabel("Mode:");
            modeLabel.setForeground(Color.GRAY);
            oneLine.add(modeLabel);
            oneLine.add(dialog.getAlignmentModeCBox());
            oneLine.add(Box.createHorizontalGlue());

            final JPanel twoLines = new JPanel();
            twoLines.setLayout(new BoxLayout(twoLines, BoxLayout.Y_AXIS));
            twoLines.add(oneLine);
            twoLines.add(Box.createVerticalStrut(12));
            panel1.add(twoLines, BorderLayout.SOUTH);

            aPanel.add(panel1);
        }
        {
            final JPanel oneLine = new JPanel();
            oneLine.setLayout(new BoxLayout(oneLine, BoxLayout.X_AXIS));
            oneLine.add(Box.createHorizontalGlue());
            oneLine.add(commandManager.getButton(SetLongReadsCommand.NAME));
            oneLine.add(Box.createHorizontalGlue());
            aPanel.add(oneLine);
        }
        innerPanel.add(aPanel, BorderLayout.NORTH);


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
            JPanel panel4 = new JPanel();
            panel4.setPreferredSize(new Dimension(10000, 100));
            panel4.setLayout(new BorderLayout());
            panel4.setBorder(BorderFactory.createTitledBorder("2. Short description of sample"));
            JPanel line4 = new JPanel();
            line4.setLayout(new BoxLayout(line4, BoxLayout.X_AXIS));

            dialog.getShortDescriptionField().setToolTipText("Provide short description of sample");

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
            innerPanel.add(panel4, BorderLayout.SOUTH);
        }

        add(innerPanel, BorderLayout.CENTER);

    }
}
