/*
 * ShowYAxisCommand.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.chart.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * show Y axis
 * Daniel Huson, 6.2012
 */
public class ShowYAxisCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return isApplicable() && chartViewer.isShowYAxis();
    }

    public String getSyntax() {
        return "show yAxis={true|false};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show yAxis=");
        boolean show = np.getBoolean();
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();
        chartViewer.setShowYAxis(show);
        chartViewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("show yAxis=" + (!isSelected()) + ";");
    }

    public boolean isApplicable() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer.getChartDrawer() != null && chartViewer.getChartDrawer().canShowYAxis();
    }

    public String getName() {
        return "Show y-Axis";
    }

    public String getDescription() {
        return "Show the y-axis";
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
