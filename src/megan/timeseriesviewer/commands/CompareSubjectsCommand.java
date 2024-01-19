/*
 * CompareSubjectsCommand.java Copyright (C) 2024 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.timeseriesviewer.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.FileUtils;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.timeseriesviewer.TimeSeriesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * * compare subjects command
 * * Daniel Huson, 7.2015
 */
public class CompareSubjectsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    /**
     * parses the given command and executes it
     */
    public void apply(NexusStreamParser np) {
    }

    public void actionPerformed(ActionEvent event) {
        final TimeSeriesViewer viewer = (TimeSeriesViewer) getViewer();
        final Document doc = viewer.getDirector().getDocument();
        final Map<String, ArrayList<String>> subject2samples = new TreeMap<>();
        final ArrayList<String> subjectOrder = new ArrayList<>();


        final String subjectDefiningAttribute = viewer.getSubjectDefiningAttribute();

        for (String sample : viewer.getDataJTable().getSelectedSamples()) {
            final Object subject = doc.getSampleAttributeTable().get(sample, subjectDefiningAttribute);
            if (subject != null) {
                ArrayList<String> list = subject2samples.get(subject.toString());
                if (list == null) {
                    list = new ArrayList<>();
                    subject2samples.put(subject.toString(), list);
                    subjectOrder.add(subject.toString());
                }
                list.add(sample);
            }
        }

        if (subject2samples.size() > 0) {
            final StringBuilder buf = new StringBuilder();
			buf.append("compare title='").append(FileUtils.replaceFileSuffix(doc.getTitle(), "")).append("-").append(subjectDefiningAttribute).append("'");
            for (String subject : subjectOrder) {
                buf.append(" name='").append(subjectDefiningAttribute).append(":").append(subject).append("' samples=");
                for (String sample : subject2samples.get(subject)) {
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

    public final static String NAME = "Compare Subjects...";

    public String getName() {
        return NAME;
    }


    public String getDescription() {
        return "Compare all selected subjects in a new document";
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
