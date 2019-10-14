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
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ShowLegendCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return isApplicable() && !chartViewer.getShowLegend().equals("none");
    }

    public String getSyntax() {
        return "show chartLegend={horizontal|vertical|none};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show chartLegend=");
        String value = np.getWordMatchesIgnoringCase("horizontal vertical none");
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();
        chartViewer.setShowLegend(value);
    }

    public void actionPerformed(ActionEvent event) {
        String legend = ((ChartViewer) getViewer()).getShowLegend();
        switch (legend) {
            case "none":
                executeImmediately("show chartLegend=horizontal;");
                break;
            case "horizontal":
                executeImmediately("show chartLegend=vertical;");
                break;
            case "vertical":
                executeImmediately("show chartLegend=none;");
                break;
        }
    }

    public boolean isApplicable() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer.getChartDrawer() != null && chartViewer.getChartDrawer().canShowLegend();
    }

    public String getName() {
        return "Show Legend";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_J, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }

    public String getDescription() {
        return "Show chart legend";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Legend16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

