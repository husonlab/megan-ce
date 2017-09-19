/*
 *  Copyright (C) 2015 Daniel H. Huson
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
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.data.IChartData;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ScaleByTotalSampleCountCommand extends CommandBase implements ICheckBoxCommand {

    public boolean isSelected() {
        final ChartViewer chartViewer = (ChartViewer) getViewer();
        return (chartViewer.getChartData() instanceof IChartData) && ((IChartData) chartViewer.getChartData()).isUseTotalSize();
    }

    public String getSyntax() {
        return "set usePercentOfTotal=<bool>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set usePercentOfTotal=");
        boolean use = np.getBoolean();
        np.matchIgnoreCase(";");
        final ChartViewer chartViewer = (ChartViewer) getViewer();
        ((IChartData) chartViewer.getChartData()).setUseTotalSize(use);
    }

    public void actionPerformed(ActionEvent event) {
        execute("set usePercentOfTotal=" + (isApplicable() && !isSelected()) + ";");
    }

    public boolean isApplicable() {
        final ChartViewer chartViewer = (ChartViewer) getViewer();
        return (chartViewer.getChartData() != null && chartViewer.getChartData() instanceof IChartData) && ((IChartData) chartViewer.getChartData()).hasTotalSize()
                && chartViewer.getChartDrawer() != null && chartViewer.getChartDrawer().getScalingType() == ChartViewer.ScalingType.PERCENT;
    }

    public String getName() {
        return "Use Percent of Total";
    }

    public String getDescription() {
        return "When selected, 'Percent' and 'Z-score' are based on total sample size, otherwise they are only based on total counts for selected nodes.";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Percent16.gif");
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

