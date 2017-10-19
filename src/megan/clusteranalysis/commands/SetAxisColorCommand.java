/*
 *  Copyright (C) 2015 Daniel H. Huson
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
package megan.clusteranalysis.commands;

import jloda.gui.ChooseColorDialog;
import jloda.gui.commands.ICommand;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.gui.PCoATab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * set axes color
 * Daniel Huson, 10.2017
 */
public class SetAxisColorCommand extends CommandBase implements ICommand {

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("setColor target=");
        final String target = np.getWordMatchesIgnoringCase("axes biplot triplot");
        np.matchIgnoreCase("color=");
        final Color color = np.getColor();
        np.matchIgnoreCase(";");

        final PCoATab pCoATab = getViewer().getPcoaTab();

        switch (target) {
            case "axes":
                pCoATab.setAxesColor(color);
                ProgramProperties.put("PCoAAxesColor", color);
                break;
            case "biplot":
                pCoATab.setBiPlotColor(color);
                ProgramProperties.put("PCoABiPlotColor", color);
                break;
            case "triplot":
                pCoATab.setTriPlotColor(color);
                ProgramProperties.put("PCoATriPlotColor", color);
                break;
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "setColor target={axes|biplot|triplot} color=<color>;";
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
        return getViewer().getSelectedComponent() == getViewer().getPcoaTab();
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Axes Color...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set axes color";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        final PCoATab pCoATab = getViewer().getPcoaTab();
        final Color color = ChooseColorDialog.showChooseColorDialog(getViewer().getFrame(), "Choose axes color", pCoATab.getAxesColor());
        if (color != null && !color.equals(pCoATab.getAxesColor()))
            execute("setColor target=axes color=" + color.getRed() + " " + color.getGreen() + " " + color.getBlue() + ";");
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
