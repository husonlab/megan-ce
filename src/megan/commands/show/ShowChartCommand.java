/*
 * ShowChartCommand.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */
package megan.commands.show;

import jloda.swing.commands.ICommand;
import jloda.swing.util.PopupMenu;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.chart.AttributesChart;
import megan.chart.FViewerChart;
import megan.chart.MetadataChart;
import megan.chart.TaxaChart;
import megan.chart.data.ChartCommandHelper;
import megan.chart.drawers.BarChartDrawer;
import megan.chart.drawers.DrawerManager;
import megan.chart.gui.ChartViewer;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.util.WindowUtilities;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * show chart command
 * Daniel Huson, 6.2012
 */
public class ShowChartCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "show chart drawer={" + Basic.toString(DrawerManager.getAllSupportedChartDrawers(), ",") + "} data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "|attributes|metadata};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();

        np.matchIgnoreCase("show chart drawer=");
        final String drawerType = np.getWordMatchesRespectingCase(Basic.toString(DrawerManager.getAllSupportedChartDrawers(), " "));
        np.matchIgnoreCase("data=");
        final String data = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " ") + " attributes metadata");
        np.matchIgnoreCase(";");

        ChartViewer chartViewer = null;
        if (data.equalsIgnoreCase(Classification.Taxonomy) || data.equalsIgnoreCase("taxa"))    // taxa for legacy
        {
            chartViewer = (TaxaChart) dir.getViewerByClass(TaxaChart.class);
            if (chartViewer == null) {
                chartViewer = new TaxaChart(dir);
                getDir().addViewer(chartViewer);
                if (drawerType.equals(BarChartDrawer.NAME) && getDir().getDocument().getNumberOfSamples() > 1)
                    chartViewer.setTranspose(true);
                chartViewer.chooseDrawer(drawerType);
            } else {
                chartViewer.sync();
                chartViewer.chooseDrawer(drawerType);
                chartViewer.updateView(Director.ALL);
            }
        } else if (data.equalsIgnoreCase("attributes")) {
            chartViewer = (AttributesChart) dir.getViewerByClass(AttributesChart.class);
            if (chartViewer == null) {
                chartViewer = new AttributesChart(dir);
                getDir().addViewer(chartViewer);
                chartViewer.chooseDrawer(drawerType);
            } else {
                chartViewer.sync();
                chartViewer.chooseDrawer(drawerType);
                chartViewer.updateView(Director.ALL);
            }
        } else if (data.equalsIgnoreCase("metadata")) {
            chartViewer = (MetadataChart) dir.getViewerByClass(AttributesChart.class);
            if (chartViewer == null) {
                chartViewer = new MetadataChart(dir, dir.getMainViewer());
                getDir().addViewer(chartViewer);
                chartViewer.chooseDrawer(drawerType);
            } else {
                chartViewer.sync();
                chartViewer.chooseDrawer(drawerType);
                chartViewer.updateView(Director.ALL);
            }
        } else if (ClassificationManager.getAllSupportedClassifications().contains(data.toUpperCase())) {
            chartViewer = (FViewerChart) dir.getViewerByClassName(FViewerChart.getClassName(data));
            if (chartViewer == null) {
                ClassificationViewer classificationViewer = (ClassificationViewer) dir.getViewerByClassName(ClassificationViewer.getClassName(data));
                if (classificationViewer == null)
                    throw new IOException(data + " viewer must be open for " + data + " chart to operate");
                chartViewer = new FViewerChart(dir, classificationViewer);
                getDir().addViewer(chartViewer);
                if (getDir().getDocument().getNumberOfSamples() == 1)
                    chartViewer.setTranspose(true);
                chartViewer.chooseDrawer(drawerType);
            } else {
                chartViewer.sync();
                chartViewer.chooseDrawer(drawerType);
                chartViewer.updateView(Director.ALL);
            }
        }
        WindowUtilities.toFront(chartViewer);
    }

    public boolean isCritical() {
        return true;
    }

    public void actionPerformed(ActionEvent event) {
        final JPopupMenu popupMenu = new PopupMenu(this, ChartCommandHelper.getOpenChartMenuString(), getCommandManager());
        final Point location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, getViewer().getFrame());
        popupMenu.show(getViewer().getFrame(), location.x, location.y);
    }

    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfReads() > 0 && getViewer() instanceof ClassificationViewer;
    }

    public String getName() {
        return "Chart...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("BarChart16.gif");
    }

    public String getDescription() {
        return "Show chart";
    }
}
