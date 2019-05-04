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
package megan.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseColorDialog;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ChooseHighLightColorCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set comparisonHighlightColor=<number>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set comparisonHighlightColor=");
        Color color = new Color(np.getInt());
        np.matchIgnoreCase(";");
        ProgramProperties.put(MeganProperties.PVALUE_COLOR, color);
        NodeDrawer.pvalueColor = color;
        ((MainViewer) getViewer()).repaint();
    }

    public void actionPerformed(ActionEvent event) {
        Color previous = ProgramProperties.get(MeganProperties.PVALUE_COLOR, Color.ORANGE);
        Color color = ChooseColorDialog.showChooseColorDialog(getViewer().getFrame(), "Choose comparison highlight color", previous);
        executeImmediately("set comparisonHighlightColor=" + color.getRGB() + ";");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Set Highlight Color...";
    }

    public String getDescription() {
        return "Set the pairwise comparison highlight color";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return getDoc().getNumberOfSamples() == 2;
    }
}
