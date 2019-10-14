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


import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.director.IViewerWithLegend;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * show legend
 * Daniel Huson, 3.2013
 */
public class ShowLegendCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        return getViewer() instanceof IViewerWithLegend &&
                (((IViewerWithLegend) getViewer()).getShowLegend().equals("horizontal")
                        || ((IViewerWithLegend) getViewer()).getShowLegend().equals("vertical"));
    }

    public String getSyntax() {
        return "show legend={horizontal|vertical|none};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show legend=");
        String which = np.getWordMatchesIgnoringCase("horizontal vertical none");
        np.matchIgnoreCase(";");
        if (getViewer() instanceof IViewerWithLegend) {
            IViewerWithLegend viewer = (IViewerWithLegend) getViewer();
            viewer.setShowLegend(which);
            if (getViewer() != null) {
                getViewer().updateView(IDirector.ENABLE_STATE);
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof IViewerWithLegend) {
            String legend = ((IViewerWithLegend) getViewer()).getShowLegend();
            switch (legend) {
                case "none":
                    executeImmediately("show legend=horizontal;");
                    break;
                case "horizontal":
                    executeImmediately("show legend=vertical;");
                    break;
                case "vertical":
                    executeImmediately("show legend=none;");
                    break;
            }
        }
    }

    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfSamples() > 1 && getViewer() instanceof IViewerWithLegend;
    }

    public String getName() {
        return "Show Legend";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Legend16.gif");
    }

    public String getDescription() {
        return "Show horizontal or vertical legend, or hide";
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_J, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }
}
