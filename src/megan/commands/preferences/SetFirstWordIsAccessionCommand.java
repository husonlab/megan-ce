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

import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.IdParser;
import megan.commands.CommandBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetFirstWordIsAccessionCommand extends CommandBase implements ICheckBoxCommand {

    @Override
    public boolean isSelected() {
        return ProgramProperties.get(IdParser.PROPERTIES_FIRST_WORD_IS_ACCESSION, true);
    }

    public String getSyntax() {
        return "set firstWordIsAccession={true|false};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set firstWordIsAccession=");
        boolean state = np.getBoolean();
        np.matchIgnoreCase(";");
        ProgramProperties.put(IdParser.PROPERTIES_FIRST_WORD_IS_ACCESSION, state);
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("set firstWordIsAccession=" + (!isSelected()) + ";");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "First Word Is Accession";
    }

    public String getDescription() {
        return "Parse first word of reference header-line as accession number in accordance with 2016 NCBI formatting";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}
