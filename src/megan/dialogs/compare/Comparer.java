/*
 * Comparer.java Copyright (C) 2022 Daniel H. Huson
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
package megan.dialogs.compare;

import jloda.fx.util.ProgramExecutorService;
import jloda.seq.BlastMode;
import jloda.util.CollectionUtils;
import jloda.util.FileUtils;
import jloda.util.ProgramProperties;
import jloda.util.Single;
import jloda.util.parse.NexusStreamParser;
import jloda.util.progress.ProgressListener;
import megan.classification.Classification;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Director;
import megan.core.SampleAttributeTable;
import megan.viewer.gui.NodeDrawer;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * comparison of multiple datasets
 * Daniel Huson, 3.2007
 */
public class Comparer {
    public enum COMPARISON_MODE {
        ABSOLUTE, RELATIVE;

        public static COMPARISON_MODE valueOfIgnoreCase(String name) {
            for (COMPARISON_MODE mode : values()) {
                if (mode.toString().equalsIgnoreCase(name))
                    return mode;
            }
            return RELATIVE; // is probably SUBSAMPLE, which is no longer supported
        }
    }

    private final List<Director> dirs;
    private int[] pid2pos;

    private COMPARISON_MODE mode = COMPARISON_MODE.ABSOLUTE;
    private boolean ignoreUnassigned = false;
    private boolean keep1 = false;

    /**
     * constructor
     */
    public Comparer() {
        dirs = new LinkedList<>();
    }

    /**
     * add a project to be compared
     */
    public void addDirector(Director dir) {
        dirs.add(dir);
    }

