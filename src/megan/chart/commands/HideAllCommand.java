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
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * show all
 * Daniel Huson, 7.2012
 */
public class HideAllCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        ChartViewer viewer = (ChartViewer) getViewer();

        np.matchIgnoreCase("hide what=");
        String what = np.getWordMatchesIgnoringCase("all none selected unselected");
        String target;
        if (np.peekMatchIgnoreCase("target=")) {
            np.matchIgnoreCase("target=");
            target = np.getWordMatchesIgnoringCase("active series classes");
        } else {
            if (viewer.isSeriesTabSelected())
                target = "series";
            else
                target = "classes";
        }
        np.matchIgnoreCase(";");

        if (target.equals("series")) {
            if (what.equalsIgnoreCase("none")) {
                viewer.getSeriesList().enableLabels(viewer.getSeriesList().getAllLabels());
            } else if (what.equalsIgnoreCase("selected")) {
                viewer.getSeriesList().disableLabels(viewer.getSeriesList().getSelectedLabels());
            } else if (what.equalsIgnoreCase("unselected")) {
                Set<String> labels = new HashSet<>();
                labels.addAll(viewer.getSeriesList().getAllLabels());
                labels.removeAll(viewer.getSeriesList().getSelectedLabels());
                viewer.getSeriesList().disableLabels(labels);
            } else  // all
            {
                viewer.getSeriesList().disableLabels(viewer.getSeriesList().getAllLabels());
            }
            viewer.getChartData().setEnabledSeries(viewer.getSeriesList().getEnabledLabels());
        } else // target equals classes
        {
            if (viewer.getChartData() instanceof IChartData) {
                if (what.equalsIgnoreCase("none")) {
                    viewer.getClassesList().enableLabels(viewer.getClassesList().getAllLabels());
                } else if (what.equalsIgnoreCase("selected")) {
                    viewer.getClassesList().disableLabels(viewer.getClassesList().getSelectedLabels());
                } else if (what.equalsIgnoreCase("unselected")) {
                    Set<String> labels = new HashSet<>();
                    labels.addAll(viewer.getClassesList().getAllLabels());
                    labels.removeAll(viewer.getClassesList().getSelectedLabels());
                    viewer.getClassesList().disableLabels(labels);
                } else  // all
                {
                    viewer.getClassesList().disableLabels(viewer.getClassesList().getAllLabels());
                }
                ((IChartData) viewer.getChartData()).setEnabledClassNames(viewer.getClassesList().getEnabledLabels());
            }
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "hide what={all|none|selected} [target={active|series|classes}];";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("hide what=all;");
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Hide All";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Hide data items";
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
        return true;
    }
}
