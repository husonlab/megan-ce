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
package megan.importblast.commands;

import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetLongReadsCommand extends CommandBase implements ICheckBoxCommand {

    public boolean isSelected() {
        ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();

        return importBlastDialog != null && importBlastDialog.isLongReads();
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        ImportBlastDialog dialog = (ImportBlastDialog) getParent();
        dialog.setLongReads(!isSelected());
        if (dialog.isLongReads()) {
            dialog.setLcaAlgorithm(Document.DEFAULT_LCA_ALGORITHM_LONG_READS);
            dialog.getMaxNumberOfMatchesPerReadField().setEnabled(false);
            dialog.getMaxNumberOfMatchesPerReadLabel().setEnabled(false);
            dialog.setReadAssignmentMode(Document.DEFAULT_READ_ASSIGNMENT_MODE_LONG_READS);
        } else {
            dialog.setLcaAlgorithm(Document.DEFAULT_LCA_ALGORITHM_SHORT_READS);
            dialog.getMaxNumberOfMatchesPerReadField().setEnabled(true);
            dialog.getMaxNumberOfMatchesPerReadLabel().setEnabled(true);
            dialog.setReadAssignmentMode(Document.DEFAULT_READ_ASSIGNMENT_MODE_SHORT_READS);
        }
    }

    public boolean isApplicable() {
        return true;
    }

    public static final String NAME = "Long Reads";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Parse and bin long reads and contigs using MEGAN-LR features";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

