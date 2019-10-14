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
package megan.stats;

import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.ProgramProperties;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Objects;
import java.util.TreeSet;

/**
 * dialog to set up comparison of data sets
 * Daniel Huson, 3.2007
 */
public class ComparisonStatsWindow extends JDialog {
    private final IDirector dir;

    final JComboBox dataCBox1;
    final JComboBox dataCBox2;
    final JComboBox methodCBox;

    private final JPanel optionsPanel;


    private final ComparisonStatsActions actions;

    /**
     * setup and display the compare dialog
     *
     * @param parent
     */
    public ComparisonStatsWindow(Component parent, IDirector dir) {
        super();
        this.dir = dir;
        setTitle("Statistical Comparison - " + ProgramProperties.getProgramVersion());
        if (parent != null)
            setLocationRelativeTo(parent);
        else
            setLocation(300, 300);
        setSize(350, 350);

        setModal(true);

        dataCBox1 = new JComboBox();
        loadList(dataCBox1);
        if (dataCBox1.getItemCount() > 0)
            dataCBox1.setSelectedIndex(0); // select first
        dataCBox2 = new JComboBox();
        loadList(dataCBox2);
        if (dataCBox2.getItemCount() > 1)
            dataCBox2.setSelectedIndex(1); // select second

        methodCBox = new JComboBox();

        actions = new ComparisonStatsActions(this);

        getContentPane().setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Set datasets and method for statistical comparison:"));
        topPanel.setBorder(BorderFactory.createEtchedBorder());

        getContentPane().add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        // centerPanel.setBorder(BorderFactory.createEtchedBorder());

        JPanel selectPanel = new JPanel();
        //selectPanel.setBorder(BorderFactory.createEtchedBorder());
        selectPanel.setLayout(new GridLayout(3, 2));
        selectPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

        selectPanel.add(new JLabel("Dataset 1:"));
        selectPanel.add(dataCBox1);
        selectPanel.add(new JLabel("Dataset 2:"));
        selectPanel.add(dataCBox2);
        selectPanel.add(new JLabel("Method:"));
        selectPanel.add(methodCBox);
        centerPanel.add(selectPanel);

        methodCBox.addItem(new ResamplingMethodItem());

        optionsPanel = new JPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Method options"));
        IMethodItem item = (IMethodItem) methodCBox.getSelectedItem();
        optionsPanel.add(Objects.requireNonNull(item).getOptionsPanel());

        centerPanel.add(new JScrollPane(optionsPanel));
        getContentPane().add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        bottomPanel.setLayout(new BorderLayout());
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

        buttonsPanel.add(new JButton(actions.getApplyAction()));
        buttonsPanel.add(new JButton(actions.getCancelAction()));
        bottomPanel.add(buttonsPanel, BorderLayout.EAST);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        getActions().updateEnableState();

        dataCBox1.addItemListener(itemEvent -> getActions().updateEnableState());
        dataCBox2.addItemListener(itemEvent -> getActions().updateEnableState());
        methodCBox.addItemListener(itemEvent -> {
            getActions().updateEnableState();
            optionsPanel.removeAll();
            IMethodItem item1 = (IMethodItem) methodCBox.getSelectedItem();
            optionsPanel.add(item1.getOptionsPanel());
        });

        setVisible(true);
    }

    /**
     * setup the list
     */
    private void loadList(JComboBox cbox) {
        java.util.List<IDirector> projects = ProjectManager.getProjects();

        TreeSet<InputDataItem> items = new TreeSet<>(new InputDataItem());

        for (IDirector project : projects) {
            if (project instanceof Director) {
                Director dir = (Director) project;

                if (!dir.getMainViewer().isLocked() && dir.getDocument().getNumberOfReads() > 0
                        && !dir.getDocument().getMeganFile().isMeganSummaryFile()) {
                    items.add(new InputDataItem(dir));
                }
            }
        }
        // add items in alphabetical order
        for (InputDataItem item : items) {
            cbox.addItem(item);
        }
    }

    /**
     * gets the actions
     *
     * @return actions
     */
    private ComparisonStatsActions getActions() {
        return actions;
    }

    /**
     * execute the comparison
     */
    public void execute() {
        InputDataItem first = (InputDataItem) dataCBox1.getSelectedItem();
        InputDataItem second = (InputDataItem) dataCBox2.getSelectedItem();
        IMethodItem item = (IMethodItem) methodCBox.getSelectedItem();

        if (item != null && !item.isApplicable()) {
            NotificationsInSwing.showError(this, "Statistical method '" + item.getName() + "' is not correctly configured");
            return;
        }

        if (first != null && second != null && item != null) {
            dir.execute("compare" + " stats=" + item.getName() + " pid1=" + first.getPID() + " pid2=" + second.getPID() + " " + item.getOptionsString() + ";", dir.getMainViewer().getCommandManager());
        }
    }

}
