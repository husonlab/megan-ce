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
import jloda.util.parse.NexusStreamParser;
import megan.chart.drawers.RadialSpaceFillingTreeDrawer;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class RotateLeftCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "rotate direction={left|right};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("rotate direction=");
        String direction = np.getWordMatchesIgnoringCase("left right");
        np.matchIgnoreCase(";");
        ChartViewer chartViewer = (ChartViewer) getViewer();
        if (chartViewer.getChartDrawer() instanceof RadialSpaceFillingTreeDrawer) {
            RadialSpaceFillingTreeDrawer drawer = (RadialSpaceFillingTreeDrawer) chartViewer.getChartDrawer();
            if (direction.equalsIgnoreCase("left")) {
                drawer.setAngleOffset(drawer.getAngleOffset() + 5);
                drawer.repaint();
            } else {
                drawer.setAngleOffset(drawer.getAngleOffset() - 5);
                drawer.repaint();
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("rotate direction=left;");
    }

    public boolean isApplicable() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer.getChartDrawer() instanceof RadialSpaceFillingTreeDrawer;
    }

    public String getName() {
        return "Rotate Left";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Rotate Radial Chart left";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }
}

