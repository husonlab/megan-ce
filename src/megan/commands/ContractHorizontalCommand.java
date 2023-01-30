/*
 * ContractHorizontalCommand.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.ScrollPaneAdjuster;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;
import megan.clusteranalysis.ClusterViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ContractHorizontalCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "contract direction={horizontal|vertical};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("contract direction=");
        String direction = np.getWordMatchesIgnoringCase("horizontal vertical");
        np.matchIgnoreCase(";");
        if (getViewer() instanceof ChartViewer) {
            ChartViewer chartViewer = (ChartViewer) getViewer();

            if (direction.equalsIgnoreCase("horizontal"))
                chartViewer.zoom(1f / 1.2f, 1, chartViewer.getZoomCenter());
            else if (direction.equalsIgnoreCase("vertical"))
                chartViewer.zoom(1, 1f / 1.2f, chartViewer.getZoomCenter());
        } else {
            GraphView viewer;
            if (getViewer() instanceof GraphView)
                viewer = (GraphView) getViewer();
            else if (getViewer() instanceof ClusterViewer)
                viewer = ((ClusterViewer) getViewer()).getGraphView();
            else
                return;

            if (direction.equals("horizontal")) {
                double scale = 1.2 * viewer.trans.getScaleX();
                if (scale >= ViewerBase.XMIN_SCALE) {
                    ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                    viewer.trans.composeScale(1 / 1.2, 1);
                    spa.adjust(true, false);
                }
            } else {
                double scale = 1.2 * viewer.trans.getScaleY();
                if (scale >= ViewerBase.YMIN_SCALE) {
                    ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                    viewer.trans.composeScale(1, 1 / 1.2);
                    spa.adjust(false, true);
                }
            }
        }
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
        return "Contract view horizontally";
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

