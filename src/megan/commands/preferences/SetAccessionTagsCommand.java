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
package megan.commands.preferences;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.IdParser;
import megan.commands.CommandBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetAccessionTagsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set accessionTags=<word ...>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set accessionTags=");
        String[] tags = np.getWordRespectCase().split("\\s+");
        np.matchIgnoreCase(";");
        ProgramProperties.put(IdParser.PROPERTIES_ACCESSION_TAGS, tags);
    }

    public void actionPerformed(ActionEvent event) {
        String[] tags = ProgramProperties.get(IdParser.PROPERTIES_ACCESSION_TAGS, IdParser.ACCESSION_TAGS);
        String result = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter tag(s) for identifying accession numbers (separated by spaces):", Basic.toString(tags, " "));
        if (result != null)
            executeImmediately("set accessionTags='" + result + "';");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Set Accession Tags";
    }

    public String getDescription() {
        return "Set tags used to identify accession numbers";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}
