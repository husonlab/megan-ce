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
import jloda.gui.director.ProjectManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * select all series
 * Daniel Huson, 7.2012
 */
public class SelectAllCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("select what=");
        final List<String> list = new LinkedList<>();
        if (np.peekMatchAnyTokenIgnoreCase("all none previous"))
            list.add(np.getWordMatchesIgnoringCase("all none previous"));
        else list.addAll(np.getTokensRespectCase(null, ";"));

        final ChartViewer viewer = (ChartViewer) getViewer();
        if (viewer.isSeriesTabSelected()) {
            for (String name : list) {
                if (name.equalsIgnoreCase("all"))
                    viewer.getChartSelection().setSelectedSeries(viewer.getSeriesList().getAllLabels(), true);
                else if (name.equalsIgnoreCase("none"))
                    viewer.getChartSelection().clearSelectionSeries();
                else if (name.equals("previous"))
                    viewer.getChartSelection().setSelectedSeries(ProjectManager.getPreviouslySelectedNodeLabels(), true);
                else
                    viewer.getChartSelection().setSelectedSeries(name, true);
            }
        } else {
            for (String name : list) {
                if (name.equalsIgnoreCase("all"))
                    viewer.getChartSelection().setSelectedClass(viewer.getClassesList().getAllLabels(), true);
                else if (name.equalsIgnoreCase("none"))
                    viewer.getChartSelection().clearSelectionClasses();
                else if (name.equals("previous"))
                    viewer.getChartSelection().setSelectedClass(ProjectManager.getPreviouslySelectedNodeLabels(), true);
                else
                    viewer.getChartSelection().setSelectedClass(name, true);
            }
        }

        for (String name : list) {
            if (name.equalsIgnoreCase("all")) {
                viewer.getChartSelection().setSelectedAttribute(viewer.getNumericalAttributes(), true);
            } else if (name.equalsIgnoreCase("none")) {
                viewer.getChartSelection().clearSelectionAttributes();
            } else if (name.equals("previous"))
                viewer.getChartSelection().setSelectedAttribute(ProjectManager.getPreviouslySelectedNodeLabels(), true);
            else
                viewer.getChartSelection().setSelectedAttribute(name, true);
        }
        viewer.repaint();
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "select what={all|none|previous|<name...>};";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("select what=all;");
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Select All";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Selection";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
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
        return true;
    }
}
