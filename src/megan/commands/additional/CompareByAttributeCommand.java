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

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.algorithms.ComputeCoreBiome;
import megan.core.*;
import megan.dialogs.compare.Comparer;
import megan.main.MeganProperties;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * * compare by command
 * * Daniel Huson, 9.2015
 */
public class CompareByAttributeCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "compareBy attribute=<name> [mode={relative|absolute}] [samples=<names>];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        final Document doc = ((Director) getDir()).getDocument();

        np.matchIgnoreCase("compareBy attribute=");
        String attribute = np.getWordRespectCase();
        final Comparer.COMPARISON_MODE mode;
        if (np.peekMatchIgnoreCase("mode")) {
            np.matchIgnoreCase("mode=");
            mode = Comparer.COMPARISON_MODE.valueOfIgnoreCase(np.getWordMatchesIgnoringCase("relative absolute"));
        } else
            mode = Comparer.COMPARISON_MODE.ABSOLUTE;

        final ArrayList<String> srcSamples = new ArrayList<>();
        if (np.peekMatchIgnoreCase("samples")) {
            srcSamples.addAll(np.getTokensRespectCase("samples=", ";"));
        } else {
            srcSamples.addAll(doc.getSampleNames());
            np.matchIgnoreCase(";");
        }

        final Map<String, List<String>> tarSample2SrcSamples = new HashMap<>();
        final List<String> tarSamples = new ArrayList<>();

        final Map<String, Object> tarSample2Value = new HashMap<>();

        for (String sample : srcSamples) {
            final Object obj = doc.getSampleAttributeTable().get(sample, attribute);
            if (obj != null) {
                final String value = obj.toString().trim();
                if (value.length() > 0) {
                    final String tarSample = (attribute.equals(SampleAttributeTable.SAMPLE_ID) ? value : attribute + ":" + value);
                    if (tarSample2SrcSamples.get(tarSample) == null) {
                        tarSamples.add(tarSample);
                        tarSample2SrcSamples.put(tarSample, new ArrayList<>());
                    }
                    tarSample2SrcSamples.get(tarSample).add(sample);
                    tarSample2Value.put(tarSample, value);
                }
            }
        }

        if (tarSample2SrcSamples.size() > 0) {

            final String fileName = Basic.replaceFileSuffix(doc.getMeganFile().getFileName(), "-" + attribute + ".megan");

            final Director newDir = Director.newProject(false);
            final Document newDocument = newDir.getDocument();
            newDocument.getMeganFile().setFile(fileName, MeganFile.Type.MEGAN_SUMMARY_FILE);

            doc.getProgressListener().setMaximum(srcSamples.size());
            doc.getProgressListener().setProgress(0);

            for (String tarSample : tarSamples) {
                doc.getProgressListener().setTasks("Comparing samples", tarSample);

                final List<String> samples = tarSample2SrcSamples.get(tarSample);
                Map<String, Map<Integer, float[]>> classification2class2counts = new HashMap<>();

                final int sampleSize = ComputeCoreBiome.apply(doc, samples, false, 0, 0, classification2class2counts, doc.getProgressListener());

                if (classification2class2counts.size() > 0) {
                    newDocument.addSample(tarSample, sampleSize, 0, doc.getBlastMode(), classification2class2counts);
                }
                doc.getProgressListener().incrementProgress();
            }

            // normalize:
            if (mode == Comparer.COMPARISON_MODE.RELATIVE) {
                float newSize = Float.MAX_VALUE;
                float maxSize = 0;
                for (String tarSample : tarSamples) {
                    newSize = Math.min(newSize, newDocument.getNumberOfReads(tarSample));
                    maxSize = Math.max(maxSize, newDocument.getNumberOfReads(tarSample));
                }
                if (newSize < maxSize) {
                    double[] factor = new double[tarSamples.size()];
                    for (int i = 0; i < tarSamples.size(); i++) {
                        String tarSample = tarSamples.get(i);
                        factor[i] = (newSize > 0 ? (double) newSize / (double) newDocument.getNumberOfReads(tarSample) : 0);
                    }
                    final DataTable dataTable = newDocument.getDataTable();
                    for (String classificationName : dataTable.getClassification2Class2Counts().keySet()) {
                        Map<Integer, float[]> class2counts = dataTable.getClass2Counts(classificationName);
                        for (Integer classId : class2counts.keySet()) {
                            float[] counts = class2counts.get(classId);
                            for (int i = 0; i < counts.length; i++) {
                                counts[i] = (int) Math.round(factor[i] * counts[i]);
                            }
                        }
                    }
                }
                newDocument.getDataTable().setParameters("mode=" + Comparer.COMPARISON_MODE.RELATIVE.toString() + " normalizedTo=" + newSize);
            } else
                newDocument.getDataTable().setParameters("mode=" + Comparer.COMPARISON_MODE.ABSOLUTE.toString());

            newDocument.getSampleAttributeTable().addAttribute(attribute, tarSample2Value, true);

            newDocument.setNumberReads(newDocument.getDataTable().getTotalReads());
            newDocument.setDirty(true);

            if (newDocument.getNumberOfSamples() > 1) {
                newDir.getMainViewer().getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.PieChart);
            }
            NotificationsInSwing.showInformation(String.format("Wrote %,d reads to file '%s'", newDocument.getNumberOfReads(), fileName));

            newDir.getMainViewer().getFrame().setVisible(true);
            newDir.getMainViewer().setDoReInduce(true);
            newDir.getMainViewer().setDoReset(true);
            newDir.execute("update reprocess=true reinduce=true;", newDir.getMainViewer().getCommandManager());
        }
    }

    public void actionPerformed(ActionEvent event) {
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return null;
    }

    public String getDescription() {
        return "Aggregate samples by this attribute and show comparison in new document";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Compare16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}
