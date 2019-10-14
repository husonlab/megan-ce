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
import jloda.swing.director.IDirectableViewer;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.chart.RarefactionPlot;
import megan.chart.gui.ChartViewer;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.util.WindowUtilities;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ShowChartRareFactionCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show rarefaction data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        Director dir = getDir();

        ChartViewer chartViewer;

        np.matchIgnoreCase("show rarefaction");
        String data = "taxonomy";
        if (np.peekMatchIgnoreCase("data")) {
            np.matchIgnoreCase("data=");
            data = np.getWordMatchesIgnoringCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        }
        np.matchIgnoreCase(";");

        chartViewer = (RarefactionPlot) dir.getViewerByClassName(RarefactionPlot.getClassName(data));
        if (chartViewer == null) {
            chartViewer = new RarefactionPlot(dir, (ClassificationViewer) dir.getViewerByClassName(data));
            getDir().addViewer(chartViewer);
        } else {
            chartViewer.sync();
            chartViewer.updateView(Director.ALL);
        }
        WindowUtilities.toFront(chartViewer);
    }

    public void actionPerformed(ActionEvent event) {
        IDirectableViewer viewer = getViewer();
        String data;
        if (viewer instanceof ClassificationViewer)
            data = viewer.getClassName();
        else
            return;
        execute("show rarefaction data=" + data + ";");
    }

    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfReads() > 0 && getViewer() instanceof ClassificationViewer;

    }

    public String getName() {
        return "Rarefaction Analysis...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("RareFaction16.gif");
    }

    public String getDescription() {
        return "Compute and chart a rarefaction curve based on the leaves of the tree shown in the viewer";
    }

    public boolean isCritical() {
        return true;
    }
}

