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
package megan.chart.drawers;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * widget for controlling the co-occurrence drawer
 * Daniel Huson, 5.2013
 */
public class CoOccurrenceToolBar extends JToolBar {

    private final static Font font = new Font((new JLabel()).getFont().getFamily(), (new JLabel()).getFont().getStyle(), 10);
    private final JTextField minThresholdField = new JTextField(8);
    private final JTextField minPrevalenceField = new JTextField(3);
    private final JSlider minPrevalenceSlider = new JSlider();
    private final JTextField maxPrevalenceField = new JTextField(3);
    private final JSlider maxPrevalenceSlider = new JSlider();
    private final JTextField minProbabilityField = new JTextField(3);
    private final JSlider minProbabilitySlider = new JSlider();

    private final JComboBox<CoOccurrenceDrawer.Method> methodCBox = new JComboBox<>();

    private final JCheckBox showCooccurring = new JCheckBox();
    private final JCheckBox showAntiOccurring = new JCheckBox();
    private final JButton applyButton;

    /**
     * constructor
     *
     * @param coOccurrenceDrawer
     */
    public CoOccurrenceToolBar(final ChartViewer chartViewer, final CoOccurrenceDrawer coOccurrenceDrawer) {
        setFloatable(false);
        setBorder(BorderFactory.createEtchedBorder());
        setMaximumSize(new Dimension(1000000, 22));
        setPreferredSize(new Dimension(1000000, 22));

        add(getSmallLabel("Detection (%)"));
        minThresholdField.setMaximumSize(new Dimension(88, 15));
        minThresholdField.setText(String.format("%2.5f", Basic.restrictToRange(0.0, 100.0, coOccurrenceDrawer.getMinThreshold())));
        minThresholdField.setFont(font);
        minThresholdField.setToolTipText("Minimum % of reads that class must have to be considered present in a sample");
        add(minThresholdField);
        addSeparator();

        add(getSmallLabel("Min samples (%)"));
        minPrevalenceField.setMaximumSize(new Dimension(33, 15));
        minPrevalenceField.setText("" + Basic.restrictToRange(0, 100, coOccurrenceDrawer.getMinPrevalence()));
        minPrevalenceField.setFont(font);
        minPrevalenceField.setToolTipText("Minimum % of samples for which class must be considered present");

        minPrevalenceSlider.setToolTipText(minPrevalenceField.getToolTipText());
        minPrevalenceSlider.setValue(Basic.restrictToRange(0, 100, coOccurrenceDrawer.getMinPrevalence()));
        minPrevalenceSlider.addChangeListener(e -> {
            int newValue = ((JSlider) e.getSource()).getValue();
            minPrevalenceField.setText("" + newValue);
        });

        minPrevalenceField.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (Basic.isFloat(minPrevalenceField.getText())) {
                    int value = Basic.restrictToRange(0, 100, (int) Float.parseFloat(minPrevalenceField.getText()));
                    minPrevalenceSlider.setValue(value);
                }
            }
        });

        add(minPrevalenceSlider);
        add(minPrevalenceField);
        addSeparator();

        add(getSmallLabel("Max samples (%)"));
        maxPrevalenceField.setMaximumSize(new Dimension(33, 15));
        maxPrevalenceField.setText("" + Basic.restrictToRange(0, 100, coOccurrenceDrawer.getMaxPrevalence()));
        maxPrevalenceField.setFont(font);
        maxPrevalenceField.setToolTipText("Maximum % of samples for which class must be considered present");

        maxPrevalenceSlider.setToolTipText(maxPrevalenceField.getToolTipText());
        maxPrevalenceSlider.setValue(Basic.restrictToRange(0, 100, coOccurrenceDrawer.getMaxPrevalence()));
        maxPrevalenceSlider.addChangeListener(e -> {
            int newValue = ((JSlider) e.getSource()).getValue();
            maxPrevalenceField.setText("" + newValue);
        });

        maxPrevalenceField.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (Basic.isFloat(maxPrevalenceField.getText())) {
                    int value = Basic.restrictToRange(0, 100, (int) Float.parseFloat(maxPrevalenceField.getText()));
                    maxPrevalenceSlider.setValue(value);
                }
            }
        });

        add(maxPrevalenceSlider);
        add(maxPrevalenceField);
        addSeparator();

        add(getSmallLabel("Edge threshold (%)"));
        minProbabilityField.setMaximumSize(new Dimension(33, 15));
        minProbabilityField.setText("" + Basic.restrictToRange(0, 100, coOccurrenceDrawer.getMinProbability()));
        minProbabilityField.setFont(font);
        minProbabilityField.setToolTipText("Minimum % co-occurrence (or anti-occurrence) score required for two classes to be joined by an edge");

        minProbabilitySlider.setToolTipText(minProbabilityField.getToolTipText());
        minProbabilitySlider.setValue(Basic.restrictToRange(0, 100, coOccurrenceDrawer.getMinProbability()));
        minProbabilitySlider.addChangeListener(e -> {
            int newValue = ((JSlider) e.getSource()).getValue();
            minProbabilityField.setText("" + newValue);
        });

        minProbabilityField.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (Basic.isFloat(minProbabilityField.getText())) {
                    int value = Basic.restrictToRange(0, 100, (int) Float.parseFloat(minProbabilityField.getText()));
                    minProbabilitySlider.setValue(value);
                }
            }
        });

        add(minProbabilitySlider);
        add(minProbabilityField);
        addSeparator();

        methodCBox.setEditable(false);
        methodCBox.setFont(font);
        for (CoOccurrenceDrawer.Method method : CoOccurrenceDrawer.Method.values())
            methodCBox.addItem(method);
        methodCBox.setSelectedItem(coOccurrenceDrawer.getMethod());
        add(methodCBox);
        methodCBox.setToolTipText("Jaccard: # samples containing both classes / samples containing at least one of the two\n" +
                "Kendall's tau: (# concordant pairs - # discordant pairs) / (# concordant pairs + # discordant pairs)\n" +
                "Pearson's r: sample Pearson correlation coefficient");

        showCooccurring.setText("Co");
        showCooccurring.setFont(font);
        showCooccurring.setToolTipText("Show co-occurrence edges");
        showCooccurring.setSelected(coOccurrenceDrawer.isShowCoOccurring());
        showCooccurring.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                coOccurrenceDrawer.setShowCoOccurring(showCooccurring.isSelected());
                if (!showCooccurring.isSelected() && !showAntiOccurring.isSelected()) {
                    showAntiOccurring.setSelected(true);
                    coOccurrenceDrawer.setShowAntiOccurring(true);
                }
                // applyButton.getAction().actionPerformed(null);
            }
        });
        add(showCooccurring);

        showAntiOccurring.setText("Anti");
        showAntiOccurring.setFont(font);
        showAntiOccurring.setToolTipText("Show anti-occurrence edges");
        showAntiOccurring.setSelected(coOccurrenceDrawer.isShowAntiOccurring());
        showAntiOccurring.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                coOccurrenceDrawer.setShowAntiOccurring(showAntiOccurring.isSelected());
                if (!showCooccurring.isSelected() && !showAntiOccurring.isSelected()) {
                    showCooccurring.setSelected(true);
                    coOccurrenceDrawer.setShowCoOccurring(true);
                }
                // applyButton.getAction().actionPerformed(null);
            }
        });
        add(showAntiOccurring);
        addSeparator();

        applyButton = new JButton(new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    //chartViewer.getClassesList().enableLabels(chartViewer.getClassesList().getAllLabels());
                    coOccurrenceDrawer.setMinThreshold(Float.parseFloat(minThresholdField.getText()));
                    coOccurrenceDrawer.setMinProbability(Integer.parseInt(minProbabilityField.getText()));

                    coOccurrenceDrawer.setMinPrevalence(minPrevalenceSlider.getValue());
                    //  coOccurrenceDrawer.setMinPrevalence(Float.parseFloat(minPrevalenceField.getText()));

                    coOccurrenceDrawer.setMaxPrevalence(Integer.parseInt(maxPrevalenceField.getText()));

                    if (methodCBox.getSelectedItem() != null)
                        coOccurrenceDrawer.setMethod((CoOccurrenceDrawer.Method) methodCBox.getSelectedItem());

                    chartViewer.getDir().execute("show what=all target=classes;", chartViewer.getCommandManager());
                } catch (Exception ex) {
                    NotificationsInSwing.showInternalError(chartViewer.getFrame(), "CoOccurrence drawer: " + ex.getMessage());
                }
            }
        });
        applyButton.setFont(font);
        add(applyButton);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        minThresholdField.setEnabled(enabled);
        minPrevalenceField.setEnabled(enabled);
        minPrevalenceSlider.setEnabled(enabled);
        maxPrevalenceField.setEnabled(enabled);
        maxPrevalenceSlider.setEnabled(enabled);
        minProbabilityField.setEnabled(enabled);
        minProbabilitySlider.setEnabled(enabled);
        methodCBox.setEnabled(enabled);
        showCooccurring.setEnabled(enabled);
        showAntiOccurring.setEnabled(enabled);
        applyButton.setEnabled(enabled);
    }

    private static JLabel getSmallLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        return label;
    }
}
