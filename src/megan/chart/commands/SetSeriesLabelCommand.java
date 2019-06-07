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
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * set series label
 * Daniel Huson, 6.2012
 */
public class SetSeriesLabelCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set seriesLabel=<label>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set seriesLabel=");
        String label = np.getLabelRespectCase();
        np.matchIgnoreCase(";");

        ChartViewer chartViewer = (ChartViewer) getViewer();
        chartViewer.getChartData().setSeriesLabel(label);
        chartViewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        ChartViewer viewer = (ChartViewer) getViewer();

        String result = JOptionPane.showInputDialog(viewer.getFrame(), "Set Series Label", viewer.getChartData().getSeriesLabel());
        if (result != null)
            execute("set seriesLabel='" + result.replaceAll("'", "\"") + "';");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Set Series Label...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }

    public String getDescription() {
        return "Set the series label of the data set";
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
