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
package megan.dialogs.importcsv;

import jloda.swing.director.IDirectableViewer;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.classification.ClassificationManager;
import megan.core.Director;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * dialog for importing data from a CSV file
 * Daniel Huson, 12.2010
 */
public class ImportCSVWindow extends JDialog {
    private boolean ok = false;
    private long multiplier = 1L;

    private boolean doReadsHits = false;

    private boolean tabSeparator = false;

    private final Set<String> selectedCNames = new HashSet<>();
    private boolean parseAccessions;

    private AbstractAction applyAction;

    private final IDirectableViewer viewer;
    private final Director dir;

    /**
     * setup and display the import csv window
     */
    public ImportCSVWindow(IDirectableViewer viewer, Director dir) {
        super();
        this.viewer = viewer;
        this.dir = dir;
        setLocationRelativeTo(viewer.getFrame());
        setSize(330, 450);

        setModal(true);
    }

    /**
     * setup the dialog
     */
    private void setup() {
        setTitle("Import  CSV - " + ProgramProperties.getProgramVersion());

        getContentPane().setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        mainPanel.add(new JLabel("Setup import from file in CSV format"), BorderLayout.NORTH);

        JPanel middle = new JPanel();
        // middle.setBorder(BorderFactory.createEtchedBorder());
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        mainPanel.add(middle, BorderLayout.CENTER);

        JPanel aPanel = new JPanel();
        aPanel.setLayout(new BoxLayout(aPanel, BoxLayout.Y_AXIS));
        aPanel.setBorder(BorderFactory.createTitledBorder("Format"));

        final JTextField multiplierField = new JTextField("" + multiplier, 8);
        multiplierField.setMaximumSize(new Dimension(80, 20));
        multiplierField.setToolTipText("Multiple all counts by this number");

        final JRadioButton firstFormat = new JRadioButton();
        firstFormat.setAction(new AbstractAction("Class Count [Count ...]") {
            public void actionPerformed(ActionEvent e) {
                doReadsHits = !firstFormat.isSelected();
                multiplierField.setEnabled(true);
            }
        });
        firstFormat.setToolTipText("CSV format is Class-Name,Count(,Counts...)");
        firstFormat.setSelected(!isDoReadsHits());
        aPanel.add(firstFormat);
        final JRadioButton secondFormat = new JRadioButton();
        secondFormat.setAction(new AbstractAction("Read Class [Score]") {
            public void actionPerformed(ActionEvent e) {
                doReadsHits = secondFormat.isSelected();
                multiplierField.setEnabled(false);
            }
        });
        secondFormat.setToolTipText("CSV format is R,C,S, where R=nead-name, C=class-name or class-id (if only importing one classification) and S=score (optional)");
        secondFormat.setSelected(isDoReadsHits());
        aPanel.add(secondFormat);
        ButtonGroup group = new ButtonGroup();
        group.add(secondFormat);
        group.add(firstFormat);
        JPanel around = new JPanel();
        around.setLayout(new BorderLayout());
        around.add(aPanel, BorderLayout.CENTER);
        middle.add(around);

        JPanel bPanel = new JPanel();
        bPanel.setLayout(new BoxLayout(bPanel, BoxLayout.Y_AXIS));
        bPanel.setBorder(BorderFactory.createTitledBorder("Separator"));

        final JRadioButton commaButton = new JRadioButton();
        commaButton.setAction(new AbstractAction("Comma") {
            public void actionPerformed(ActionEvent e) {
                tabSeparator = !commaButton.isSelected();
            }
        });
        commaButton.setSelected(!isTabSeparator());
        bPanel.add(commaButton);

        final JRadioButton tabButton = new JRadioButton();
        tabButton.setAction(new AbstractAction("Tab") {
            public void actionPerformed(ActionEvent e) {
                tabSeparator = !tabButton.isSelected();
            }
        });
        bPanel.add(tabButton);
        tabButton.setSelected(isTabSeparator());
        ButtonGroup group2 = new ButtonGroup();
        group2.add(commaButton);
        group2.add(tabButton);
        around = new JPanel();
        around.setLayout(new BorderLayout());
        around.add(bPanel, BorderLayout.CENTER);
        middle.add(around);

        JPanel cPanel = new JPanel();
        cPanel.setLayout(new BoxLayout(cPanel, BoxLayout.Y_AXIS));
        cPanel.setBorder(BorderFactory.createTitledBorder("Classifications"));

        final String[] cNames = ClassificationManager.getAllSupportedClassifications().toArray(new String[0]);

        for (final String cName : cNames) {
            final JCheckBox checkBox = new JCheckBox();
            AbstractAction action = new AbstractAction(cName) {
                public void actionPerformed(ActionEvent e) {
                    if (checkBox.isSelected())
                        selectedCNames.add(cName);
                    else
                        selectedCNames.remove(cName);
                    applyAction.setEnabled(selectedCNames.size() > 0);
                }
            };
            checkBox.setAction(action);

            checkBox.setToolTipText("Try to match named classes to " + cName);
            cPanel.add(checkBox);
        }

        around = new JPanel();
        around.setLayout(new BorderLayout());
        around.add(cPanel, BorderLayout.CENTER);
        middle.add(around);

        JPanel dPanel = new JPanel();
        dPanel.setLayout(new GridLayout(2, 1));
        dPanel.setBorder(BorderFactory.createEtchedBorder());

        final JPanel line2 = new JPanel();
        line2.setLayout(new BoxLayout(line2, BoxLayout.X_AXIS));

        final JCheckBox useAccessionIds = new JCheckBox();

        final Set<String> classifications = new HashSet<>(Arrays.asList(cNames));

        useAccessionIds.setAction(new AbstractAction("Parse accessions ids") {
            public void actionPerformed(ActionEvent e) {
                parseAccessions = useAccessionIds.isSelected();
            }
        });
        useAccessionIds.setSelected(parseAccessions);
        useAccessionIds.setToolTipText("Use accession number parsing for " + Basic.toString(classifications, ", ") + " ids");
        line2.add(useAccessionIds);

        dPanel.add(line2);

        multiplierField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent documentEvent) {
                changedUpdate(documentEvent);
            }

