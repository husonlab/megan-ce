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
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Set use id parsing
 * Daniel Huson, 10.2015
 */
public class SetUseIdParsingCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set idParsing={true|false} cName=<name> [prefix=<prefix prefix ...>];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set idParsing=");
        boolean useIdParsing = np.getBoolean();

        np.matchIgnoreCase("cName=");
        final String cName = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));

        ProgramProperties.put(cName + "ParseIds", useIdParsing);

        if (np.peekMatchIgnoreCase("prefix")) {
            np.matchIgnoreCase("prefix=");
            String prefix = np.getWordRespectCase();
            if (!prefix.equals(";")) {
                ProgramProperties.put(cName + "Tags", prefix.split("\\s+"));
                np.matchIgnoreCase(";");
            }
        }
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
        return "Set ID prefixes and use ID parsing";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
