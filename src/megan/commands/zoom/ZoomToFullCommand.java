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
 * zoom to full
 * Daniel Huson, 2005
 */
public class ZoomToFullCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "zoom what={fit|full|selection}";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("zoom what=");
        final String what = np.getWordMatchesIgnoringCase("fit full selected");
        np.matchIgnoreCase(";");

        if (getViewer() instanceof ViewerBase) {
            final ViewerBase viewer = (ViewerBase) getViewer();
            if (what.equalsIgnoreCase("fit")) {
                viewer.fitGraphToWindow();
                //viewer.trans.setScaleY(0.14); // no idea why this was here...
            } else if (what.equalsIgnoreCase("full")) {
                viewer.fitGraphToWindow();
                viewer.trans.setScaleY(1);
            } else { // selection
                viewer.zoomToSelection();
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("zoom what=full;");
    }

    public String getName() {
        return "Fully Expand";
    }

    public String getDescription() {
        return "Expand tree";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/AlignJustifyVertical16.gif");
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase;
    }

    public boolean isCritical() {
        return true;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

