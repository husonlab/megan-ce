/*
 * RemoveLowAbundancesCommand.java Copyright (C) 2022 Daniel H. Huson
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
package megan.commands.algorithms;

import jloda.graph.Node;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import jloda.util.progress.ProgressListener;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.main.MeganProperties;
import megan.samplesviewer.SamplesViewer;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomyData;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * compute robust biome
 * Daniel Huson, 22022
 */
public class RemoveLowAbundancesCommand extends CommandBase implements ICommand {
	public String getSyntax() {
		return "compute minAbundance=<threshold> samples=<name name...>;";
	}

	public void apply(NexusStreamParser np) throws Exception {
		final var dir = getDir();
		final var doc = dir.getDocument();

		np.matchIgnoreCase("compute minAbundance=");
		var threshold = (float) np.getDouble(0, 100);


		final var legalSampleNames = "'" + StringUtils.toString(doc.getSampleNames(), "' '") + "' ALL";
		final var selectedSamples = new ArrayList<String>();
		np.matchIgnoreCase("samples=");
		while (!np.peekMatchIgnoreCase(";")) {
			selectedSamples.add(np.getWordMatchesRespectingCase(legalSampleNames));
		}
		np.matchIgnoreCase(";");

		if (selectedSamples.contains("ALL")) {
			selectedSamples.addAll(doc.getSampleNames());
		}

		System.err.println("Number of samples: " + selectedSamples.size());

		var newDir = Director.newProject(false);
		if (dir.getMainViewer() != null) {
			newDir.getMainViewer().getFrame().setVisible(true);
			newDir.getMainViewer().setDoReInduce(true);
			newDir.getMainViewer().setDoReset(true);
		}
		final var newDocument = newDir.getDocument();

		final var tarClassification2class2counts = new HashMap<String, Map<Integer, float[]>>();
		var sampleSizes = removeLowAbundance(doc, selectedSamples, threshold, tarClassification2class2counts, doc.getProgressListener());

		if (tarClassification2class2counts.size() > 0) {
			for (var s = 0; s < selectedSamples.size(); s++) {
				newDocument.getDataTable().addSample(selectedSamples.get(s), sampleSizes[s], doc.getDataTable().getBlastMode(), s, tarClassification2class2counts);
			}
			newDocument.getSampleAttributeTable().addTable(doc.getSampleAttributeTable().extractTable(selectedSamples), false, true);

			newDocument.setNumberReads(newDocument.getDataTable().getTotalReads());
			var fileName = FileUtils.replaceFileSuffix(doc.getMeganFile().getFileName(), "-min-" + threshold + ".megan");
			newDocument.getMeganFile().setFile(fileName, MeganFile.Type.MEGAN_SUMMARY_FILE);
			System.err.printf("Total number of reads: %,d%n", newDocument.getNumberOfReads());
			newDocument.processReadHits();
			newDocument.setTopPercent(100);
			newDocument.setMinScore(0);
			newDocument.setMaxExpected(10000);
			newDocument.setMinSupport(1);
			newDocument.setMinSupportPercent(0);
			newDocument.setDirty(true);
			for (var classificationName : newDocument.getDataTable().getClassification2Class2Counts().keySet()) {
				newDocument.getActiveViewers().add(classificationName);
			}

			newDocument.getSampleAttributeTable().addTable(doc.getSampleAttributeTable().mergeSamples(selectedSamples, newDocument.getSampleNames().get(0)), false, true);

			if (newDocument.getNumberOfSamples() > 1) {
				newDir.getMainViewer().getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.PieChart);
			}
			NotificationsInSwing.showInformation("Filtered dataset has %,d reads".formatted(newDocument.getNumberOfReads()));

			newDir.execute("update reprocess=true reinduce=true;", newDir.getMainViewer().getCommandManager());

		}
	}

	public void actionPerformed(ActionEvent event) {
		final Collection<String> samples;
		if (getViewer() instanceof SamplesViewer)
			samples = ((SamplesViewer) getViewer()).getSamplesTableView().getSelectedSamples();
		else if (getViewer() instanceof ClassificationViewer)
			samples = ((ClassificationViewer) getViewer()).getDocument().getSampleNames();
		else
			return;

		if (samples.size() > 1) {
			float classThresholdPercent = (float) ProgramProperties.get("LowAbundanceThreshold", 1.0);

			var result = JOptionPane.showInputDialog(getViewer().getFrame(), "Minimum abundance threshold (%):", "MEGAN - Remove low abundance nodes", JOptionPane.QUESTION_MESSAGE, null, null, classThresholdPercent);
			if (result instanceof String string && NumberUtils.isFloat(string)) {
				classThresholdPercent = NumberUtils.parseFloat(string);
				ProgramProperties.put("LowAbundanceThreshold", classThresholdPercent);

				execute("compute minAbundance=" + result + " samples='" + StringUtils.toString(samples, "' '") + "';");
			} else
				NotificationsInSwing.showError(getViewer().getFrame(), "Failed to parse value: " + result);
		}
	}


	public boolean isApplicable() {
		return getViewer() instanceof ClassificationViewer || getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTableView().getCountSelectedSamples() > 1;
	}

	public boolean isCritical() {
		return true;
	}

	public String getName() {
		return "Remove Low Abundances...";
	}

	public ImageIcon getIcon() {
		return null;
	}

	public String getDescription() {
		return "Remove taxa and functions whose abundance lie below a given percentage of assigned reads for all samples";
	}

	/**
	 * compute a new comparison in which low abundance items have been removed
	 *
	 * @param srcDoc
	 * @param samplesToUse
	 * @param threshold
	 * @param tarClassification2class2counts
	 * @param progress
	 * @return low abundance version
	 * @throws CanceledException
	 */
	private int[] removeLowAbundance(Document srcDoc, ArrayList<String> samplesToUse, float threshold, HashMap<String, Map<Integer, float[]>> tarClassification2class2counts, ProgressListener progress) throws CanceledException {
		var numberInputSamples = srcDoc.getNumberOfSamples();
		var numberOutputSamples = samplesToUse.size();

		final var sampleIds = srcDoc.getDataTable().getSampleIds(samplesToUse);

		var input2outputId = new int[sampleIds.cardinality()];
		var outId = 0;
		for (var inId : BitSetUtils.members(sampleIds)) {
			input2outputId[inId] = outId++;
		}

		final var sizes = new int[numberOutputSamples];

		if (numberOutputSamples > 0) {
			var dataTable = srcDoc.getDataTable();
			for (var classificationName : dataTable.getClassification2Class2Counts().keySet()) {
				final var srcClass2counts = srcDoc.getDataTable().getClass2Counts(classificationName);
				final Node root;

				if (classificationName.equals(Classification.Taxonomy))
					root = TaxonomyData.getTree().getRoot();
				else {
					root = ClassificationManager.get(classificationName, true).getFullTree().getRoot();
				}

				final int[] detectionThresholds = ComputeCoreOrRareBiome.computeDetectionThreshold(classificationName, numberInputSamples, srcClass2counts, threshold);

				var tarClass2counts = new HashMap<Integer, float[]>();

				if (true) {
					var coreClass2counts = new HashMap<Integer, float[]>();
					ComputeCoreOrRareBiome.computeCoreBiomeRec(sampleIds, false, srcDoc.getNumberOfSamples(), 0, detectionThresholds, root, srcClass2counts, coreClass2counts, progress);

					for (var classId : coreClass2counts.keySet()) {
						var value = coreClass2counts.get(classId)[0];
						if (value > 0) {
							var srcCounts = srcClass2counts.get(classId);
							var tarCounts = new float[numberOutputSamples];
							var outS = 0;
							for (var inS : BitSetUtils.members(sampleIds)) {
								tarCounts[outS++] = srcCounts[inS];
							}
							tarClass2counts.put(classId, tarCounts);
						}
					}
				} else {
					for (var s : BitSetUtils.members(sampleIds)) {
						var tmpClass2counts = new HashMap<Integer, float[]>();
						ComputeCoreOrRareBiome.computeCoreBiomeRec(BitSetUtils.asBitSet(s), false, srcDoc.getNumberOfSamples(), 0, detectionThresholds, root, srcClass2counts, tmpClass2counts, progress);
						for (var classId : tmpClass2counts.keySet()) {
							var value = tmpClass2counts.get(classId)[0];
							var array = tarClass2counts.computeIfAbsent(classId, a -> new float[numberOutputSamples]);
							array[input2outputId[s]] = value;
						}
					}
				}
				tarClassification2class2counts.put(classificationName, tarClass2counts);
				if (classificationName.equals(Classification.Taxonomy)) {
					for (var counts : tarClass2counts.values()) {
						for (var i = 0; i < counts.length; i++) {
							sizes[i] += counts[i];
						}
					}
				}
			}
		}
		return sizes;
	}
}

