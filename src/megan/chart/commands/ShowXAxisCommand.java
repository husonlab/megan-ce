/*
 * ShowXAxisCommand.java Copyright (C) 2023 Daniel H. Huson
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
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * show x axis
 * Daniel Huson, 6.2012
 */
public class ShowXAxisCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return isApplicable() && chartViewer.isShowXAxis();
    }

    public String getSyntax() {
        return "show xAxis={true|false};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show xAxis=");
        boolean show = np.getBoolean();
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();
        chartViewer.setShowXAxis(show);
        chartViewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("show xAxis=" + (!isSelected()) + ";");
    }

    public boolean isApplicable() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer.getChartDrawer() != null && chartViewer.getChartDrawer().canShowXAxis();
    }

    public String getName() {
        return "Show x-Axis";
    }

    public String getDescription() {
        return "Show the x-axis";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
