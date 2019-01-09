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

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.chart.drawers.MultiChartDrawer;
import megan.chart.drawers.WordCloudDrawer;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RectangularWordCloudCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer.isUseRectangleShape();
    }

    public String getSyntax() {
        return "set shape={square|rectangle};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set shape=");
        String shape = np.getWordMatchesIgnoringCase("rectangle square");
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();
        chartViewer.setUseRectangleShape(shape.equalsIgnoreCase("rectangle"));
        if (chartViewer.getChartDrawer() instanceof MultiChartDrawer)
            chartViewer.getChartDrawer().forceUpdate(); // need to recompute word clouds
    }

    public void actionPerformed(ActionEvent event) {
        execute("set shape='" + (isSelected() ? "square" : "rectangle") + "';");
    }

    public boolean isApplicable() {
        ChartViewer viewer = (ChartViewer) getViewer();
        return viewer.getChartDrawer() instanceof WordCloudDrawer
                || (viewer.getChartDrawer() instanceof MultiChartDrawer && ((MultiChartDrawer) viewer.getChartDrawer()).getBaseDrawer() instanceof WordCloudDrawer);
    }

    public String getName() {
        return "Rectangle Shape";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Set wordcloud shape";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}

