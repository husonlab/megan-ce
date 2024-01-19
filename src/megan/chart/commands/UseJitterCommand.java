/*
 * UseJitterCommand.java Copyright (C) 2024 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.chart.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.chart.drawers.Plot2DDrawer;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class UseJitterCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        if (chartViewer.getChartDrawer() instanceof Plot2DDrawer) {
            Plot2DDrawer drawer = (Plot2DDrawer) chartViewer.getChartDrawer();
            for (String series : chartViewer.getChartData().getSeriesNames()) {
                if (drawer.isUseJitter(series))
                    return true;
            }
        }
        return false;
    }

    public String getSyntax() {
        return "set jitter={false|true};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set jitter=");
        boolean value = np.getBoolean();
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();
        if (chartViewer.getChartDrawer() instanceof Plot2DDrawer) {
            Plot2DDrawer drawer = (Plot2DDrawer) chartViewer.getChartDrawer();
            for (String series : chartViewer.getChartData().getChartSelection().getSelectedSeries()) {
                drawer.setUseJitter(series, value);
            }
        }
        chartViewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("set jitter='" + (!isSelected()) + "';");
    }

    public boolean isApplicable() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return (chartViewer.getChartDrawer() instanceof Plot2DDrawer) && chartViewer.getChartData().getChartSelection().getSelectedSeries().size() > 0;
    }

    public String getName() {
        return "Use Jitter";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Jitter points in 2D plot to make them more visible";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}

