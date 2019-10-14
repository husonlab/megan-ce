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
package megan.commands.additional;

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.classification.data.Name2IdMap;
import megan.commands.CommandBase;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class CorrelateClassToAttributeCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "correlate class={name|number ...} classification={name} attribute={name} ;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("correlate class=");
        ArrayList<String> labels = new ArrayList<>();
        while (!np.peekMatchIgnoreCase("classification")) {
            labels.add(np.getWordRespectCase());
        }
        np.matchIgnoreCase("classification=");
        String classificationName = np.getWordRespectCase();
        np.matchIgnoreCase("attribute=");
        String attribute = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        final ClassificationViewer viewer = (ClassificationViewer) getDir().getViewerByClassName(classificationName);
        final Name2IdMap name2IdMap = ClassificationManager.get(classificationName, false).getName2IdMap();

        final String[] sampleNames = getDoc().getSampleNamesAsArray();
        final int n = sampleNames.length;

        for (String label : labels) {
            final Integer id = (Basic.isInteger(label) ? Basic.parseInt(label) : name2IdMap.get(label));
            final String name = (Basic.isInteger(label) ? name2IdMap.get(id) : label);
            if (id != 0) {
                final Node v = viewer.getANode(id);
                final NodeData nodeData = (NodeData) v.getData();
                final float[] x = (v.getOutDegree() == 0 ? nodeData.getSummarized() : nodeData.getAssigned());
                final double[] y = new double[n];
                for (int i = 0; i < n; i++) {
                    Object obj = getDoc().getSampleAttributeTable().get(sampleNames[i], attribute);
                    if (obj instanceof Number)
                        y[i] = ((Number) obj).doubleValue();
                    else
                        throw new IOException("Attribute '" + attribute + "': has non-numerical value: " + obj);
                }
                System.out.println("Sample\t'" + name + "'\t'" + attribute + "':");
                for (int i = 0; i < n; i++) {
                    System.err.println(String.format("%s\t%f\t%f", sampleNames[i], x[i], y[i]));
                }
                System.err.println("Correlation coefficient: " + computeCorrelationCoefficient(x, y, n));
            }
        }
    }

    /**
     * computes the correlation coefficient
     *
     * @param x
     * @param y
     * @param n
     * @return
     */
    private double computeCorrelationCoefficient(float[] x, double[] y, int n) {
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        double sumY2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
        }
        final double bottom = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        if (bottom == 0)
            return 0;
        final double top = n * sumXY - sumX * sumY;
        return top / bottom;
    }

    public void actionPerformed(ActionEvent event) {
        final Collection<String> list = getDoc().getSampleAttributeTable().getNumericalAttributes();
        final ClassificationViewer viewer = (ClassificationViewer) getViewer();
        final Collection<Integer> ids = viewer.getSelectedIds();
        if (ids.size() > 0 && list.size() > 0) {
            final String[] choices = list.toArray(new String[0]);
            String choice = ProgramProperties.get("CorrelateToAttribute", choices[0]);
            if (!list.contains(choice))
                choice = choices[0];

            choice = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Choose attribute to correlate to:",
                    "Compute Correlation Coefficient", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), choices, choice);

            if (choice != null) {
                ProgramProperties.put("CorrelateToAttribute", choice);

                StringBuilder buf = new StringBuilder();

                buf.append("correlate class=");
                for (Integer id : ids) {
                    buf.append(" ").append(id);
                }
                buf.append(" classification='").append(viewer.getClassName()).append("' attribute='").append(choice).append("';");
                executeImmediately("show window=message;");
                execute(buf.toString());
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof ClassificationViewer && ((ClassificationViewer) getViewer()).getNumberSelectedNodes() > 0 && getDoc().getNumberOfSamples() > 0 && getDoc().getSampleAttributeTable().getNumberOfAttributes() > 0;
    }

    public String getName() {
        return "Correlate To Attributes...";
    }

    public String getDescription() {
        return "Correlate assigned (or summarized) counts for nodes with a selected attribute";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}
