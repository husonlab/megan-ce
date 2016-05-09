/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.clusteranalysis.commands.zoom;

import jloda.graphview.ScrollPaneAdjuster;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ContractVerticalCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "contract direction=vertical;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        ViewerBase viewer = (ViewerBase) ((ClusterViewer) getViewer()).getGraphView();
        if (viewer != null) {
            double scale = 1.2 * viewer.trans.getScaleY();
            if (scale >= 0.0001) {
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                if (viewer.trans.getLockXYScale()) {
                    viewer.trans.composeScale(1 / 1.2, 1 / 1.2);
                    spa.adjust(true, true);
                } else {
                    viewer.trans.composeScale(1, 1 / 1.2);
                    spa.adjust(false, true);
                }
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Contract Vertical";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("ContractVertical16.gif");
    }

    public String getDescription() {
        return "Contract view vertically";
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