    /**
     * compute a comparison
     */
    public void computeComparison(SampleAttributeTable sampleAttributeTable, final DataTable result, final ProgressListener progressListener) throws IOException {
        progressListener.setTasks("Comparison", "Initialization");
        progressListener.setMaximum(-1);

        System.err.println("Computing comparison: ");
        pid2pos = setupPid2Pos();

        result.setCreator(ProgramProperties.getProgramName());
        result.setCreationDate((new Date()).toString());

        final var names = new String[dirs.size()];
        final var uids = new Long[dirs.size()];
        final var originalNumberOfReads = new float[dirs.size()];
        final var blastModes = new BlastMode[dirs.size()];

        // lock all unlocked projects involved in the comparison
        final var myLocked = new LinkedList<Director>();

        try {
            final var sample2source = new HashMap<String, Object>();

            for (final var dir : dirs) {
                if (!dir.isLocked()) {
                    dir.notifyLockInput();
                    myLocked.add(dir);
                }
                final var pos = pid2pos[dir.getID()];
				names[pos] = getUniqueName(names, pos, FileUtils.getFileBaseName(dir.getDocument().getTitle()));
				originalNumberOfReads[pos] = (int) dir.getDocument().getNumberOfReads();
                blastModes[pos] = dir.getDocument().getBlastMode();
                if (dir.getDocument().getSampleAttributeTable().getNumberOfSamples() == 1) {
                    var oSample = dir.getDocument().getSampleAttributeTable().getSampleSet().iterator().next();
                    var attributes2value = dir.getDocument().getSampleAttributeTable().getAttributesToValues(oSample);
                    sampleAttributeTable.addSample(names[pos], attributes2value, false, true);
                }
                try {
                    if (!dir.getDocument().getMeganFile().isMeganSummaryFile())
                        uids[pos] = dir.getDocument().getConnector().getUId();
                } catch (Exception e) {
                    uids[pos] = 0L;
                }
                sample2source.put(names[pos], dir.getDocument().getMeganFile().getFileName());
            }

            sampleAttributeTable.addAttribute(SampleAttributeTable.HiddenAttribute.Source.toString(), sample2source, true, true);

            final var useRelative = (getMode() == COMPARISON_MODE.RELATIVE);

            final double newSampleSize;
            {
                var calculateNewSampleSize = 0.0;
                if (useRelative) {
                    for (var dir : dirs) {
                        final var mainViewer = dir.getMainViewer();
                        final double numberOfReads;

                        if (isIgnoreUnassigned())
                            numberOfReads = mainViewer.getTotalAssignedReads();
                        else {
                            numberOfReads = mainViewer.getNodeData(mainViewer.getTree().getRoot()).getCountSummarized();
                        }
                        if (calculateNewSampleSize == 0 || numberOfReads < calculateNewSampleSize)
                            calculateNewSampleSize = numberOfReads;
                    }
                    System.err.printf("Normalizing to: %,.0f reads per sample%n", calculateNewSampleSize);
                }
                newSampleSize = calculateNewSampleSize;
            }

            var parameters = "mode=" + getMode();
            if (useRelative)
                parameters += " normalizedTo=" + newSampleSize;
            if (isIgnoreUnassigned())
                parameters += " ignoreUnassigned=true";
            result.setParameters(parameters);

            final var sizes = new float[dirs.size()];

            progressListener.setMaximum(dirs.size());
            progressListener.setProgress(0);

            final var numberOfThreads = Math.min(ProgramExecutorService.getNumberOfCoresToUse(), dirs.size());
            final var service = Executors.newFixedThreadPool(numberOfThreads);


            final var exception = new Single<Exception>();

            progressListener.setTasks("Computing comparison", "Using " + mode.toString().toLowerCase() + " mode");
            progressListener.setProgress(0);
            progressListener.setMaximum(dirs.size());

            final var totalAssigned=new DoubleAdder();

            try {
                for (var dir : dirs) {
                    service.execute(() -> {
                        if (exception.isNull()) {
                            try {
                                final var pos = pid2pos[dir.getID()];
                                final DataTable table = dir.getDocument().getDataTable();

                                final double numberOfReads;
                                {
                                    if (isIgnoreUnassigned())
                                        numberOfReads = dir.getMainViewer().getTotalAssignedReads();
                                    else {
                                        numberOfReads = dir.getMainViewer().getNodeData(dir.getMainViewer().getTree().getRoot()).getCountSummarized();
                                    }
                                }

                                var readsWithTaxonomyAssignment=0f;

                                for (var classificationName : table.getClassification2Class2Counts().keySet()) {
                                    var isTaxonomy = classificationName.equals(ClassificationType.Taxonomy.toString());

                                    var class2countsSrc = table.getClass2Counts(classificationName);
                                    var class2countsTarget = result.getClass2Counts(classificationName);
                                    if (class2countsTarget == null) {
                                        synchronized (result) {
                                            class2countsTarget = result.getClass2Counts(classificationName);
                                            if (class2countsTarget == null) {
                                                class2countsTarget = new HashMap<>();
                                                result.getClassification2Class2Counts().put(classificationName, class2countsTarget);
                                            }
                                        }
                                    }

                                    final var factor = numberOfReads > 0 ? newSampleSize / numberOfReads : 1.0;

                                    for (var classId : class2countsSrc.keySet()) {
                                        // todo: here we assume that the nohits id is the same for all classifications...
                                        if (!isIgnoreUnassigned() || classId > 0) {
                                            var countsTarget = class2countsTarget.get(classId);
                                            if (countsTarget == null) {
                                                synchronized (result) {
                                                    countsTarget = class2countsTarget.get(classId);
                                                    if (countsTarget == null) {
                                                        countsTarget = new float[dirs.size()];
                                                        Arrays.fill(countsTarget, 0);
                                                        class2countsTarget.put(classId, countsTarget);
                                                    }
                                                }
                                            }
                                            final var count = CollectionUtils.getSum(class2countsSrc.get(classId));
                                            if (count == 0)
                                                countsTarget[pos] = 0;
                                            else if (useRelative) {
                                                countsTarget[pos] = (float) (count * factor);
                                                if (countsTarget[pos] == 0 && isKeep1())
                                                    countsTarget[pos] = 1;
                                            } else
                                                countsTarget[pos] = count;
                                            if (isTaxonomy) {
                                                readsWithTaxonomyAssignment += countsTarget[pos];
                                            }
                                        }
                                    }
                                    if(isTaxonomy) {
                                        sizes[pos]=readsWithTaxonomyAssignment;
                                        totalAssigned.add(readsWithTaxonomyAssignment);
                                    }
                                }
                                synchronized (progressListener) {
                                    progressListener.incrementProgress();
                                }
                            } catch (Exception ex) {
                                exception.setIfCurrentValueIsNull(ex);
                            }
                        }
                    });
                }
            }
            finally {
                service.shutdown();
            }

            try {
                if(!service.awaitTermination(1000, TimeUnit.DAYS))
                    exception.setIfCurrentValueIsNull(new IOException("timed out"));
            } catch (InterruptedException ex) {
                exception.setIfCurrentValueIsNull(ex);
            }
            finally {
                service.shutdownNow();
            }
            if(exception.isNotNull())
                throw new IOException("Comparison computation failed: " + exception.get().getMessage(), exception.get());

            // if we have a taxonomy classification, then use it to get exact values:
            if (result.getClassification2Class2Counts().containsKey(Classification.Taxonomy)) {
                var class2counts = result.getClass2Counts(Classification.Taxonomy);
                Arrays.fill(sizes, 0);
                for (var counts : class2counts.values()) {
                    for (var i = 0; i < counts.length; i++)
                        sizes[i] += counts[i];
                }
            }

            result.setSamples(names, uids, sizes, blastModes);
            sampleAttributeTable.removeAttribute(SampleAttributeTable.HiddenAttribute.Label.toString());

            for (var classificationName : result.getClassification2Class2Counts().keySet()) {
                result.setNodeStyle(classificationName, NodeDrawer.Style.PieChart.toString());
            }

            if (useRelative) {
                System.err.printf("Total assigned: %,12d normalized%n", totalAssigned.longValue());
            } else {
                System.err.printf("Total assigned: %,12d%n", totalAssigned.longValue());
            }

            result.setTotalReads((int) CollectionUtils.getSum(originalNumberOfReads));
        } finally {
            // unlock all projects involved in the comparison
            for (var dir : myLocked) {
                dir.notifyUnlockInput();
            }
        }
    }

