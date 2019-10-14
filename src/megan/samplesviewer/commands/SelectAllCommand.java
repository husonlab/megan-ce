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
import jloda.swing.director.ProjectManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * * selection command
 * * Daniel Huson, 11.2010
 */
public class SelectAllCommand extends CommandBase implements ICommand {
    private static final String[] legalOptions = {"all", "none", "similar", "commentLike", "numerical", "uninformative", "romPrevious", "samples"};
    public String getSyntax() {
        return "select {" + Basic.toString(legalOptions, "|") + "} [name=<string>] [value=<string>];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(final NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("select");
        final String what = np.getWordMatchesIgnoringCase(legalOptions);
        final String name;
        final String value;
        final java.util.List<String> samples;
        if (what.equalsIgnoreCase("similar")) {
            np.matchIgnoreCase("name=");
            name = np.getWordRespectCase();
            if (np.peekMatchIgnoreCase("value=")) {
                np.matchIgnoreCase("value=");
                value = np.getWordRespectCase();
            } else
                value = null;
            samples = null;
            np.matchIgnoreCase(";");
        } else if (what.equalsIgnoreCase("samples")) {
            name = null;
            value = null;
            samples = np.getTokensRespectCase("name=", ";");
        } else {
            name = null;
            value = null;
            samples = null;
            np.matchIgnoreCase(";");
        }

        final SamplesViewer viewer = (SamplesViewer) getViewer();

        switch (what) {
            case "samples": {
                if (samples != null) {
                    viewer.getSamplesTableView().selectSamples(samples, true);
                    System.err.println("Selected " + samples.size() + " rows");
                }
                break;
            }
            case "all":
                viewer.getSamplesTableView().selectAll(true);
                break;
            case "none":
                viewer.getSamplesTableView().selectAll(false);
                break;
            case "commentLike": {
                int count = 0;
                for (String attribute : viewer.getSamplesTableView().getAttributes()) {
                    int min = Integer.MAX_VALUE;
                    int max = 0;
                    for (String sample : viewer.getSamplesTableView().getSamples()) {
                        final Object value1 = viewer.getSampleAttributeTable().get(sample, attribute);
                        if (value1 != null) {
                            String string = value1.toString().trim();
                            if (string.length() > 0) {
                                min = Math.min(min, string.length());
                                max = Math.max(max, string.length());
                            }
                        }
                    }
                    if (max - min > 100) {
                        viewer.getSamplesTableView().selectAttribute(attribute, true);
                        count++;
                    }
                }
                if (count > 0)
                    System.err.println("Selected " + count + " columns");
                break;
            }
            case "numerical": {
                int count = 0;
                final Collection<String> numericalAttributes = viewer.getSampleAttributeTable().getNumericalAttributes();
                for (String attribute : viewer.getSamplesTableView().getAttributes()) {
                    if (numericalAttributes.contains(attribute)) {
                        viewer.getSamplesTableView().selectAttribute(attribute, true);
                        count++;
                    }

                }
                if (count > 0)
                    System.err.println("Selected " + count + " columns");
                break;
            }
            case "uninformative": {
                int count = 0;
                for (String attribute : viewer.getSamplesTableView().getAttributes()) {
                    final Set<String> values = new HashSet<>();
                    for (String sample : viewer.getSamplesTableView().getSamples()) {
                        Object value1 = viewer.getSampleAttributeTable().get(sample, attribute);
                        if (value1 != null) {
                            String string = value1.toString().trim();
                            if (string.length() > 0) {
                                values.add(string);
                            }
                        }
                    }
                    if (values.size() <= 1 || values.size() == viewer.getSamplesTableView().getSampleCount()) {
                        viewer.getSamplesTableView().selectAttribute(attribute, true);
                        count++;
                    }
                }
                if (count > 0)
                    System.err.println("Selected " + count + " columns");
                break;
            }
            case "similar":
                viewer.getSamplesTableView().selectByValue(name, value);
                break;
            case "fromPrevious":
                String row1 = null;
                for (String sample : viewer.getSamplesTableView().getSamples()) {
                    if (ProjectManager.getPreviouslySelectedNodeLabels().contains(sample)) {
                        viewer.getSamplesTableView().selectSample(sample, true);
                        row1 = sample;
                    }
                }
                if (row1 != null) {
                    viewer.getSamplesTableView().scrollToSample(row1);
                }

                String col1 = null;
                for (String attribute : viewer.getSamplesTableView().getAttributes()) {
                    if (ProjectManager.getPreviouslySelectedNodeLabels().contains(attribute)) {
                        viewer.getSamplesTableView().selectAttribute(attribute, true);
                        col1 = attribute;
                    }
                }
                if (row1 == null && col1 != null) {
                    viewer.getSamplesTableView().scrollToSample(null);
                }
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("select all;");
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer;
    }

    public String getName() {
        return "Select All";
    }

    public String getDescription() {
        return "Selection";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
