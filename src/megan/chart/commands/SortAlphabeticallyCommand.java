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
import jloda.swing.util.ResourceManager;
import jloda.util.Pair;
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
import java.util.*;

public class SortAlphabeticallyCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set sort={up|down|alphabetically|alphaBackward|enabled};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set sort=");
        final String which = np.getWordMatchesIgnoringCase("up down alphabetically alphaBackward enabled");
        np.matchIgnoreCase(";");

        final ChartViewer chartViewer = (ChartViewer) getViewer();
        final LabelsJList list = chartViewer.getActiveLabelsJList();
        final LinkedList<String> disabled = new LinkedList<>(list.getDisabledLabels());

        switch (which.toLowerCase()) {
            case "up":
            case "down": {
                final int direction = (which.equalsIgnoreCase("up") ? -1 : 1);

                final SortedSet<Pair<Number, String>> sorted = new TreeSet<>((pair1, pair2) -> {
                    if (pair1.get1().doubleValue() > pair2.get1().doubleValue())
                        return -direction;
                    if (pair1.get1().doubleValue() < pair2.get1().doubleValue())
                        return direction;
                    return pair1.get2().compareTo(pair2.get2());
                });
                if (list == chartViewer.getSeriesList()) {
                    final IData chartData = chartViewer.getChartData();
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
                    final LinkedList<String> labels = new LinkedList<>();
                    for (Pair<Number, String> pair : sorted) {
                        labels.add(pair.get2());
                    }
                    list.sync(labels, list.getLabel2ToolTips(), true);
                } else if (chartViewer.getChartData() instanceof IChartData) {
                    final IChartData chartData = (IChartData) chartViewer.getChartData();
                    for (String label : list.getAllLabels()) {
                        Number value = chartData.getTotalForClass(label);
                        sorted.add(new Pair<>(value, label));
                    }
                    final LinkedList<String> labels = new LinkedList<>();
                    for (Pair<Number, String> pair : sorted) {
                        labels.add(pair.get2());
                    }
                    list.sync(labels, list.getLabel2ToolTips(), true);
                }
                break;
            }
            case "alphabetically":
            case "alphabackward": {
                final int direction = (which.equalsIgnoreCase("alphabetically") ? 1 : -1);
                final SortedSet<String> sorted = new TreeSet<>((s, s1) -> direction * s.compareToIgnoreCase(s1));
                sorted.addAll(list.getAllLabels());
                list.sync(sorted, list.getLabel2ToolTips(), true);
                break;
            }
            case "enabled": {
                final String[] array = list.getAllLabels().toArray(new String[0]);
                final Set<String> disabledSet = new HashSet<>(disabled);
                Arrays.sort(array, (a, b) -> {
                    if (!disabledSet.contains(a) && disabledSet.contains(b))
                        return -1;
                    else if (disabledSet.contains(a) && !disabledSet.contains(b))
                        return 1;
                    else
                        return 0;
                });
                list.sync(Arrays.asList(array), list.getLabel2ToolTips(), true);
                break;
            }
        }
        list.disableLabels(disabled);
        list.fireSyncToViewer();

        if (chartViewer.getChartDrawer() instanceof MultiChartDrawer) {
            MultiChartDrawer multiChartDrawer = (MultiChartDrawer) chartViewer.getChartDrawer();
            if (multiChartDrawer.getBaseDrawer() instanceof BarChartDrawer)
                multiChartDrawer.updateView();
        }
    }

    public void actionPerformed(ActionEvent event) {
        execute("set sort=alphabetically;");
    }

    public boolean isApplicable() {
        final LabelsJList list = ((ChartViewer) getViewer()).getActiveLabelsJList();
        return list != null && list.isEnabled() && !list.isDoClustering();
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

