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
import jloda.util.Pair;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.data.IChartData;
import megan.chart.data.IData;
import megan.chart.data.IPlot2DData;
import megan.chart.drawers.BarChartDrawer;
import megan.chart.drawers.MultiChartDrawer;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.LabelsJList;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

public class SortAlphabeticallyCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set sort=<up|down|alphabetically|alphaBackward>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set sort=");
        String which = np.getWordMatchesIgnoringCase("up down alphabetically alphaBackward");
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();
        LabelsJList list = chartViewer.getActiveLabelsJList();
        LinkedList<String> disabled = new LinkedList<>();
        disabled.addAll(list.getDisabledLabels());

        if (which.equalsIgnoreCase("up") || which.equalsIgnoreCase("down")) {
            final int direction = (which.equalsIgnoreCase("up") ? -1 : 1);

            SortedSet<Pair<Number, String>> sorted = new TreeSet<>(new Comparator<Pair<Number, String>>() {
                public int compare(Pair<Number, String> pair1, Pair<Number, String> pair2) {
                    if (pair1.get1().doubleValue() > pair2.get1().doubleValue())
                        return -direction;
                    if (pair1.get1().doubleValue() < pair2.get1().doubleValue())
                        return direction;
                    return pair1.get2().compareTo(pair2.get2());
                }
            });


            if (list == chartViewer.getSeriesList()) {
                IData chartData = chartViewer.getChartData();
                for (String label : list.getAllLabels()) {
                    Number value;
                    if (chartViewer.getChartData() instanceof IChartData)
                        value = ((IChartData) chartData).getTotalForSeries(label);
                    else
                        value = ((IPlot2DData) chartData).getRangeX().get2();
                    if (value == null)
                        value = 0;
                    sorted.add(new Pair<>(value, label));
                }
                LinkedList<String> labels = new LinkedList<>();
                for (Pair<Number, String> pair : sorted) {
                    labels.add(pair.get2());
                }
                list.sync(labels, list.getLabel2ToolTips(), true);
            } else if (chartViewer.getChartData() instanceof IChartData) {
                IChartData chartData = (IChartData) chartViewer.getChartData();
                for (String label : list.getAllLabels()) {
                    Number value = chartData.getTotalForClass(label);
                    sorted.add(new Pair<>(value, label));
                }
                LinkedList<String> labels = new LinkedList<>();
                for (Pair<Number, String> pair : sorted) {
                    labels.add(pair.get2());
                }
                list.sync(labels, list.getLabel2ToolTips(), true);
            }
        } else {
            final int direction = (which.equalsIgnoreCase("alphabetically") ? 1 : -1);

            SortedSet<String> sorted = new TreeSet<>(new Comparator<String>() {
                public int compare(String s, String s1) {
                    return direction * s.compareToIgnoreCase(s1);
                }
            });
            sorted.addAll(list.getAllLabels());
            list.sync(sorted, list.getLabel2ToolTips(), true);
        }
        list.disableLabels(disabled);
        list.fireSyncToViewer();

        if (chartViewer.getChartDrawer() instanceof MultiChartDrawer) {
            MultiChartDrawer multiChartDrawer = (MultiChartDrawer) chartViewer.getChartDrawer();
            if (multiChartDrawer.getBaseDrawer() instanceof BarChartDrawer)
                multiChartDrawer.updateView();
        } else
            chartViewer.getChartDrawer().updateView();
    }

    public void actionPerformed(ActionEvent event) {
        execute("set sort=alphabetically;");
    }

    public boolean isApplicable() {
        return getViewer() != null;
    }

    public String getName() {
        return "Sort Alphabetically";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Sort the list of entries alphabetically";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("SortAlpha16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

