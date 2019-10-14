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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ContractHorizontalCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "contract direction={horizontal|vertical};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("contract direction=");
        String direction = np.getWordMatchesIgnoringCase("horizontal vertical");
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();

        if (direction.equalsIgnoreCase("horizontal"))
            chartViewer.zoom(1f / 1.2f, 1, chartViewer.getZoomCenter());
        else if (direction.equalsIgnoreCase("vertical"))
            chartViewer.zoom(1, 1f / 1.2f, chartViewer.getZoomCenter());
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("contract direction=horizontal;");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Contract Horizontal";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("ContractHorizontal16.gif");
    }

    public String getDescription() {
        return "Contract view";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}

