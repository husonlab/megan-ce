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
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.chart.drawers.CoOccurrenceDrawer;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * set max radius
 * Daniel Huson, 2.2015
 */
public class SetMaximumRadiusCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set maxRadius=<number>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set maxRadius=");
        int maxRadius = np.getInt(1, 1000);
        np.matchIgnoreCase(";");

        ChartViewer chartViewer = (ChartViewer) getViewer();
        if (chartViewer.getChartDrawer() instanceof CoOccurrenceDrawer) {
            ((CoOccurrenceDrawer) chartViewer.getChartDrawer()).setMaxRadius(maxRadius);
            ProgramProperties.put("COMaxRadius", maxRadius);
        }
        chartViewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        if (chartViewer.getChartDrawer() instanceof CoOccurrenceDrawer) {

            String result = JOptionPane.showInputDialog(chartViewer.getFrame(), "Set Max Radius", ((CoOccurrenceDrawer) chartViewer.getChartDrawer()).getMaxRadius());
            if (result != null && Basic.isInteger(result))
                execute("set maxRadius='" + result + "';");
        }
    }

    public boolean isApplicable() {

        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer != null && chartViewer.getChartDrawer() instanceof CoOccurrenceDrawer;
    }

    public String getName() {
        return "Set Max Radius...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }

    public String getDescription() {
        return "Set the max radius to use for nodes in a co-occurrence plot";
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
