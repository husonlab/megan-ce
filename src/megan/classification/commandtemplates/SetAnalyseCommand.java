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
package megan.classification.commandtemplates;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Turns use of named functional viewer on or off
 * Daniel Huson, 4.2015
 */
public class SetAnalyseCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "use cViewer=<name> state={true|false};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("use cViewer=");
        String cName = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        np.matchIgnoreCase("state=");
        boolean select = np.getBoolean();
        np.matchIgnoreCase(";");
        ProgramProperties.put("Use" + cName, select);
    }

    public void actionPerformed(ActionEvent event) {
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return null;
    }

    public String getDescription() {
        return "Determine whether to perform a specific functional analysis";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}
