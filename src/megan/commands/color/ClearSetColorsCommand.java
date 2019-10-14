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
package megan.commands.color;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ClearSetColorsCommand extends CommandBase implements ICommand {


    public String getSyntax() {
        return "clearSetColors;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        ((Director) getDir()).getDocument().getChartColorManager().clearChangedColors();
    }

    public void actionPerformed(ActionEvent event) {
        if (JOptionPane.showConfirmDialog(getViewer().getFrame(), "This will discard all individually set colors, proceed?", "Question", JOptionPane.YES_NO_CANCEL_OPTION)
                != JOptionPane.YES_OPTION)
            return; // answered no or cancle
        execute(getSyntax());
    }

    public boolean isApplicable() {
        return ((Director) getDir()).getDocument().getChartColorManager().hasChangedColors();
    }

    private static final String NAME = "Clear Set Colors";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Clear all individually set colors";
    }

    public ImageIcon getIcon() {
        return null;
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

