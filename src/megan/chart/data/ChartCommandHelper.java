/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.chart.data;

import jloda.gui.commands.ICommand;
import megan.chart.commandtemplates.SetChartDrawerCommand;
import megan.chart.commandtemplates.SetChartDrawerSpecificCommand;
import megan.chart.commandtemplates.ShowChartSpecificCommand;
import megan.chart.drawers.DrawerManager;

import java.util.ArrayList;
import java.util.Collection;

/**
 * helper class for setting up chart commands
 * Daniel Huson, 4.2015
 */
public class ChartCommandHelper {
    /**
     * open chart menu string for main viewers
     *
     * @return open chart menu string
     */
    public static String getOpenChartMenuString() {
        final StringBuilder buf = new StringBuilder();
        for (String name : DrawerManager.getAllSupportedChartDrawers()) {
            buf.append((new ShowChartSpecificCommand(name)).getName()).append(";");
        }
        return buf.toString();
    }

    /**
     * gets the menu string for opening all registered viewers, is used in GUIConfiguration of chart viewer
     *
     * @return menu string
     */
    public static String getSetDrawerCommandString() {
        final StringBuilder buf = new StringBuilder();
        for (String name : DrawerManager.getAllSupportedChartDrawers()) {
            buf.append((new SetChartDrawerSpecificCommand(name)).getName()).append(";");
        }
        return buf.toString();
    }

    /**
     * get all commands needed to draw charts, used in chart viewer
     *
     * @return commands
     */
    public static Collection<ICommand> getChartDrawerCommands() {
        final ArrayList<ICommand> commands = new ArrayList<>();
        commands.add(new SetChartDrawerCommand());
        for (String name : DrawerManager.getAllSupportedChartDrawers()) {
            commands.add(new SetChartDrawerSpecificCommand(name));
            commands.add(new ShowChartSpecificCommand(name));
        }
        return commands;
    }
}
