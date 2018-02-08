/*
 *  Copyright (C) 2018 Daniel H. Huson
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
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetLabelStandardCommand extends CommandBase implements ICheckBoxCommand {
    @Override
    public boolean isSelected() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer.getClassLabelAngle() == 0;
    }

    public String getSyntax() {
        return "set labelOrientation={standard|up45|up90|down45|down90};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set labelOrientation=");
        String what = np.getWordMatchesIgnoringCase("standard up45 up90 down45 down90");
        np.matchIgnoreCase(";");

        ChartViewer chartViewer = (ChartViewer) getViewer();

        switch (what) {
            case "standard":
                chartViewer.setClassLabelAngle(0);
                break;
            case "up45":
                chartViewer.setClassLabelAngle(-Math.PI / 4);
                break;
            case "up90":
                chartViewer.setClassLabelAngle(Math.PI / 2);
                break;
            case "down45":
                chartViewer.setClassLabelAngle(Math.PI / 4);
                break;
            case "down90":
                chartViewer.setClassLabelAngle(-Math.PI / 2);
                break;
        }
    }

    public boolean isApplicable() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer.getChartDrawer() != null && chartViewer.getChartDrawer().canShowXAxis();
    }

    public boolean isCritical() {
        return true;
    }


    public void actionPerformed(ActionEvent event) {
        execute("set labelOrientation=standard;");
    }

    public String getName() {
        return "Labels Standard";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Category labels drawn standard";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("LabelsStandard16.gif");
    }
}

