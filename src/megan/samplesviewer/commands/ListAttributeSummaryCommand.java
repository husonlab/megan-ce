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
package megan.samplesviewer.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Pair;
import jloda.util.Statistics;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * list a report over all attributes
 *
 * @author Daniel Huson, 3.2017
 */
public class ListAttributeSummaryCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("list attributes=");
        final String what = np.getWordMatchesIgnoringCase(" all selected");
        np.matchIgnoreCase(";");

        final Collection<String> activeSamples;
        final Collection<String> activeAttributes;

        if (what.equalsIgnoreCase("selected")) {
            if (getViewer() instanceof SamplesViewer) {
                final SamplesViewer samplesViewer = (SamplesViewer) getViewer();
                activeSamples = samplesViewer.getSamplesTableView().getSelectedSamples();
                activeAttributes = samplesViewer.getSamplesTableView().getSelectedAttributes();
            } else
                return;
        } else {
            final Document doc = ((Director) getDir()).getDocument();
            activeSamples = doc.getSampleNames();
            activeAttributes = doc.getSampleAttributeTable().getUnhiddenAttributes();
        }


        final Document doc = ((Director) getDir()).getDocument();
        final SampleAttributeTable sampleAttributeTable = doc.getSampleAttributeTable();

        final Collection<String> numericalAttributes = sampleAttributeTable.getNumericalAttributes();


        System.err.println("Active samples:    " + activeSamples.size());
        System.err.println("Active attributes: " + activeAttributes.size());

        for (String attribute : activeAttributes) {
            if (numericalAttributes.contains(attribute)) {
                ArrayList<Number> numbers = new ArrayList<>(activeSamples.size());
                for (String sample : activeSamples) {
                    Object value = sampleAttributeTable.get(sample, attribute);
                    if (value instanceof Number)
                        numbers.add((Number) value);
                }
                final Statistics statistics = new Statistics(numbers);
                System.out.println(String.format("%s: m=%.1f sd=%.1f (%.1f - %.1f)", attribute, statistics.getMean(), statistics.getStdDev(), statistics.getMin(), statistics.getMax()));
            } else {
                final Map<String, Integer> value2count = new HashMap<>();
                for (String sample : activeSamples) {
                    Object value = sampleAttributeTable.get(sample, attribute);
                    if (value != null) {
                        String string = value.toString().trim();
                        if (string.length() > 0) {
                            value2count.merge(string, 1, Integer::sum);
                        }
                    }
                }
                ArrayList<Pair<Integer, String>> list = new ArrayList<>(value2count.size());
                for (Map.Entry<String, Integer> entry : value2count.entrySet()) {
                    list.add(new Pair<>(entry.getValue(), entry.getKey()));
                }
                if (list.size() > 0) {
                    list.sort((a, b) -> {
                        if (a.getFirst() > b.getFirst())
                            return -1;
                        else if (a.getFirst() < b.getFirst())
                            return 1;
                        else
                            return a.getSecond().compareTo(b.getSecond());
                    });
                    System.out.println(String.format("%s:", attribute));

                    Pair<Integer, String> first = list.get(0);
                    if (first.getFirst() == 1) {
                        System.out.println(String.format("\t(Singleton: %.1f %%)", 100 * ((double) list.size() / (double) activeSamples.size())));
                        if (list.size() < activeSamples.size())
                            System.out.println(String.format("\t(Rest: %.1f %%)", 100 * ((double) (activeSamples.size() - list.size()) / (double) activeSamples.size())));
                    } else {
                        int count = 0;
                        double sum = 0;
                        for (Pair<Integer, String> pair : list) {
                            double percent = Math.max(0, Math.min(100, 100 * ((double) pair.getFirst() / (double) activeSamples.size())));
                            System.out.println(String.format("\t%s: %.1f %%", pair.getSecond(), percent));
                            sum += percent;
                            if (++count == 20) {
                                if (count < list.size())
                                    System.out.println(String.format("\t(Rest: %d items, %.1f %%)", (list.size() - count), Math.max(0, 100 - sum)));
                                break;
                            }
                        }
                    }
                }
            }
        }

    }

    public String getSyntax() {
        return "list attributes={all|selected};";
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof SamplesViewer) {
            final SamplesViewer samplesViewer = (SamplesViewer) getViewer();
            if (samplesViewer.getSamplesTableView().getSelectedSamples().size() > 0 ||
                    samplesViewer.getSamplesTableView().getSelectedAttributes().size() > 0)
                executeImmediately("show window=message;list attributes=selected;");
            else
                executeImmediately("show window=message;list attributes=all;");
        }
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "List Summary...";
    }

    public String getAltName() {
        return "List Attribute Summary...";
    }


    public String getDescription() {
        return "List summary of attributes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/History16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
