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
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class UseContaminantsFilterCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        final ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
        return importBlastDialog != null && importBlastDialog.isUseContaminantsFilter();
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        final ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
        if (isSelected() || importBlastDialog.getContaminantsFileName() != null)
            importBlastDialog.setUseContaminantsFilter(!isSelected());
        getCommandManager().updateEnableState(ListContaminantsCommand.NAME);

        if (isSelected())
            NotificationsInSwing.showInformation("Contaminants file: " + importBlastDialog.getContaminantsFileName());
    }

    public boolean isApplicable() {
        final ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
        return importBlastDialog.getContaminantsFileName() != null && (new File(importBlastDialog.getContaminantsFileName())).exists();
    }

    public final static String NAME = "Use Contaminants Filter";

    public String getName() {
        return NAME;
    }

    private final static String DESCRIPTION = "Filter reads that align to any of the provided contaminants";

    public String getDescription() {
        return DESCRIPTION;
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

