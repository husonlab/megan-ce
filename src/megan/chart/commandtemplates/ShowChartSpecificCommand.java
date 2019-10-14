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
package megan.chart.commandtemplates;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * shows a chart
 * Daniel Huson, 4.2015
 */
public class ShowChartSpecificCommand extends CommandBase implements ICommand {
    private final String chartDrawerName;
    private final String displayName;

    public ShowChartSpecificCommand(String chartDrawerName) {
        this.chartDrawerName = chartDrawerName;
        displayName = Basic.fromCamelCase(chartDrawerName);
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        execute("show chart drawer=" + chartDrawerName + " data='" + getViewer().getClassName() + "';");
    }

    public boolean isApplicable() {
        return ((Director) getDir()).getDocument().getNumberOfReads() > 0 && getViewer() instanceof ClassificationViewer;

    }

    private static String getName(String displayName) {
        return "Show " + displayName + "";
    }

    public String getName() {
        return getName(displayName);
    }

    public String getDescription() {
        return "Show " + displayName;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon(chartDrawerName + "16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}
