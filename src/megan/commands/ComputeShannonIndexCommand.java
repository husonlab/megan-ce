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

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.util.DiversityIndex;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ComputeShannonIndexCommand extends CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("compute index=");
        final String indexName = np.getWordMatchesIgnoringCase(DiversityIndex.SHANNON + " " + DiversityIndex.SIMPSON_RECIPROCAL);
        np.matchIgnoreCase(";");

        String message;
        final Document doc = getDir().getDocument();
        int numberSelectedNodes = ((ViewerBase) getViewer()).getNumberSelectedNodes();

        if (indexName.equalsIgnoreCase(DiversityIndex.SHANNON)) {
            message = "Shannon-Weaver index for " + doc.getNumberOfSamples() + " samples based on " + numberSelectedNodes + " selected nodes:\n"
                    + DiversityIndex.computeShannonWeaver((ViewerBase) getViewer(), doc.getProgressListener());
        } else if (indexName.equalsIgnoreCase(DiversityIndex.SIMPSON_RECIPROCAL)) {
            message = "Simpson's reciprocal index for " + doc.getNumberOfSamples() + " samples based on " + numberSelectedNodes + " selected nodes:\n"
                    + DiversityIndex.computeSimpsonReciprocal((ViewerBase) getViewer(), doc.getProgressListener());
        } else {
            message = "Error: Unknown index: " + indexName;
        }
        System.err.println(message);
        NotificationsInSwing.showInformation(getViewer().getFrame(), message);
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getNumberSelectedNodes() > 0 && getDir().getDocument().getNumberOfSamples() > 0;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "compute index={" + DiversityIndex.SHANNON + "|" + DiversityIndex.SIMPSON_RECIPROCAL + "};";
    }

    public void actionPerformed(ActionEvent event) {
        execute("compute index=" + DiversityIndex.SHANNON + ";");
    }

    public String getName() {
        return "Shannon-Weaver Index...";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Compute the Shannon-Weaver diversity index";
    }
}


