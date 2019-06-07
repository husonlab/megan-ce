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
package megan.commands.zoom;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * zoom to selection
 * Daniel Huson, 2005
 */
public class ZoomToSelectionCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    @Override
    public void apply(NexusStreamParser np) throws Exception {

    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("zoom what=selected;");
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getSelectedNodes().size() > 0;
    }

    public String getName() {
        return "Zoom To Selection";
    }

    public String getDescription() {
        return "Zoom to the selection";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/AlignCenter16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}

