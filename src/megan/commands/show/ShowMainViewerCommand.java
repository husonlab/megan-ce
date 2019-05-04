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

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.util.WindowUtilities;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ShowMainViewerCommand extends megan.commands.CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=mainViewer;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        if (ProgramProperties.isUseGUI()) {
            final MainViewer viewer = (MainViewer) getDir().getViewerByClass(MainViewer.class);
            WindowUtilities.toFront(viewer.getFrame());
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Main Viewer...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("TaxonomyViewer16.gif");
    }

    public String getDescription() {
        return "Brings the main viewer to the front";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}


