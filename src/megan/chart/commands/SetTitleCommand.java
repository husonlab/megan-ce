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

public class SetTitleCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set title=<string>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set title=");
        String title = np.getWordRespectCase();
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();
        chartViewer.setChartTitle(title);
        chartViewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        ChartViewer chartViewer = (ChartViewer) getViewer();

        String result = JOptionPane.showInputDialog(chartViewer, "Set Title", chartViewer.getChartTitle());
        if (result != null)
            executeImmediately("set title='" + result.replaceAll("'", "\"") + "';");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Set Title...";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Set the chart title";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

