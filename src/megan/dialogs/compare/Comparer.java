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
package megan.dialogs.compare;

import jloda.fx.util.ProgramExecutorService;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Director;
import megan.core.SampleAttributeTable;
import megan.viewer.MainViewer;
import megan.viewer.gui.NodeDrawer;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

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
     *
     * @param dir
     */
    public void addDirector(Director dir) {
        dirs.add(dir);
    }

    /**
     * compute a comparison
     *
     * @param result
     * @param progressListener
     * @throws CanceledException
     */
    public void computeComparison(SampleAttributeTable sampleAttributeTable, final DataTable result, final ProgressListener progressListener) throws IOException, CanceledException {
        progressListener.setTasks("Comparison", "Initialization");
        progressListener.setMaximum(-1);

        System.err.println("Computing comparison: ");
        pid2pos = setupPid2Pos();

        result.setCreator(ProgramProperties.getProgramName());
        result.setCreationDate((new Date()).toString());

        final String[] names = new String[dirs.size()];
        final Long[] uids = new Long[dirs.size()];
        final float[] originalNumberOfReads = new float[dirs.size()];
        final BlastMode[] blastModes = new BlastMode[dirs.size()];

        // lock all unlocked projects involved in the comparison
        final List<Director> myLocked = new LinkedList<>();

        try {
            final Map<String, Object> sample2source = new HashMap<>();

            for (final Director dir : dirs) {
                if (!dir.isLocked()) {
                    dir.notifyLockInput();
                    myLocked.add(dir);
                }
                final int pos = pid2pos[dir.getID()];
                names[pos] = getUniqueName(names, pos, Basic.getFileBaseName(dir.getDocument().getTitle()));
                originalNumberOfReads[pos] = (int) dir.getDocument().getNumberOfReads();
                blastModes[pos] = dir.getDocument().getBlastMode();
                if (dir.getDocument().getSampleAttributeTable().getNumberOfSamples() == 1) {
                    String oSample = dir.getDocument().getSampleAttributeTable().getSampleSet().iterator().next();
                    Map<String, Object> attributes2value = dir.getDocument().getSampleAttributeTable().getAttributesToValues(oSample);
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

            final boolean useRelative = (getMode() == COMPARISON_MODE.RELATIVE);

            final long newSampleSize;
            {
                long calculateNewSampleSize = 0;
                if (useRelative) {
                    for (Director dir : dirs) {
                        final MainViewer mainViewer = dir.getMainViewer();
                        final long numberOfReads;

                        if (isIgnoreUnassigned())
                            numberOfReads = mainViewer.getTotalAssignedReads();
                        else {
                            numberOfReads = Math.round(mainViewer.getNodeData(mainViewer.getTree().getRoot()).getCountSummarized());
                        }
                        if (calculateNewSampleSize == 0 || numberOfReads < calculateNewSampleSize)
                            calculateNewSampleSize = numberOfReads;
                    }
                    System.err.println("Normalizing to: " + calculateNewSampleSize + " reads per sample");
                }
                newSampleSize = calculateNewSampleSize;
            }

            String parameters = "mode=" + getMode();
            if (useRelative)
                parameters += " normalizedTo=" + newSampleSize;
            if (isIgnoreUnassigned())
                parameters += " ignoreUnassigned=true";
            result.setParameters(parameters);

            final float[] sizes = new float[dirs.size()];

            progressListener.setMaximum(dirs.size());
            progressListener.setProgress(0);

            final int numberOfThreads = Math.min(ProgramExecutorService.getNumberOfCoresToUse(), dirs.size());
            final ArrayBlockingQueue<Director> inputQueue = new ArrayBlockingQueue<>(dirs.size() + numberOfThreads);
            final ExecutorService service = ProgramExecutorService.createServiceForParallelAlgorithm(numberOfThreads);

            final long[] assignedCountPerThread = new long[numberOfThreads];

            final Single<Integer> progressListenerThread = new Single<>(-1); // make sure we are only moving progresslistener in one thread
            final ProgressSilent progressSilent = new ProgressSilent();

            final Single<Exception> exception = new Single<>();
            final Director sentinel = new Director(null);

            final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

            for (int i = 0; i < numberOfThreads; i++) {
                final int threadNumber = i;
                service.execute(() -> {
                    long readCount = 0;
                    try {
                        while (true) {
                            final Director dir = inputQueue.take();

                            if (dir == sentinel)
                                return;

                            final int pos = pid2pos[dir.getID()];
                            readCount = 0;

                            final DataTable table = dir.getDocument().getDataTable();

                            final long numberOfReads;

                            {
                                final MainViewer mainViewer = dir.getMainViewer();
                                if (isIgnoreUnassigned())
                                    numberOfReads = mainViewer.getTotalAssignedReads();
                                else {
                                    numberOfReads = Math.round(mainViewer.getNodeData(mainViewer.getTree().getRoot()).getCountSummarized());
                                }
                            }

                            ProgressListener progress;
                            synchronized (progressListenerThread) {
                                if (progressListenerThread.get() == -1) {
                                    progress = progressListener;
                                    progressListenerThread.set(threadNumber);
                                } else
                                    progress = progressSilent;
                            }

                            for (String classificationName : table.getClassification2Class2Counts().keySet()) {
                                boolean isTaxonomy = classificationName.equals(ClassificationType.Taxonomy.toString());

                                Map<Integer, float[]> class2countsSrc = table.getClass2Counts(classificationName);
                                Map<Integer, float[]> class2countsTarget = result.getClass2Counts(classificationName);
                                if (class2countsTarget == null) {
                                    synchronized (result) {
                                        class2countsTarget = result.getClass2Counts(classificationName);
                                        if (class2countsTarget == null) {
                                            class2countsTarget = new HashMap<>();
                                            result.getClassification2Class2Counts().put(classificationName, class2countsTarget);
                                        }
                                    }
                                }

                                final double factor = numberOfReads > 0 ? (double) newSampleSize / (double) numberOfReads : 1;

                                for (Integer classId : class2countsSrc.keySet()) {
                                    // todo: here we assume that the nohits id is the same for all classifications...
                                    if (!isIgnoreUnassigned() || classId > 0) {
                                        float[] countsTarget = class2countsTarget.get(classId);
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
                                        final float count = Basic.getSum(class2countsSrc.get(classId));
                                        if (count == 0)
                                            countsTarget[pos] = 0;
                                        else if (useRelative) {
                                            countsTarget[pos] = (int) Math.round(count * factor);
                                            if (countsTarget[pos] == 0 && isKeep1())
                                                countsTarget[pos] = 1;
                                        } else
                                            countsTarget[pos] = count;
                                        if (isTaxonomy)
                                            readCount += countsTarget[pos];
                                    }
                                }
                            }
                            sizes[pos] = (int) readCount;
                            progress.incrementProgress();
                        }
                    } catch (Exception ex) {
                        exception.set(ex);
                        while (countDownLatch.getCount() > 0)
                            countDownLatch.countDown();
                        service.shutdownNow();
                    } finally {
                        synchronized (progressListenerThread) {
                            if (progressListenerThread.get() == threadNumber)
                                progressListenerThread.set(-1);
                        }
                        assignedCountPerThread[threadNumber] += readCount;
                        countDownLatch.countDown();
                    }
                });
            }

            progressListener.setTasks("Computing comparison", "Using " + mode.toString().toLowerCase() + " mode");
            progressListener.setProgress(0);
            progressListener.setMaximum(dirs.size());

            try {
                for (Director dir : dirs) {
                    inputQueue.put(dir);
                }
                for (int i = 0; i < numberOfThreads; i++) {
                    inputQueue.put(sentinel);
                }
                // wait until all jobs are done
                countDownLatch.await();
            } catch (InterruptedException e) {
               Basic.caught(e);
                if (exception.get() == null)
                    exception.set(new IOException("Comparison computation failed: " + e.getMessage(), e));
            }

            if (exception.get() != null) {
                throw new IOException("Comparison computation failed: " + exception.get().getMessage(), exception.get());
            }
            service.shutdownNow();

            // if we have a taxonomy classification, then use it to get exact values:
            if (result.getClassification2Class2Counts().containsKey(Classification.Taxonomy)) {
                Map<Integer, float[]> class2counts = result.getClass2Counts(Classification.Taxonomy);
                Arrays.fill(sizes, 0);
                for (float[] counts : class2counts.values()) {
                    for (int i = 0; i < counts.length; i++)
                        sizes[i] += counts[i];
                }
            }

            result.setSamples(names, uids, sizes, blastModes);
            sampleAttributeTable.removeAttribute(SampleAttributeTable.HiddenAttribute.Label.toString());

            final long totalAssigned = Basic.getSum(assignedCountPerThread);

            for (String classificationName : result.getClassification2Class2Counts().keySet()) {
                result.setNodeStyle(classificationName, NodeDrawer.Style.PieChart.toString());
            }

            if (useRelative) {
                System.err.println(String.format("Total assigned: %,12d normalized", totalAssigned));
            } else {
                System.err.println(String.format("Total assigned: %,12d", totalAssigned));
            }

            result.setTotalReads((int) Basic.getSum(originalNumberOfReads));
        }
        finally{
            // unlock all projects involved in the comparison
            for (final Director dir : myLocked) {
                dir.notifyUnlockInput();
            }
        }
    }

    /**
     * modifies given name so that it does not match any of names[0],..,names[pos-1]
     *
     * @param names
     * @param pos
     * @param name
     * @return name or new name
     */
    private String getUniqueName(String[] names, int pos, String name) {
        boolean ok = false;
        int count = 0;
        String newName = name;
        while (!ok && count < 1000) {
            ok = true;
            for (int i = 0;i < pos; i++) {
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
        int maxId = 0;
        for (final Director dir : dirs) {
            int pid = dir.getID();
            if (pid > maxId)
                maxId = pid;
        }
        int[] pid2pos = new int[maxId + 1];
        int dirCount = 0;
        for (final Director dir : dirs) {
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
     * @param parameterString
     * @return mode
     */
    static public COMPARISON_MODE parseMode(String parameterString) {
        try {
            if (parameterString != null) {
                NexusStreamParser np = new NexusStreamParser(new StringReader(parameterString));
                while (np.peekNextToken() != NexusStreamParser.TT_EOF) {
                    if (np.peekMatchIgnoreCase("mode=")) {
                        np.matchIgnoreCase("mode=");
                        return COMPARISON_MODE.valueOfIgnoreCase(np.getWordRespectCase());
                    } else np.getWordRespectCase(); // skip
                }
            }
        } catch (Exception ignored) {
        }
        return COMPARISON_MODE.ABSOLUTE;
    }

    /**
     * Convenience method: gets the normalization number encoded in the parameter string
     *
     * @param parameterString
     * @return number of reads normalized by
     */
    public static int parseNormalizedTo(String parameterString) {
        try {
            if (parameterString != null) {
                NexusStreamParser np = new NexusStreamParser(new StringReader(parameterString));
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
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * set the comparison mode
     *
     * @param mode
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
