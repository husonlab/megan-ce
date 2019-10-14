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
package megan.chart.commandtemplates;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.chart.data.IChartData;
import megan.chart.data.IPlot2DData;
import megan.chart.drawers.CoOccurrenceDrawer;
import megan.chart.drawers.Plot2DDrawer;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * sets the chart drawer
 * Daniel Huson, 4.2015
 */
public class SetChartDrawerSpecificCommand extends CommandBase implements ICheckBoxCommand {
    private final String chartDrawerName;
    private final String displayName;

    public SetChartDrawerSpecificCommand(String chartDrawerName) {
        this.chartDrawerName = chartDrawerName;
        displayName = Basic.fromCamelCase(chartDrawerName);
    }

    public boolean isSelected() {
        return getViewer() != null && ((ChartViewer) getViewer()).getChartDrawerName().equals(chartDrawerName);
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        execute("set chartDrawer=" + chartDrawerName + ";");
    }

    public boolean isApplicable() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        if (chartViewer != null) {
            switch (chartDrawerName) {
                case Plot2DDrawer.NAME:
                    return (chartViewer.getChartData() instanceof IPlot2DData);
                case CoOccurrenceDrawer.NAME:
                    return (chartViewer.getChartData() instanceof IChartData && chartViewer.getChartData().getNumberOfSeries() > 1);
                default:
                    return (chartViewer.getChartData() instanceof IChartData);
            }
        }
        return false;
    }

    private static String getName(String displayName) {
        return "Use " + displayName;
    }

    public String getName() {
        return getName(displayName);
    }

    public String getDescription() {
        return "Set chart to " + displayName;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon(chartDrawerName + "16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}
