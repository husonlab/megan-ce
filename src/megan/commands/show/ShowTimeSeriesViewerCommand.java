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
import megan.core.Director;
import megan.timeseriesviewer.TimeSeriesViewer;
import megan.util.WindowUtilities;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ShowTimeSeriesViewerCommand extends megan.commands.CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=timeSeriesViewer;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        TimeSeriesViewer viewer = (TimeSeriesViewer) getDir().getViewerByClass(TimeSeriesViewer.class);
        if (viewer == null) {
            try {
                viewer = new TimeSeriesViewer(getViewer().getFrame(), getDir());
                getDir().addViewer(viewer);
                WindowUtilities.toFront(viewer.getFrame());
            } catch (Exception e) {
                Basic.caught(e);
            }
        } else {
            viewer.updateView(Director.ALL);
            WindowUtilities.toFront(viewer.getFrame());
        }
    }

    public void actionPerformed(ActionEvent event) {
        execute(getSyntax());
    }

    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfSamples() > 0 && getViewer() instanceof ClassificationViewer;

    }

    public String getName() {
        return "Time Series Viewer...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("TimeSeries16.gif");
    }

    public String getDescription() {
        return "Opens the Time Series Viewer";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

