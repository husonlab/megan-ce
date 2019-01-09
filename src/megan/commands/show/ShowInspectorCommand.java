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
package megan.commands.show;

import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.inspector.InspectorWindow;
import megan.util.WindowUtilities;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ShowInspectorCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=inspector;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final Director dir = getDir();
        InspectorWindow inspectorWindow = (InspectorWindow) dir.getViewerByClass(InspectorWindow.class);
        if (inspectorWindow == null) {
            inspectorWindow = (InspectorWindow) dir.addViewer(new InspectorWindow(dir));
        }
        WindowUtilities.toFront(inspectorWindow.getFrame());
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getDoc().getMeganFile().hasDataConnector();
    }

    public String getName() {
        return "Inspector Window...";
    }

    public String getDescription() {
        return "Open inspector window";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Inspector16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

