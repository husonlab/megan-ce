/*
 * DisableDefaultTaxaCommand.java Copyright (C) 2022 Daniel H. Huson
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
package megan.commands.mapping;

import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class DisableDefaultTaxaCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) {
    }

    public void actionPerformed(ActionEvent event) {
        execute("show window=message;disable taxa=default;list taxa=disabled;");
    }

    public boolean isApplicable() {
        return getViewer() instanceof MainViewer && !getDoc().getMeganFile().isReadOnly();
    }

    public String getName() {
        return "Disable Default";
    }

    public String getDescription() {
        return "Reset disabling to default value, ignoring a lot of unclassified taxa";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}

