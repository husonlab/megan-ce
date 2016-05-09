/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetUseReadMagnitudesCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        final ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
        return importBlastDialog != null && importBlastDialog.isUseReadMagnitudes();
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
        importBlastDialog.setUseReadMagnitudes(!isSelected());
    }

    public boolean isApplicable() {
        return true;
    }

    public final static String NAME = "Use Read Magnitudes";

    public String getName() {
        return NAME;
    }

    public final static String DESCRIPTION = "Use read magnitudes that are defined in read headers as weight= or magnitude=";

    public String getDescription() {
        return DESCRIPTION;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Preferences16.gif");
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

