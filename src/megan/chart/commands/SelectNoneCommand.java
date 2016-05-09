/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.chart.data.IChartData;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * select all series
 * Daniel Huson, 7.2012
 */
public class SelectNoneCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("deselect what=");
        List<String> list = np.getTokensRespectCase(null, ";");

        ChartViewer viewer = (ChartViewer) getViewer();
        if (viewer.isSeriesTabSelected()) {
            for (String name : list) {
                if (name.equalsIgnoreCase("all"))
                    viewer.getChartSelection().setSelectedSeries(viewer.getSeriesList().getAllLabels(), false);
                else if (name.equalsIgnoreCase("none"))
                    viewer.getChartSelection().setSelectedSeries(viewer.getSeriesList().getAllLabels(), true);
                else
                    viewer.getChartSelection().setSelectedSeries(name, false);
            }
            viewer.repaint();
        } else {
            for (String name : list) {
                if (name.equalsIgnoreCase("all"))
                    viewer.getChartSelection().setSelectedClass(viewer.getClassesList().getAllLabels(), false);
                else if (name.equalsIgnoreCase("none"))
                    viewer.getChartSelection().setSelectedClass(viewer.getClassesList().getAllLabels(), true);
                else
                    viewer.getChartSelection().setSelectedClass(name, false);
            }
            viewer.repaint();
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "deselect what={all|none|<name...>};";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("deselect what=all;");
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Select None";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Deselect all";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | java.awt.event.InputEvent.SHIFT_MASK);
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return false;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        ChartViewer viewer = (ChartViewer) getViewer();
        return (viewer.isSeriesTabSelected() && viewer.getChartSelection().getSelectedSeries().size() > 0)
                || (!viewer.isSeriesTabSelected() && viewer.getChartData() instanceof IChartData
                && (viewer.getChartSelection().getSelectedClasses().size()) > 0);
    }
}
