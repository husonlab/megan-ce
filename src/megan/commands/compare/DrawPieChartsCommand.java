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
package megan.commands.compare;

import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Document;
import megan.main.MeganProperties;
import megan.viewer.ViewerBase;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class DrawPieChartsCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ViewerBase viewer = (ViewerBase) getViewer();
        return viewer != null && viewer.getNodeDrawer().getStyle() == NodeDrawer.Style.PieChart;
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set nodeDrawer=");
        final ViewerBase viewer = (ViewerBase) getViewer();
        final Document doc = getDoc();
        final String style = np.getWordMatchesIgnoringCase(Basic.toString(NodeDrawer.Style.values(), " "));
        np.matchIgnoreCase(";");
        viewer.getNodeDrawer().setStyle(style, NodeDrawer.Style.Circle);
        viewer.getLegendPanel().setStyle(viewer.getNodeDrawer().getStyle());
        doc.setDirty(true);
        if (doc.getNumberOfSamples() > 1)
            ProgramProperties.put(MeganProperties.COMPARISON_STYLE, style);
        viewer.repaint();
    }

    public String getSyntax() {
        return "set nodeDrawer={" + Basic.toString(NodeDrawer.ScaleBy.values(), "|") + "};";
    }

    public void actionPerformed(ActionEvent event) {
        execute("set nodeDrawer=" + NodeDrawer.Style.PieChart + ";");
    }

    public String getName() {
        return "Draw Pies";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("PieChart16.gif");
    }

    public String getDescription() {
        return "Draw data as pie charts";
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0;
    }
}

