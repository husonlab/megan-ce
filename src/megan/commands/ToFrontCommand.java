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
package megan.commands;

import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirectableViewer;
import jloda.gui.director.IDirector;
import jloda.gui.director.ProjectManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.util.WindowUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * bring window to front
 * Daniel Huson, 2.2011
 */
public class ToFrontCommand extends CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("toFront");
        String fileName = null;
        if (np.peekMatchIgnoreCase("file")) {
            np.matchIgnoreCase("file=");
            fileName = np.getWordFileNamePunctuation();
        }
        np.matchIgnoreCase(";");

        IDirectableViewer viewer = null;
        if (fileName == null) {
            viewer = getViewer();
        } else {
            for (IDirector iDir : ProjectManager.getProjects()) {
                final Director dir = (Director) iDir;
                final String aName = dir.getDocument().getMeganFile().getFileName();
                if (aName != null && aName.equals(fileName)) {
                    viewer = dir.getMainViewer();
                    break;
                }
            }
        }

        if (viewer != null) {
            WindowUtilities.toFront(viewer.getFrame());
        } else if (getParent() != null && getParent() instanceof Window) {
            WindowUtilities.toFront((Window) getParent());
        }
    }

    public boolean isApplicable() {
        return getViewer() != null || (getParent() != null && getParent() instanceof Window);
    }

    public boolean isCritical() {
        return false;
    }

    public String getSyntax() {
        return "toFront [file=name];";
    }

    public void actionPerformed(ActionEvent event) {
        execute(getSyntax());
    }

    public String getName() {
        return "To Front";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Bring window to front";
    }
}

