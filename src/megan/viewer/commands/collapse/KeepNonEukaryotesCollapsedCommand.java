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
package megan.viewer.commands.collapse;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class KeepNonEukaryotesCollapsedCommand extends CommandBase implements ICheckBoxCommand {
    @Override
    public boolean isSelected() {
        return ProgramProperties.get("KeepOthersCollapsed", "none").equals("eukaryotes");
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        execute("set keepOthersCollapsed=" + (isSelected() ? "none" : "eukaryotes") + ";");
    }

    public boolean isApplicable() {
        return getViewer() instanceof MainViewer;
    }

    public String getName() {
        return "Keep Non-Eukaryotes Collapsed";
    }

    public String getDescription() {
        return "Keep all non-eukaryotes collapsed when uncollapsing by rank or level";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}