            public void removeUpdate(DocumentEvent documentEvent) {
                changedUpdate(documentEvent);
            }

            public void changedUpdate(DocumentEvent documentEvent) {
                String text = multiplierField.getText();
                if (Basic.isLong(text))
                    multiplier = Long.parseLong(text);
            }
        });
        JPanel line1 = new JPanel();
        line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
        line1.add(Box.createHorizontalStrut(10));
        line1.add(new JLabel("Multiplier:"));
        line1.add(multiplierField);
        dPanel.add(line1);

        around = new JPanel();
        around.setLayout(new BorderLayout());
        around.add(dPanel, BorderLayout.CENTER);
        middle.add(around);

        JPanel bottom = new JPanel();
        bottom.setBorder(BorderFactory.createEtchedBorder());
        bottom.setLayout(new BorderLayout());

        bottom.setLayout(new BoxLayout(bottom, BoxLayout.LINE_AXIS));
        bottom.add(Box.createHorizontalGlue());
        bottom.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }));

        applyAction = new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                if (getSelectedCNames().size() > 0) {
                    for (String cName : getSelectedCNames()) {
                        ProgramProperties.get("Use" + cName, true);
                    }

                    if (parseAccessions) {
                        final ParsersDialog parsersDialog = new ParsersDialog(viewer.getFrame(), dir, getSelectedCNames());
                        parsersDialog.setVisible(true);
                        ok = parsersDialog.isPressedApply();
                    } else
                        ok = true;
                }
            }
        };
        applyAction.setEnabled(false);
        bottom.add(new JButton(applyAction));
        getContentPane().add(bottom, BorderLayout.SOUTH);
    }

    public Set<String> getSelectedCNames() {
        return selectedCNames;
    }

    /**
     * show the window and return true, if not canceled
     *
     * @return ok
     */
    public boolean apply() {
        setup();
        setVisible(true);
        return ok;
    }

    public boolean isDoReadsHits() {
        return doReadsHits;
    }

    public long getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(long multiplier) {
        this.multiplier = multiplier;
    }


    public void setDoReadsHits(boolean doReadsHits) {
        this.doReadsHits = doReadsHits;
    }

    public boolean isTabSeparator() {
        return tabSeparator;
    }

    public void setTabSeparator(boolean tabSeparator) {
        this.tabSeparator = tabSeparator;
    }

    public boolean isParseAccessions() {
        return parseAccessions;
    }

    public void setParseAccessions(boolean parseAccessions) {
        this.parseAccessions = parseAccessions;
    }
}
