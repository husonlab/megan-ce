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

package megan.commands.format;

import javafx.application.Platform;
import javafx.scene.control.ChoiceDialog;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.NodeShape;
import jloda.swing.util.ResourceManager;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * * shape by command
 * * Daniel Huson, 9.2105
 */
public class ShapeSamplesByCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "shapeBy attribute=<name>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("shapeBy attribute=");
        String attribute = np.getWordRespectCase();
        Document doc = ((Director) getDir()).getDocument();

        java.util.Collection<String> samples;
        if (getViewer() instanceof SamplesViewer) {
            samples = ((SamplesViewer) getViewer()).getSamplesTableView().getSelectedSamples();
        } else
            samples = doc.getSampleAttributeTable().getSampleSet();

        ProgramProperties.put("SetByAttribute", attribute);

        Map<String, Integer> value2Count = new HashMap<>();
        for (String sample : samples) {
            Object value = doc.getSampleAttributeTable().get(sample, attribute);
            if (value != null) {
                value2Count.merge(value.toString(), 1, Integer::sum);
            }
        }

        Pair<Integer, String>[] pairs = new Pair[value2Count.size()];
        int i = 0;
        for (String value : value2Count.keySet()) {
            pairs[i++] = new Pair<>(value2Count.get(value), value);
        }
        Arrays.sort(pairs, (p1, p2) -> -p1.compareTo(p2));

        Map<String, NodeShape> value2shape = new HashMap<>();
        int count = 0;
        for (Pair<Integer, String> pair : pairs) {
            value2shape.put(pair.get2(), NodeShape.values()[Math.min(++count, NodeShape.values().length - 1)]);
        }

        StringBuilder buf = new StringBuilder();
        for (String value : value2shape.keySet()) {
            boolean first = true;
            for (String sample : samples) {
                Object aValue = doc.getSampleAttributeTable().get(sample, attribute);
                if (aValue != null && aValue.toString().equals(value)) {
                    if (first) {
                        buf.append("set nodeShape=").append(value2shape.get(value)).append(" sample=");
                        first = false;
                    }
                    buf.append(" '").append(sample).append("'");
                }
            }
            if (!first)
                buf.append(";");
        }
        executeImmediately(buf.toString());
    }

    public void actionPerformed(ActionEvent event) {
        final Document doc = ((Director) getDir()).getDocument();
        final java.util.List<String> attributes = doc.getSampleAttributeTable().getUnhiddenAttributes();

        if (attributes.size() > 0) {
            final JFrame frame = getViewer().getFrame();
            Platform.runLater(() -> {
                String defaultChoice = ProgramProperties.get("SetByAttribute", "");

                if (!attributes.contains(defaultChoice))
                    defaultChoice = attributes.get(0);

                ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, attributes);
                dialog.setTitle("MEGAN6 " + getViewer().getClassName() + " choice");
                dialog.setHeaderText("Select attribute to shape by");
                dialog.setContentText("Choose attribute:");

                if (frame != null) {
                    dialog.setX(frame.getX() + (frame.getWidth() - 200) / 2);
                    dialog.setY(frame.getY() + (frame.getHeight() - 200) / 2);
                }

                final Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    final String choice = result.get();
                    SwingUtilities.invokeLater(() -> execute("shapeBy attribute='" + choice + "';"));
                }
            });
        }
    }

    public boolean isApplicable() {
        Document doc = ((Director) getDir()).getDocument();
        return doc.getSampleAttributeTable().getNumberOfUnhiddenAttributes() > 0;
    }

    public String getName() {
        return "Shape Samples By Attribute";
    }

    public String getDescription() {
        return "Shape samples by selected attribute";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Circle16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}
