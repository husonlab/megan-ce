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
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * Turns use of named functional viewer on or off
 * Daniel Huson, 4.2015
 */
public class SetAnalyse4ViewerCommand extends CommandBase implements ICheckBoxCommand {
    private final String cName;

    public SetAnalyse4ViewerCommand(String cName) {
        this.cName = cName;
    }

    public boolean isSelected() {
        return ProgramProperties.get("Use" + cName, false);
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("use cViewer=" + cName + " state=" + (!isSelected()) + ";");
    }

    public boolean isApplicable() {
        return true;
    }

    public static String getName(String cName) {
        return "Analyze " + cName + " content"; // use Analyze not Analyse to differ from old commands
    }

    public String getName() {
        return getName(cName);
    }

    public String getDescription() {
        return "Analyse the " + cName + " content of the sample";
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
