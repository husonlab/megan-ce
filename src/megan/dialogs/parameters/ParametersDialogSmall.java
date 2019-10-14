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
package megan.dialogs.parameters;

import jloda.util.Basic;
import megan.core.Director;
import megan.core.Document;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * parameter dialog for CSV import
 * Daniel Huson, 8.2008
 */
public class ParametersDialogSmall extends JDialog {
    private final JTextField minScoreField = new JTextField(8);
    private final JTextField topPercentField = new JTextField(8);
    private final JTextField minSupportPercentField = new JTextField(8);
    private final JTextField minSupportField = new JTextField(8);
    // no mincomplexity field because we don't get to see the sequences

    private boolean ok = false;

    public ParametersDialogSmall(Component parent, Director dir) {
        Document doc = dir.getDocument();
        setMinScore(doc.getMinScore());
        setTopPercent(doc.getTopPercent());
        setMinSupportPercent(doc.getMinSupportPercent());
        setMinSupport(doc.getMinSupportPercent() > 0 ? 0 : doc.getMinSupport());

        setLocationRelativeTo(parent);
        setTitle("Choose LCA Parameters - MEGAN");
        setModal(true);
        setSize(300, 200);


        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(makeLCAParametersPanel(), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(new JButton(getCancelAction()));

        JButton applyButton = new JButton(getApplyAction());
        bottomPanel.add(applyButton);
        getRootPane().setDefaultButton(applyButton);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        // actions.getApplyAction().setEnabled(false);

        setVisible(true);
    }

    /**
     * construct the parameters panel
     */
    private JPanel makeLCAParametersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel();
        centerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("LCA parameters:"),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        centerPanel.setLayout(new GridLayout(6, 2));

        centerPanel.add(new JLabel("Min Score:"));
        centerPanel.add(minScoreField);
        minScoreField.setToolTipText("Minimal bitscore that a match must attain");
        minScoreField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateEnableState();
            }

            public void removeUpdate(DocumentEvent event) {
                updateEnableState();
            }

            public void changedUpdate(DocumentEvent event) {
                updateEnableState();
            }
        });

        centerPanel.add(new JLabel(" "));
        centerPanel.add(new JLabel(" "));

        centerPanel.add(new JLabel("Top Percent:"));
        centerPanel.add(topPercentField);
        topPercentField.setToolTipText("Match must lie within this percentage of the best score attained for a read");
        topPercentField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateEnableState();
            }

            public void removeUpdate(DocumentEvent event) {
                updateEnableState();
            }

            public void changedUpdate(DocumentEvent event) {
                updateEnableState();
            }
        });

        centerPanel.add(new JLabel(" "));
        centerPanel.add(new JLabel(" "));

        centerPanel.add(new JLabel("Min Support Percent:"));
        centerPanel.add(minSupportPercentField);
        minSupportPercentField.setToolTipText("Minimum number of reads that a taxon must obtain as a percentage of total reads assigned");
        minSupportPercentField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateEnableState();
            }

            public void removeUpdate(DocumentEvent event) {
                updateEnableState();

            }

            public void changedUpdate(DocumentEvent event) {
                updateEnableState();
            }
        });

        centerPanel.add(new JLabel("Min Support:"));
        centerPanel.add(minSupportField);
        minSupportField.setToolTipText("Minimum number of reads that a taxon must obtain");
        minSupportField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateEnableState();
            }

            public void removeUpdate(DocumentEvent event) {
                updateEnableState();
            }

            public void changedUpdate(DocumentEvent event) {
                updateEnableState();
            }
        });

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    public double getMinScore() {
        double value = Document.DEFAULT_MINSCORE;
        try {
            value = Double.parseDouble(minScoreField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    public double getTopPercent() {
        double value = Document.DEFAULT_TOPPERCENT;
        try {
            value = Double.parseDouble(topPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    private void setMinScore(double value) {
        minScoreField.setText("" + (float) value);
    }

    private void setTopPercent(double value) {
        topPercentField.setText("" + value);
    }

    public int getMinSupport() {
        int value = Document.DEFAULT_MINSUPPORT;
        try {
            value = Basic.parseInt(minSupportField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(1, value);
    }

    private void setMinSupport(int value) {
        minSupportField.setText("" + Math.max(1, value));
    }


    public float getMinSupportPercent() {
        float value = 0;
        try {
            value = Basic.parseFloat(minSupportPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(0, value);
    }

    private void setMinSupportPercent(float value) {
        minSupportPercentField.setText("" + Math.max(0f, value) + (value <= 0 ? " (off)" : ""));
    }

    /**
     * updates the enable state of all action items...
     */
    private void updateEnableState() {
        // originally false, true once some replace has been entered
        applyAction.setEnabled(true);
    }


    private AbstractAction cancelAction;

    private AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;

        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                ok = false;
                setVisible(false);
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
                ok = true;
                setVisible(false);
            }
        };
        action.setEnabled(true);
        action.putValue(Action.NAME, "Apply");
        action.putValue(Action.SHORT_DESCRIPTION, "Rerun the LCA analysis using the set parameters");
        return applyAction = action;
    }

    /**
     * did the user select apply?
     *
     * @return true, if apply
     */
    public boolean isOk() {
        return ok;
    }
}
