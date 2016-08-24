/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.*;
import megan.dialogs.compare.Comparer;
import megan.fx.NotificationsInSwing;
import megan.main.MeganProperties;
import megan.samplesviewer.ComputeCoreBiome;
import megan.samplesviewer.SamplesSpreadSheet;
import megan.samplesviewer.SamplesViewer;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * * compare by command
 * * Daniel Huson, 9.2015
 */
public class CompareByAttributeRelativeCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "compareBy attribute=<name> [mode={relative|absolute}];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("compareBy attribute=");
        String attribute = np.getWordRespectCase();
        Comparer.COMPARISON_MODE mode = Comparer.COMPARISON_MODE.ABSOLUTE;
        if (!np.peekMatchIgnoreCase(";")) {
            np.matchIgnoreCase("mode=");
            mode = Comparer.COMPARISON_MODE.valueOfIgnoreCase(np.getWordMatchesIgnoringCase("relative absolute"));
        }
        np.matchIgnoreCase(";");

        final Document doc = ((Director) getDir()).getDocument();

        final SamplesSpreadSheet samplesTable = ((SamplesViewer) getViewer()).getSamplesTable();

        final BitSet samples = samplesTable.getSelectedSampleIndices();

        final List<String> tarSamplesOrder = new ArrayList<>();
        final Map<String, List<String>> tarSample2SrcSamples = new HashMap<>();

        final Map<String, Object> tarSample2Value = new HashMap<>();

        for (int row = samples.nextSetBit(1); row != -1; row = samples.nextSetBit(row + 1)) {
            final String sample = samplesTable.getDataGrid().getRowName(row);
            final Object obj = doc.getSampleAttributeTable().get(sample, attribute);
            if (obj != null) {
                final String value = obj.toString().trim();
                if (value.length() > 0) {
                    final String tarSample = (attribute.equals(SampleAttributeTable.SAMPLE_ID) ? value : attribute + ":" + value);
                    if (!tarSamplesOrder.contains(tarSample)) {
                        tarSamplesOrder.add(tarSample);
                        tarSample2SrcSamples.put(tarSample, new ArrayList<String>());
                        tarSample2Value.put(tarSample, value);
                    }
                    tarSample2SrcSamples.get(tarSample).add(sample);
                }
            }
        }

        if (tarSample2SrcSamples.size() > 0) {

            final String fileName = Basic.replaceFileSuffix(doc.getMeganFile().getFileName(), "-" + attribute + ".megan");

            final Director newDir = Director.newProject(false);
            final Document newDocument = newDir.getDocument();
            newDocument.getMeganFile().setFile(fileName, MeganFile.Type.MEGAN_SUMMARY_FILE);

            doc.getProgressListener().setMaximum(tarSamplesOrder.size());
            doc.getProgressListener().setProgress(0);

            for (String tarSample : tarSamplesOrder) {
                doc.getProgressListener().setTasks("Comparing samples", tarSample);

                List<String> srcSamples = tarSample2SrcSamples.get(tarSample);
                Map<String, Map<Integer, Integer[]>> classification2class2counts = new HashMap<>();

                int sampleSize = ComputeCoreBiome.apply(doc, srcSamples, false, 0, 0, classification2class2counts, doc.getProgressListener());

                if (classification2class2counts.size() > 0) {
                    newDocument.addSample(tarSample, sampleSize, 0, doc.getBlastMode(), classification2class2counts);
                }
                doc.getProgressListener().incrementProgress();
            }

            // normalize:
            if (mode == Comparer.COMPARISON_MODE.RELATIVE) {
                int newSize = Integer.MAX_VALUE;
                int maxSize = 0;
                for (String tarSample : tarSamplesOrder) {
                    newSize = Math.min(newSize, newDocument.getNumberOfReads(tarSample));
                    maxSize = Math.max(maxSize, newDocument.getNumberOfReads(tarSample));
                }
                if (newSize < maxSize) {
                    double[] factor = new double[tarSamplesOrder.size()];
                    for (int i = 0; i < tarSamplesOrder.size(); i++) {
                        String tarSample = tarSamplesOrder.get(i);
                        factor[i] = (newSize > 0 ? (double) newSize / (double) newDocument.getNumberOfReads(tarSample) : 0);
                    }
                    final DataTable dataTable = newDocument.getDataTable();
                    for (String classificationName : dataTable.getClassification2Class2Counts().keySet()) {
                        Map<Integer, Integer[]> class2counts = dataTable.getClass2Counts(classificationName);
                        for (Integer classId : class2counts.keySet()) {
                            Integer[] counts = class2counts.get(classId);
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
        final SamplesViewer viewer = ((SamplesViewer) getViewer());
        final String attribute = viewer.getSamplesTable().getASelectedColumn();
        if (attribute != null)
            execute("compareBy attribute='" + attribute + "' mode=" + Comparer.COMPARISON_MODE.RELATIVE.toString().toLowerCase() + ";");
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTable().getNumberOfSelectedColsIncludingSamplesCol() == 1;
    }

    public String getName() {
        return "Compare Relative...";
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
