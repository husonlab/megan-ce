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
package megan.chart.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.util.ChooseColorDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.ChartColorManager;
import megan.chart.gui.ChartViewer;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * show all
 * Daniel Huson, 7.2012
 */
public class SetColorCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        ChartViewer viewer = (ChartViewer) getViewer();
        ChartColorManager chartColors = viewer.getChartColorManager();

        np.matchIgnoreCase("set color=");
        Color color = np.getColor();

        String series = null;
        if (np.peekMatchIgnoreCase("series")) {
            np.matchIgnoreCase("series=");
            series = np.getLabelRespectCase();
        }
        String className = null;
        if (np.peekMatchIgnoreCase("class")) {
            np.matchIgnoreCase("class=");
            className = np.getLabelRespectCase();
        }
        np.matchIgnoreCase(";");

        if (series != null)
            chartColors.setSampleColor(series, color);
        if (className != null)
            chartColors.setClassColor(className, color);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set color=<color> [series=<name>] [class=<name>];";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        ChartViewer viewer = (ChartViewer) getViewer();
        ChartColorManager chartColors = viewer.getChartColorManager();

        if (viewer.isSeriesTabSelected()) {
            int count = viewer.getChartData().getChartSelection().getSelectedSeries().size();
            Set<String> done = new HashSet<>();
            for (String series : viewer.getChartData().getChartSelection().getSelectedSeries()) {
                final ColorChooser chooser = new ColorChooser(viewer, "Change color for series '" + series + "'", series, null, chartColors, count - done.size() >= 2);
                if (chooser.getResult() == null)
                    return; // canceled
                else {
                    if (chooser.isApplyToAll()) {
                        Set<String> toDo = new HashSet<>(viewer.getChartData().getChartSelection().getSelectedSeries());
                        toDo.removeAll(done);
                        for (String label : toDo) {
                            chartColors.setSampleColor(label, chooser.getResult());
                        }
                        viewer.repaint();
                        break;
                    } else {
                        Color color = chartColors.getSampleColor(series);
                        if (!chooser.getResult().equals(color)) {
                            chartColors.setSampleColor(series, chooser.getResult());
                            done.add(series);
                            viewer.repaint();
                        }
                    }
                }
            }
            ((Director) getDir()).getDocument().setDirty(true);
            getDir().notifyUpdateViewer(IDirector.TITLE);

        } else // target equals classes
        {
            int count = viewer.getChartData().getChartSelection().getSelectedClasses().size();
            Set<String> done = new HashSet<>();
            for (String className : viewer.getChartData().getChartSelection().getSelectedClasses()) {
                final ColorChooser chooser = new ColorChooser(viewer, "Change color for class '" + className + "'",
                        viewer.getChartData().getDataSetName(), className, chartColors, count - done.size() >= 2);
                if (chooser.getResult() == null)
                    return; // canceled
                else {
                    if (chooser.isApplyToAll()) {
                        Set<String> toDo = new HashSet<>(viewer.getChartData().getChartSelection().getSelectedClasses());
                        toDo.removeAll(done);
                        for (String label : toDo) {
                            chartColors.setClassColor(viewer.getClass2HigherClassMapper().get(label), chooser.getResult());
                        }
                        viewer.repaint();
                        break;
                    } else {
                        Color color = chartColors.getClassColor(viewer.getClass2HigherClassMapper().get(className));
                        if (!chooser.getResult().equals(color)) {
                            chartColors.setClassColor(viewer.getClass2HigherClassMapper().get(className), chooser.getResult());
                            done.add(className);
                            viewer.repaint();
                        }
                    }
                }
            }
            ((Director) getDir()).getDocument().setDirty(true);
            getDir().notifyUpdateViewer(IDirector.TITLE);
        }
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Color...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the color of a series or class";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("YellowSquare16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        ChartViewer viewer = (ChartViewer) getViewer();
        return (viewer.isSeriesTabSelected() && viewer.getChartData().getChartSelection().getSelectedSeries().size() > 0)
                || (!viewer.isSeriesTabSelected() && viewer.getChartData().getChartSelection().getSelectedClasses().size() > 0);
    }

    static class ColorChooser extends JDialog {
        Color result = null;
        private boolean applyToAll = false;

        /**
         * constructor
         *
         * @param viewer
         * @param message
         * @param series
         * @param className
         * @param colors
         */
        ColorChooser(ChartViewer viewer, String message, String series, String className, ChartColorManager colors, boolean showApplyToAll) {
            super(viewer.getFrame());
            setSize(500, 400);
            setModal(true);
            setLocationRelativeTo(viewer.getFrame());
            getContentPane().setLayout(new BorderLayout());
            JLabel header = new JLabel(message);
            getContentPane().add(header, BorderLayout.NORTH);
            if (className == null)
                result = colors.getSampleColor(series);
            else
                result = colors.getClassColor(viewer.getClass2HigherClassMapper().get(className));
            final JColorChooser colorChooser = ChooseColorDialog.colorChooser;
            colorChooser.setColor(result);
            getContentPane().add(colorChooser, BorderLayout.CENTER);

            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            JButton doneButton = new JButton(new AbstractAction("Done") {
                public void actionPerformed(ActionEvent actionEvent) {
                    result = null;
                    ColorChooser.this.setVisible(false);
                }
            });
            doneButton.setToolTipText("Close the dialog");
            buttons.add(Box.createHorizontalGlue());
            buttons.add(doneButton);

            if (showApplyToAll) {
                JButton applyToAllButton = new JButton(new AbstractAction("Apply To All") {
                    public void actionPerformed(ActionEvent actionEvent) {
                        result = colorChooser.getColor();
                        applyToAll = true;
                        ColorChooser.this.setVisible(false);
                    }
                });
                applyToAllButton.setToolTipText("Apply to all remaining selected items");
                buttons.add(applyToAllButton);
            }
            JButton applyButton = new JButton(new AbstractAction("Apply") {
                public void actionPerformed(ActionEvent actionEvent) {
                    result = colorChooser.getColor();
                    ColorChooser.this.setVisible(false);
                }
            });
            applyButton.setToolTipText("Apply to current item");
            buttons.add(applyButton);

            getContentPane().add(buttons, BorderLayout.SOUTH);
            setVisible(true);
        }

        Color getResult() {
            return result;
        }

        boolean isApplyToAll() {
            return applyToAll;
        }
    }
}
