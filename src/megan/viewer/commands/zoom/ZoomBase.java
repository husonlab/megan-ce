/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.viewer.commands.zoom;

import jloda.gui.commands.CommandBase;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ClassificationViewer;

public abstract class ZoomBase extends CommandBase {
    public String getSyntax() {
        return "zoom {fit|full|selection}";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("zoom");
        String what = np.getWordMatchesIgnoringCase("fit full selected");
        if (what.equalsIgnoreCase("fit")) {
            ((ClassificationViewer) getViewer()).fitGraphToWindow();
            ((ClassificationViewer) getViewer()).trans.setScaleY(0.14);
        } else if (what.equalsIgnoreCase("full")) {
            ((ClassificationViewer) getViewer()).fitGraphToWindow();
            ((ClassificationViewer) getViewer()).trans.setScaleY(1);
        } else { // selection
            ((ClassificationViewer) getViewer()).zoomToSelection();
        }
    }

    public boolean isApplicable() {
        return true;
    }

    public boolean isCritical() {
        return true;
    }
}