    /**
     * modifies given name so that it does not match any of names[0],..,names[pos-1]
     *
     * @return name or new name
     */
    private String getUniqueName(String[] names, int pos, String name) {
        var ok = false;
        var count = 0;
        var newName = name;
        while (!ok && count < 1000) {
            ok = true;
            for (var i = 0; i < pos; i++) {
                if (newName.equalsIgnoreCase(names[i])) {
                    ok = false;
                    break;
                }
            }
            if (!ok)
                newName = name + "." + (++count);
        }
        return newName;
    }

    /**
     * setup pid 2 position mapping
     */
    private int[] setupPid2Pos() {
        var maxId = 0;
        for (var dir : dirs) {
            var pid = dir.getID();
            if (pid > maxId)
                maxId = pid;
        }
        var pid2pos = new int[maxId + 1];
        var dirCount = 0;
        for (var dir : dirs) {
            int pid = dir.getID();
            pid2pos[pid] = dirCount++;
        }
        return pid2pos;
    }

    /**
     * gets the algorithm string
     *
     * @return algorithm string
     */
    public String getAlgorithm() {
        return "compare";
    }

    /**
     * Convenience method: gets the mode encoded in the parameter string
     *
     * @return mode
     */
    static public COMPARISON_MODE parseMode(String parameterString) {
        if (parameterString != null) {
            try (var np = new NexusStreamParser(new StringReader(parameterString))) {
                while (np.peekNextToken() != NexusStreamParser.TT_EOF) {
                    if (np.peekMatchIgnoreCase("mode=")) {
                        np.matchIgnoreCase("mode=");
                        return COMPARISON_MODE.valueOfIgnoreCase(np.getWordRespectCase());
                    } else np.getWordRespectCase(); // skip
                }
            } catch (Exception ignored) {
            }
        }
        return COMPARISON_MODE.ABSOLUTE;
    }

    /**
     * Convenience method: gets the normalization number encoded in the parameter string
     *
     * @return number of reads normalized by
     */
    public static int parseNormalizedTo(String parameterString) {
            if (parameterString != null) {
                try (var np = new NexusStreamParser(new StringReader(parameterString))) {
                    while (np.peekNextToken() != NexusStreamParser.TT_EOF) {
                        if (np.peekMatchIgnoreCase("normalizedTo=")) {
                            np.matchIgnoreCase("normalizedTo=");
                            return np.getInt();
                        }
                        // for backward compatibility:
                        if (np.peekMatchIgnoreCase("normalized_to=")) {
                            np.matchIgnoreCase("normalized_to=");
                            return np.getInt();
                        }
                        np.getWordRespectCase();
                    }
                } catch (Exception ignored) {
                }
            }
        return 0;
    }

    /**
     * set the comparison mode
     */
    private void setMode(COMPARISON_MODE mode) {
        this.mode = mode;
    }

    public void setMode(String modeName) {
        setMode(COMPARISON_MODE.valueOfIgnoreCase(modeName));
    }

    /**
     * gets the comparison mode
     *
     * @return mode
     */
    private COMPARISON_MODE getMode() {
        return mode;
    }

    private boolean isIgnoreUnassigned() {
        return ignoreUnassigned;
    }

    public void setIgnoreUnassigned(boolean ignoreUnassigned) {
        this.ignoreUnassigned = ignoreUnassigned;
    }

    public List<Director> getDirs() {
        return dirs;
    }

    private boolean isKeep1() {
        return keep1;
    }

    public void setKeep1(boolean keep1) {
        this.keep1 = keep1;
    }
}
