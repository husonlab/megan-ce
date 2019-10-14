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

import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import megan.main.MeganProperties;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * dialog requesting setup of regular expressions for matching reads
 * Daniel Huson, 10.2009
 */
public class ReadPairingDialog {
    private final ImportBlastDialog importBlastDialog;
    private final JDialog dialog = new JDialog();

    private final JCheckBox noSufficesCBox = new JCheckBox();

    private final JTextField field1 = new JTextField(10);
    private final JTextField field2 = new JTextField(10);

    private boolean canceled = false;

    private String pairedReadSuffix1;
    private String pairedReadSuffix2;

    /**
     * constructor
     *
     * @param parent
     */
    public ReadPairingDialog(ImportBlastDialog parent, String readsFile) {
        this.importBlastDialog = parent;
        dialog.setModal(true);

        String[] suffixes = guessPairedSuffixes(readsFile);
        this.pairedReadSuffix1 = suffixes[0];
        this.pairedReadSuffix2 = suffixes[1];

        field1.setText(pairedReadSuffix1);
        field1.setToolTipText("Suffix of first read name");
        field2.setText(pairedReadSuffix2);
        field2.setToolTipText("Suffix of second read name");

        dialog.setTitle("Paired reads - " + ProgramProperties.getProgramName());
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(parent);

        Container container = dialog.getContentPane();
        container.setLayout(new BorderLayout());

        final JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        final JPanel midPanel = new JPanel();
        midPanel.setLayout(new BorderLayout());
        midPanel.setBorder(BorderFactory.createTitledBorder("Specify read pair suffixes:"));
        final JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new GridLayout(2, 3));
        innerPanel.add(new JLabel("Suffix 1"));
        innerPanel.add(new JLabel(""));
        innerPanel.add(new JLabel("Suffix 2"));
        innerPanel.add(field1);
        field1.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (noSufficesCBox.isSelected())
                    noSufficesCBox.setSelected(false);
            }

            public void removeUpdate(DocumentEvent e) {
                if (noSufficesCBox.isSelected())
                    noSufficesCBox.setSelected(false);
            }

            public void changedUpdate(DocumentEvent e) {
                if (noSufficesCBox.isSelected())
                    noSufficesCBox.setSelected(false);
            }
        });
        innerPanel.add(new JLabel("paired with"));
        innerPanel.add(field2);
        field2.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (noSufficesCBox.isSelected())
                    noSufficesCBox.setSelected(false);
            }

            public void removeUpdate(DocumentEvent e) {
                if (noSufficesCBox.isSelected())
                    noSufficesCBox.setSelected(false);
            }

            public void changedUpdate(DocumentEvent e) {
                if (noSufficesCBox.isSelected())
                    noSufficesCBox.setSelected(false);
            }
        });
        midPanel.add(innerPanel, BorderLayout.CENTER);
        centerPanel.add(midPanel, BorderLayout.CENTER);

        JPanel nextPanel = new JPanel();
        nextPanel.setLayout(new BoxLayout(nextPanel, BoxLayout.LINE_AXIS));
        nextPanel.add(new JLabel("Read pairs not distinguished by suffix:"));
        nextPanel.add(noSufficesCBox);
        noSufficesCBox.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                for (Component component : innerPanel.getComponents()) {
                    component.setEnabled(!noSufficesCBox.isSelected());
                }
            }
        });
        centerPanel.add(nextPanel, BorderLayout.SOUTH);
        container.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        bottomPanel.add(new JButton(getCancelAction()));
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(new JButton(getApplyAction()));

        container.add(bottomPanel, BorderLayout.SOUTH);

        if (this.pairedReadSuffix1.length() == 0)
            noSufficesCBox.setSelected(true);
    }

    /**
     * show the dialog
     *
     * @return true, if not canceled
     */
    public boolean showDialog() {
        dialog.setVisible(true);
        return !canceled;
    }

    private AbstractAction cancelAction;

    private AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;

        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                canceled = true;
                dialog.setVisible(false);
            }
        };
        action.putValue(Action.NAME, "Cancel");
        action.putValue(Action.SHORT_DESCRIPTION, "Cancel this dialog");
        return cancelAction = action;
    }

    private AbstractAction applyAction;

    private AbstractAction getApplyAction() {
        AbstractAction action = applyAction;

        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (noSufficesCBox.isSelected()) {
                    importBlastDialog.setPairedReadSuffix1("");
                    importBlastDialog.setPairedReadSuffix2("");
                } else {
                    pairedReadSuffix1 = field1.getText();
                    pairedReadSuffix2 = field2.getText();
                    if (pairedReadSuffix1.equals("?") || pairedReadSuffix2.equals("?")) {
                        NotificationsInSwing.showError("Must specify suffix for both reads");
                        return;
                    }
                    if (pairedReadSuffix1.length() != pairedReadSuffix2.length()) {
                        NotificationsInSwing.showError(dialog, "Suffixes must have sample length");
                        return;
                    }
                    importBlastDialog.setPairedReadSuffix1(pairedReadSuffix1);
                    importBlastDialog.setPairedReadSuffix2(pairedReadSuffix2);
                }
                dialog.setVisible(false);
            }
        };
        action.putValue(Action.NAME, "Apply");
        action.putValue(Action.SHORT_DESCRIPTION, "Use the specified suffixes to pair reads");
        return applyAction = action;
    }

    /**
     * try to guess the appropriate patterns
     *
     * @param readsFile
     * @return patterns
     */
    private String[] guessPairedSuffixes(String readsFile) {
        String pattern1 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX1, "?");
        String pattern2 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX2, "?");

        if (readsFile != null && readsFile.length() > 0) {
            BufferedReader r = null;
            try {
                r = new BufferedReader(new InputStreamReader(ResourceManager.getFileAsStream(readsFile)));
                if (r.ready()) {
                    String[] words = r.readLine().split(" ");

                    String name;
                    if (words.length >= 2) {
                        if (words[0].equals(">"))
                            name = words[1];
                        else
                            name = words[0];
                    } else
                        name = null;

                    if (name != null) {
                        if (name.endsWith("_1") || name.endsWith("_2")) {
                            pattern1 = "_1";
                            pattern2 = "_2";
                        } else if (name.endsWith(".1") || name.endsWith(".2")) {
                            pattern1 = ".1";
                            pattern2 = ".2";
                        } else if (name.endsWith("_F3") || name.endsWith("_R3")) {
                            pattern1 = "_F3";
                            pattern2 = "_R3";
                        } else if (name.endsWith("_1,") || name.endsWith("_2,")) {
                            pattern1 = "_1,";
                            pattern2 = "_2,";
                        } else if (name.endsWith(".1,") || name.endsWith(".2,")) {
                            pattern1 = ".1,";
                            pattern2 = ".2,";
                        } else if (name.endsWith("_F3,") || name.endsWith("_R3,")) {
                            pattern1 = "_F3,";
                            pattern2 = "_R3,";
                        } else if (name.endsWith("/1") || name.endsWith("/2")) {
                            pattern1 = "/1";
                            pattern2 = "/2";
                        } else if (name.endsWith(".x") || name.endsWith(".y")) {
                            pattern1 = ".x";
                            pattern2 = ".y";
                        } else if (name.endsWith(".a") || name.endsWith(".b")) {
                            pattern1 = ".a";
                            pattern2 = ".b";
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (r != null)
                    try {
                        r.close();
                    } catch (IOException ignored) {
                    }
            }
        }
        return new String[]{pattern1, pattern2};
    }

    public String getPairedReadSuffix1() {
        return pairedReadSuffix1;
    }

    public String getPairedReadSuffix2() {
        return pairedReadSuffix2;
    }
}
