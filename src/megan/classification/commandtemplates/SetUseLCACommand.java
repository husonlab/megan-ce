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
import megan.classification.IdMapper;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Set whether to use LCA for specific fViewer
 * Daniel Huson, 1.2016
 */
public class SetUseLCACommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "set useLCA={true|false} cName=<name>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set useLCA=");
        boolean useLCA = np.getBoolean();
        np.matchIgnoreCase("cName=");
        final String cName = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        np.matchIgnoreCase(";");

        ProgramProperties.put(cName + "UseLCA", useLCA);

        if (getParent() instanceof ImportBlastDialog) {
            final IdMapper mapper = ClassificationManager.get(cName, true).getIdMapper();
            ((ImportBlastDialog) getParent()).getCommandManager().execute("use cViewer=" + cName + " state=" + mapper.hasActiveAndLoaded() + ";");
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
        return "Set whether to use the LCA algorithm for assignment (alternative: best hit)";
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
