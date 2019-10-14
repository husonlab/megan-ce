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
package megan.timeseriesviewer;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.RememberingComboBox;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.Table;
import megan.core.Director;
import megan.core.SampleAttributeTable;
import megan.core.SelectionSet;
import megan.main.MeganProperties;
import megan.timeseriesviewer.commands.CompareSamplesCommand;
import megan.timeseriesviewer.commands.CompareSubjectsCommand;
import megan.timeseriesviewer.commands.CompareTimePointsCommand;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

/**
 * times series viewer
 * Daniel Huson, 6.2015
 */
public class TimeSeriesViewer extends JFrame implements IDirectableViewer {
    private final Director dir;
    private boolean uptoDate;
    private boolean locked = false;

    private final MenuBar menuBar;

    private final CommandManager commandManager;

    private final SubjectJTable subjectJTable;
    private final DataJTable dataJTable;

    private final RememberingComboBox subjectDefiningAttribute;
    private final RememberingComboBox timepointsDefiningAttribute;

    private final SelectionSet.SelectionListener selectionListener;


    /**
     * constructor
     *
     * @param dir
     */
    public TimeSeriesViewer(JFrame parent, final Director dir) {
        this.dir = dir;

        commandManager = new CommandManager(dir, this, new String[]{"megan.commands", "megan.timeseriesviewer.commands"}, !ProgramProperties.isUseGUI());

        setTitle();

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setIconImages(ProgramProperties.getProgramIconImages());

        int[] geometry = ProgramProperties.get(getClassName() + "Geometry", new int[]{100, 100, 800, 600});
        if (parent != null)
            getFrame().setLocationRelativeTo(parent);
        else
            getFrame().setLocation(geometry[0] + (dir.getID() - 1) * 20, geometry[1] + (dir.getID() - 1) * 20);
        getFrame().setSize(geometry[2], geometry[3]);

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);

        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        final ToolBar toolBar = new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), commandManager);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        subjectJTable = new SubjectJTable(this);
        dataJTable = new DataJTable(this);


        final JScrollPane seriesScrollPane = new JScrollPane(subjectJTable.getJTable());
        seriesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        seriesScrollPane.setAutoscrolls(true);
        seriesScrollPane.setWheelScrollingEnabled(false);
        seriesScrollPane.getViewport().setBackground(new Color(240, 240, 240));

        final JScrollPane dataScrollPane = new JScrollPane(dataJTable.getJTable());
        dataScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        dataScrollPane.setAutoscrolls(true);
        dataScrollPane.setWheelScrollingEnabled(false);
        dataScrollPane.getViewport().setBackground(new Color(240, 240, 240));

        seriesScrollPane.getVerticalScrollBar().setModel(dataScrollPane.getVerticalScrollBar().getModel());

        selectionListener = (labels, selected) -> dataJTable.selectSamples(labels, selected);
        dir.getDocument().getSampleSelection().addSampleSelectionListener(selectionListener);


        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, seriesScrollPane, dataScrollPane);
        splitPane.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
        splitPane.setOneTouchExpandable(true);
        splitPane.setEnabled(true);
        splitPane.setResizeWeight(0);
        splitPane.setDividerLocation(150);
        splitPane.setLastDividerLocation(0);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        dataJTable.getJTable().setFillsViewportHeight(true);

        {
            final JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
            buttonsPanel.setBorder(new EmptyBorder(2, 5, 2, 5));

            final JButton applyButton = new JButton(new AbstractAction("Apply") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setupTable(Objects.requireNonNull(subjectDefiningAttribute.getSelectedItem()).toString(), Objects.requireNonNull(timepointsDefiningAttribute.getSelectedItem()).toString());
                }
            });

            buttonsPanel.add(new JLabel("Subjects:"));

            subjectDefiningAttribute = new RememberingComboBox();
            subjectDefiningAttribute.setMaximumSize(new Dimension(150, 20));
            subjectDefiningAttribute.setEditable(false);
            subjectDefiningAttribute.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    Object a = subjectDefiningAttribute.getSelectedItem();
                    Object b = timepointsDefiningAttribute.getSelectedItem();
                    applyButton.setEnabled(a != null && b != null && ((String) a).length() > 0 && ((String) b).length() > 0
                            && !a.equals(b));
                }
            });
            buttonsPanel.add(subjectDefiningAttribute);
            subjectDefiningAttribute.setToolTipText("Select metadata attribute that defines the different subjects");

            buttonsPanel.add(new JLabel("Time points:"));

            timepointsDefiningAttribute = new RememberingComboBox();
            timepointsDefiningAttribute.setEditable(false);
            timepointsDefiningAttribute.setMaximumSize(new Dimension(150, 20));
            timepointsDefiningAttribute.addItemListener(e -> {
                Object a = subjectDefiningAttribute.getSelectedItem();
                Object b = timepointsDefiningAttribute.getSelectedItem();
                applyButton.setEnabled(a != null && b != null && ((String) a).length() > 0 && ((String) b).length() > 0
                        && !a.equals(b));
            });
            buttonsPanel.add(timepointsDefiningAttribute);
            timepointsDefiningAttribute.setToolTipText("Select metadata attribute that defines the different time points");

            buttonsPanel.add(applyButton);

            mainPanel.add(buttonsPanel, BorderLayout.NORTH);

        }

        {
            JPanel aLine = new JPanel();
            aLine.setLayout(new BoxLayout(aLine, BoxLayout.LINE_AXIS));
            aLine.add(Box.createHorizontalGlue());
            aLine.add(commandManager.getButton(CompareSubjectsCommand.NAME));
            aLine.add(commandManager.getButton(CompareTimePointsCommand.NAME));
            aLine.add(commandManager.getButton(CompareSamplesCommand.NAME));
            mainPanel.add(aLine, BorderLayout.SOUTH);
        }


        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(toolBar, BorderLayout.NORTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().validate();

        commandManager.updateEnableState();

        getFrame().setVisible(true);
    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return this;
    }

    public void updateView(String what) {
        uptoDate = false;
        setTitle();
        commandManager.updateEnableState();
        if (what.equals(IDirector.ALL)) {
            if (getSubjectDefiningAttribute() == null || getTimePointsDefiningAttribute() == null) {
                dataJTable.setDefault();
                dataJTable.updateView();
                subjectJTable.setDefault();
                subjectJTable.updateView();
            }

            final Collection<String> attributes = dir.getDocument().getSampleAttributeTable().getAttributeOrder();

            final Object selectedSeriesDefiningAttribute = subjectDefiningAttribute.getSelectedItem();
            subjectDefiningAttribute.removeAllItems();
            for (String attribute : attributes) {
                if (!attribute.startsWith("@"))
                    subjectDefiningAttribute.addItem(attribute);
            }
            subjectDefiningAttribute.setSelectedItem(selectedSeriesDefiningAttribute);

            final Object selectedTimepointDefiningAttribute = timepointsDefiningAttribute.getSelectedItem();
            timepointsDefiningAttribute.removeAllItems();
            for (String attribute : attributes) {
                if (!attribute.startsWith("@"))
                    timepointsDefiningAttribute.addItem(attribute);
            }
            timepointsDefiningAttribute.setSelectedItem(selectedTimepointDefiningAttribute);
        }
        uptoDate = true;
    }

    public void lockUserInput() {
        locked = true;
        commandManager.setEnableCritical(false);
        menuBar.setEnableRecentFileMenuItems(false);
    }

    public void unlockUserInput() {
        commandManager.setEnableCritical(true);
        menuBar.setEnableRecentFileMenuItems(true);
        locked = false;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return locked;
    }

    public void destroyView() throws CanceledException {
        ProgramProperties.put(getClassName() + "Geometry", new int[]
                {getFrame().getLocation().x, getFrame().getLocation().y, getFrame().getSize().width, getFrame().getSize().height});

        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    private void setTitle() {
        String newTitle = "Time Series Viewer - " + dir.getDocument().getTitle();

        /*
        if (dir.getDocument().isDirty())
            newTitle += "*";
          */

        if (dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!getFrame().getTitle().equals(newTitle)) {
            getFrame().setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "TimesSeriesViewer";
    }

    public Director getDirector() {
        return dir;
    }

    public SubjectJTable getSubjectJTable() {
        return subjectJTable;
    }

    public DataJTable getDataJTable() {
        return dataJTable;
    }

    /**
     * sets up the table based on the chosen series attribute and time point attribute
     *
     * @param seriesAttribute
     * @param timePointAttribute
     */
    private void setupTable(String seriesAttribute, String timePointAttribute) {
        final Table<String, String, java.util.List<String>> seriesAndTimePoint2Samples = new Table<>();

        final ArrayList<String> subjectsOrder = new ArrayList<>();
        final ArrayList<String> timePointOrder = new ArrayList<>();
        final Map<String, Integer> timePoint2MaxCount = new HashMap<>();

        final SampleAttributeTable sampleAttributeTable = dir.getDocument().getSampleAttributeTable();
        for (String sample : dir.getDocument().getSampleNames()) {
            Object series = sampleAttributeTable.get(sample, seriesAttribute);
            if (series == null)
                series = "NA";
            if (!subjectsOrder.contains(series.toString()))
                subjectsOrder.add(series.toString());
            Object timePoint = sampleAttributeTable.get(sample, timePointAttribute);
            if (timePoint == null)
                timePoint = "NA";
            if (!timePointOrder.contains(timePoint.toString()))
                timePointOrder.add(timePoint.toString());

            java.util.List<String> list = seriesAndTimePoint2Samples.get(series.toString(), timePoint.toString());
            if (list == null) {
                list = new ArrayList<>();
                seriesAndTimePoint2Samples.put(series.toString(), timePoint.toString(), list);
            }
            list.add(sample);
            Integer count = timePoint2MaxCount.get(timePoint.toString());
            if (count == null || list.size() > count)
                timePoint2MaxCount.put(timePoint.toString(), list.size());
        }

        int rows = subjectsOrder.size();
        int cols = 0;
        for (Integer count : timePoint2MaxCount.values())
            cols += count;

        dataJTable.setRowsAndCols(rows, cols);
        subjectJTable.setRows(rows);

        BitSet headersAlreadySet = new BitSet();

        int row = 0;
        for (String series : subjectsOrder) {
            int col = 0;
            for (String timePoint : timePointOrder) {
                int countPlaced = 0;
                final Collection<String> samples = seriesAndTimePoint2Samples.get(series, timePoint);
                if (samples != null) {
                    for (String sample : samples) {
                        if (!headersAlreadySet.get(col))
                            dataJTable.setColumnName(col, timePoint);
                        dataJTable.putCell(row, col++, sample);
                        countPlaced++;
                    }
                }
                while (countPlaced < timePoint2MaxCount.get(timePoint)) {
                    if (!headersAlreadySet.get(col))
                        dataJTable.setColumnName(col, timePoint);
                    dataJTable.putCell(row, col++, "NA");
                    countPlaced++;
                }
            }
            row++;
        }
        dataJTable.updateView();
        subjectJTable.updateView();
    }

    public String getSubjectDefiningAttribute() {
        return subjectDefiningAttribute.getCurrentText(false);
    }

    public String getTimePointsDefiningAttribute() {
        return timepointsDefiningAttribute.getCurrentText(false);
    }
}
