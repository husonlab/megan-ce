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
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.chart.ComparisonPlot;
import megan.chart.gui.ChartViewer;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.util.WindowUtilities;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class ShowComparisonPlotCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show comparisonPlot [data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "]};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        Director dir = getDir();

        ChartViewer chartViewer;

        np.matchIgnoreCase("show comparisonPlot");
        String data = "taxonomy";
        if (np.peekMatchIgnoreCase("data")) {
            np.matchIgnoreCase("data=");
            data = np.getWordMatchesIgnoringCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        }
        np.matchIgnoreCase(";");

        chartViewer = (ComparisonPlot) dir.getViewerByClassName(ComparisonPlot.getClassName(data));
        if (chartViewer == null) {
            ClassificationViewer classificationViewer = (ClassificationViewer) dir.getViewerByClassName(ClassificationViewer.getClassName(data));
            if (classificationViewer == null)
                throw new IOException("Viewer must be open for chart to operate");
            chartViewer = new ComparisonPlot(dir, classificationViewer);
            getDir().addViewer(chartViewer);
        } else {
            chartViewer.sync();
        }
        WindowUtilities.toFront(chartViewer);
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getSelectedNodes().size() == 0)
            executeImmediately("select nodes=leaves;");

        if (getViewer() instanceof MainViewer) {
            execute("show comparisonPlot data=" + Classification.Taxonomy + ";");
        } else if (getViewer() instanceof ClassificationViewer) {
            execute("show comparisonPlot data=" + getViewer().getClassName() + ";");
        }
    }

    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfSamples() > 1 && getViewer() instanceof ClassificationViewer;

    }

    public String getName() {
        return "Comparison Plot...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Plot2D16.gif");
    }

    public String getDescription() {
        return "Plot pairwise comparison of assignments to classes";
    }

    public boolean isCritical() {
        return true;
    }
}

