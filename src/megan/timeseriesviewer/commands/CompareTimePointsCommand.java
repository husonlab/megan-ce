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

package megan.timeseriesviewer.commands;

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.timeseriesviewer.TimeSeriesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * * compare time points command
 * * Daniel Huson, 6.2015
 */
public class CompareTimePointsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        final TimeSeriesViewer viewer = (TimeSeriesViewer) getViewer();
        final Document doc = viewer.getDirector().getDocument();

        final Map<String, ArrayList<String>> timePoint2samples = new TreeMap<>();
        final ArrayList<String> timePointOrder = new ArrayList<>();

        final String timePointAttribute = viewer.getTimePointsDefiningAttribute();

        for (String sample : viewer.getDataJTable().getSelectedSamples()) {
            final Object timePoint = doc.getSampleAttributeTable().get(sample, timePointAttribute);
            if (timePoint != null) {
                ArrayList<String> list = timePoint2samples.get(timePoint.toString());
                if (list == null) {
                    list = new ArrayList<>();
                    timePoint2samples.put(timePoint.toString(), list);
                    timePointOrder.add(timePoint.toString());
                }
                list.add(sample);
            }
        }

        if (timePoint2samples.size() > 0) {
            final StringBuilder buf = new StringBuilder();
            buf.append("compare title='").append(Basic.replaceFileSuffix(doc.getTitle(), "")).append("-").append(timePointAttribute).append("'");
            for (String timePoint : timePointOrder) {
                buf.append(" name='").append(timePointAttribute).append(":").append(timePoint).append("' samples=");
                for (String sample : timePoint2samples.get(timePoint)) {
                    buf.append(" '").append(sample).append("'");
                }
            }
            buf.append(";");
            execute(buf.toString());
        }
    }

    public boolean isApplicable() {
        return ((TimeSeriesViewer) getViewer()).getDataJTable().getSelectedSamples().size() > 0;
    }

    public final static String NAME = "Compare Time Points...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Compare all selected time points in a new document";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;

    }
}
