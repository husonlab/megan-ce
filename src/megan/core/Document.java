/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.core;

import jloda.gui.ColorTableManager;
import jloda.gui.ILabelGetter;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import megan.algorithms.DataProcessor;
import megan.chart.ChartColorManager;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.data.SyncDataTableAndClassificationViewer;
import megan.daa.connector.DAAConnector;
import megan.daa.io.DAAParser;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.parsers.blast.BlastMode;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.SyncDataTableAndTaxonomy;
import megan.viewer.TaxonomyData;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * The main document
 * Daniel Huson    11.2005
 */
public class Document {
    public enum LCAAlgorithm {
        Naive, Weighted, NaiveLongReads;

        public static LCAAlgorithm valueOfIgnoreCase(String str) {
            for (LCAAlgorithm lcaAlgorithm : values()) {
                if (lcaAlgorithm.toString().equalsIgnoreCase(str))
                    return lcaAlgorithm;
            }
            return null;
        }
    }
    final static Map<String, String> name2versionInfo = new HashMap<>(); // used to track versions of tree etc

    private long numberReads = 0;
    private long additionalReads = 0;
    public boolean neverOpenedReads = true;

    private final DataTable dataTable = new DataTable();
    private final SelectionSet sampleSelection = new SelectionSet();

    private boolean dirty = false; // to true when imported from blast files

    public final static float DEFAULT_MINSCORE = 50;    // this is aimed at reads of length 100
    public final static float DEFAULT_MAXEXPECTED = 0.01f; // maximum e-value
    public final static float DEFAULT_MIN_PERCENT_IDENTITY = 0f;
    public final static float DEFAULT_TOPPERCENT = 10; // in percent
    public final static int DEFAULT_MINSUPPORT = 0;
    public final static float DEFAULT_MINSUPPORT_PERCENT = 0.01f; // in percent
    public static final LCAAlgorithm DEFAULT_LCA_ALGORITHM = LCAAlgorithm.Naive;
    public final static float DEFAULT_WEIGHTED_LCA_PERCENT = 80f;
    public final static float DEFAULT_MINCOMPLEXITY = 0f;
    public static final boolean DEFAULT_USE_IDENTITY = false;
    public static final boolean DEFAULT_LONG_READS = false;


    private float minScore = DEFAULT_MINSCORE;
    private float maxExpected = DEFAULT_MAXEXPECTED;
    private float minPercentIdentity = DEFAULT_MIN_PERCENT_IDENTITY;
    private float topPercent = DEFAULT_TOPPERCENT;
    private float minSupportPercent = DEFAULT_MINSUPPORT_PERCENT; // if this is !=0, overrides explicit minSupport value and uses percentage of assigned reads
    private int minSupport = DEFAULT_MINSUPPORT; // min summary count that a node needs to make it into the induced taxonomy

    private LCAAlgorithm lcaAlgorithm = DEFAULT_LCA_ALGORITHM;
    private float weightedLCAPercent = DEFAULT_WEIGHTED_LCA_PERCENT;

    private float minComplexity = DEFAULT_MINCOMPLEXITY;

    private boolean useIdentityFilter = DEFAULT_USE_IDENTITY;

    private boolean longReads = DEFAULT_LONG_READS;

    private long lastRecomputeTime = 0;

    private final MeganFile meganFile = new MeganFile();

    private ProgressListener progressListener = new ProgressCmdLine(); // for efficiency, allow only one

    private boolean pairedReads = false; // treat reads as paired

    private int significanceTestCorrection = -1;
    private boolean highlightContrasts = false;

    private Director dir;
    private final SampleAttributeTable sampleAttributeTable = new SampleAttributeTable();
    private ChartColorManager chartColorManager;
    private final ILabelGetter sampleLabelGetter;

    private Color[] colorsArray = null;

    // set of active fViewers
    private final Set<String> activeViewers = new HashSet<>();
    private int pairedReadSuffixLength;
    private boolean openDAAFileOnlyIfMeganized = true;

    /**
     * constructor
     */
    public Document() {
        //fullTaxonomy.getInduceTaxonomy(collapsedTaxa, false, inducedTaxonomy);
        setupChartColorManager(false);

        sampleLabelGetter = new ILabelGetter() {
            public String getLabel(String name) {
                String label = getSampleAttributeTable().getSampleLabel(name);
                if (label != null)
                    return label;
                    //   return label + " (" + Basic.abbreviateDotDotDot(name, 15) + ")";
                else
                    return name;
            }
        };
        setWeightedLCAPercent((float) ProgramProperties.get("WeightedLCAPercent", DEFAULT_WEIGHTED_LCA_PERCENT));
    }

    public Director getDir() {
        return dir;
    }

    public void setDir(Director dir) {
        this.dir = dir;
    }

    /**
     * setup the chart color manager
     * @param useProgramColorTable
     */
    public void setupChartColorManager(boolean useProgramColorTable) {
        ChartColorManager.initialize();
        if (!useProgramColorTable) {
            chartColorManager = new ChartColorManager(ColorTableManager.getDefaultColorTable());
            chartColorManager.setHeatMapTable(ColorTableManager.getDefaultColorTableHeatMap().getName());
            chartColorManager.setSeriesOverrideColorGetter(new ChartColorManager.ColorGetter() {
                public Color get(String label) {
                    return getSampleAttributeTable().getSampleColor(label);
                }
            });
        } else {
            chartColorManager = ChartColorManager.programChartColorManager;
        }
    }

    /**
     * erase all reads
     */
    public void clearReads() {
        dataTable.clear();
        setNumberReads(0);
    }

    /**
     * load data from the set file
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void loadMeganFile() throws IOException, CanceledException {
        clearReads();
        getProgressListener().setTasks("Loading MEGAN File", getMeganFile().getName());
        if (getMeganFile().hasDataConnector()) {
            final IConnector connector = getConnector();
            SyncArchiveAndDataTable.syncArchive2Summary(meganFile.getFileName(), connector, dataTable, sampleAttributeTable);

            if (dataTable.getTotalReads() == 0 && connector.getNumberOfReads() > 0) {
                SyncArchiveAndDataTable.syncRecomputedArchive2Summary(getMeganFile().getName(), "merge", dataTable.getBlastMode(), "", connector, dataTable, 0);
            }
            setNumberReads(getDataTable().getTotalReads());
            setAdditionalReads(getDataTable().getAdditionalReads());
            getActiveViewers().clear();
            getActiveViewers().addAll(Arrays.asList(connector.getAllClassificationNames()));
            String parameters = getDataTable().getParameters();
            if (parameters != null) {
                parseParameterString(parameters);
            }
            getSampleAttributeTable().addAttribute(SampleAttributeTable.HiddenAttribute.Source.toString(), getMeganFile().getFileName(), true);
        } else if (getMeganFile().isMeganSummaryFile()) {
            loadMeganSummaryFile();
        } else
            throw new IOException("File format not (or no longer) supported");
        loadColorTableFromDataTable();

        lastRecomputeTime = System.currentTimeMillis();
        colorsArray = new Color[getNumberOfSamples()];
    }

    /**
     * load color table from data table
     */
    public void loadColorTableFromDataTable() {
        if (getDataTable().getColorTable() != null) {
            getChartColorManager().setColorTable(getDataTable().getColorTable(), getDataTable().isColorByPosition());
            if (getDataTable().getColorTableHeatMap() != null) {
                getChartColorManager().setHeatMapTable(getDataTable().getColorTableHeatMap());
                getDataTable().setColorTableHeatMap(getChartColorManager().getHeatMapTable().getName()); // this ensures that we save a valid name back to the file
            }
        }
        if (!getChartColorManager().isUsingProgramColors())
            getChartColorManager().loadColorEdits(getDataTable().getColorEdits());
    }

    /**
     * parse an algorithm parameter string
     *
     * @param parameters
     */
    public void parseParameterString(String parameters) {
        if (parameters != null && parameters.length() > 0) {
            try {
                NexusStreamParser np = new NexusStreamParser(new StringReader(parameters));
                List<String> tokens = np.getTokensRespectCase(null, null);

                setMinScore(np.findIgnoreCase(tokens, "minScore=", getMinScore()));
                setMaxExpected(np.findIgnoreCase(tokens, "maxExpected=", getMaxExpected()));
                setMinPercentIdentity(np.findIgnoreCase(tokens, "minPercentIdentity=", getMinPercentIdentity()));
                setTopPercent(np.findIgnoreCase(tokens, "topPercent=", getTopPercent()));
                setMinSupportPercent(np.findIgnoreCase(tokens, "minSupportPercent=", 0f));
                setMinSupport((int) np.findIgnoreCase(tokens, "minSupport=", getMinSupport()));
                if (np.findIgnoreCase(tokens, "weightedLCA=true", true, false))
                    setLcaAlgorithm(LCAAlgorithm.Weighted);
                else if (np.findIgnoreCase(tokens, "weightedLCA=false", true, false))
                    setLcaAlgorithm(LCAAlgorithm.Naive);
                else if (np.findIgnoreCase(tokens, "lcaAlgorithm=" + LCAAlgorithm.Naive.toString()))
                    setLcaAlgorithm(LCAAlgorithm.Naive);
                else if (np.findIgnoreCase(tokens, "lcaAlgorithm=" + LCAAlgorithm.Weighted.toString()))
                    setLcaAlgorithm(LCAAlgorithm.Weighted);
                else if (np.findIgnoreCase(tokens, "lcaAlgorithm=" + LCAAlgorithm.NaiveLongReads.toString()))
                    setLcaAlgorithm(LCAAlgorithm.NaiveLongReads);

                setWeightedLCAPercent(np.findIgnoreCase(tokens, "weightedLCAPercent=", getWeightedLCAPercent()));
                setMinComplexity(np.findIgnoreCase(tokens, "minComplexity=", getMinComplexity()));

                if (np.findIgnoreCase(tokens, "longReads=true", true, false))
                    setLongReads(true);
                else if (np.findIgnoreCase(tokens, "longReads=false", true, false))
                    setLongReads(false);

                if (np.findIgnoreCase(tokens, "pairedReads=true", true, false))
                    setPairedReads(true);
                else if (np.findIgnoreCase(tokens, "pairedReads=false", true, false))
                    setPairedReads(false);

                if (np.findIgnoreCase(tokens, "identityFilter=true", true, false))
                    setUseIdentityFilter(true);
                else if (np.findIgnoreCase(tokens, "identityFilter=false", true, false))
                    setUseIdentityFilter(false);

                {
                    String fNamesString = (np.findIgnoreCase(tokens, "fNames=", "{", "}", "").trim());
                    if (fNamesString.length() > 0) {
                        final Set<String> cNames = new HashSet<>();
                        cNames.addAll(Arrays.asList(fNamesString.split("\\s+")));
                        if (cNames.size() > 0) {
                            getActiveViewers().clear();
                            for (final String cName : cNames) {
                                if (cName.length() > 0) {
                                    if (ClassificationManager.getAllSupportedClassifications().contains(cName))
                                        getActiveViewers().add(cName);
                                    else
                                        System.err.println("Unknown classification name: '" + cName + "': ignored");
                                }
                            }
                            if (!getActiveViewers().contains(Classification.Taxonomy))
                                getActiveViewers().add(Classification.Taxonomy);
                        }
                    }
                }
            } catch (IOException e) {
                Basic.caught(e);
            }
        }
    }

    /**
     * write an algorithm parameter string
     *
     * @return parameter string
     */
    public String getParameterString() {
        StringBuilder buf = new StringBuilder();
        buf.append("minScore=").append(getMinScore());
        buf.append(" maxExpected='").append(getMaxExpected()).append("'");
        buf.append(" minPercentIdentity='").append(getMinPercentIdentity()).append("'");
        buf.append(" topPercent=").append(getTopPercent());
        buf.append(" minSupportPercent=").append(getMinSupportPercent());
        buf.append(" minSupport=").append(getMinSupport());
        buf.append(" lcaAlgorithm=").append(getLcaAlgorithm().toString());
        if (getLcaAlgorithm().equals(LCAAlgorithm.Weighted))
            buf.append(" weightedLCAPercent=").append(getWeightedLCAPercent());
        buf.append(" minComplexity=").append(getMinComplexity());
        if (isLongReads())
            buf.append(" longReads=true");
        if (isPairedReads())
            buf.append(" pairedReads=true");
        if (isUseIdentityFilter())
            buf.append(" identityFilter=true");
        if (getActiveViewers().size() > 0) {
            buf.append(" fNames= {");
            for (String cName : getActiveViewers()) {
                buf.append(" ").append(cName);
            }
            buf.append(" }");
        }
        return buf.toString();
    }

    /**
     * load the set megan summary file
     *
     * @throws IOException
     */
    public void loadMeganSummaryFile() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Basic.getInputStreamPossiblyZIPorGZIP(getMeganFile().getFileName())));
        getDataTable().read(reader, false);
        getSampleAttributeTable().read(reader, getSampleNames(), true);
        reader.close();
        String parameters = getDataTable().getParameters();
        if (parameters != null) {
            parseParameterString(parameters);
        }
        getActiveViewers().clear();
        getActiveViewers().addAll(getDataTable().getClassification2Class2Counts().keySet());
        loadColorTableFromDataTable();
    }

    /**
     * get the sample selection
     *
     * @return sample selection
     */
    public SelectionSet getSampleSelection() {
        return sampleSelection;
    }

    /**
     * get the set progress listener
     *
     * @return progress listener
     */
    public ProgressListener getProgressListener() {
        return progressListener;
    }

    /**
     * set the progress listener
     *
     * @param progressListener
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * gets the document title
     *
     * @return title
     */
    public String getTitle() {
        return getMeganFile().getName();
    }


    public float getMinScore() {
        return minScore;
    }

    public void setMinScore(float minScore) {
        this.minScore = minScore;
    }

    public float getMaxExpected() {
        return maxExpected;
    }

    public void setMaxExpected(float maxExpected) {
        this.maxExpected = maxExpected;
    }

    public float getMinPercentIdentity() {
        return minPercentIdentity;
    }

    public void setMinPercentIdentity(float minPercentIdentity) {
        this.minPercentIdentity = minPercentIdentity;
    }

    public float getTopPercent() {
        return topPercent;
    }

    public void setTopPercent(float topPercent) {
        this.topPercent = topPercent;
    }

    public boolean isLongReads() {
        return longReads;
    }

    public void setLongReads(boolean longReads) {
        this.longReads = longReads;
    }

    /**
     * process the given reads
     */
    public void processReadHits() throws CanceledException {
        if (getMeganFile().hasDataConnector()) {
            try {
                final int readsFound = DataProcessor.apply(this);

                // rescan size:
                {
                    getSampleAttributeTable().addAttribute("Size", numberReads, true);
                }

                try {
                    saveAuxiliaryData();
                } catch (IOException e) {
                    Basic.caught(e);
                }

                if (readsFound > getNumberOfReads()) // rounding in comparison mode may cause an increase by 1
                    setNumberReads(readsFound);
                if (getNumberOfReads() == 0 && getDir() != null)
                    getDir().getMainViewer().collapseToDefault();


                if (sampleAttributeTable.getSampleOrder().size() == 0)
                    sampleAttributeTable.setSampleOrder(getSampleNames());
            } finally {
                getProgressListener().setCancelable(true);
            }
        }
        lastRecomputeTime = System.currentTimeMillis();
    }

    /**
     * writes auxiliary data to archive
     *
     * @throws IOException
     */
    public void saveAuxiliaryData() throws IOException {
        if (getMeganFile().hasDataConnector() && !getMeganFile().isReadOnly()) {
            if (dir != null) {
                final MainViewer mainViewer = dir.getMainViewer();
                if (mainViewer != null)
                    SyncDataTableAndTaxonomy.syncFormattingFromViewer2Summary(mainViewer, getDataTable());
                for (String cName : ClassificationManager.getAllSupportedClassifications()) {
                    if (dir.getViewerByClassName(ClassificationViewer.getClassName(cName)) != null && dir.getViewerByClassName(ClassificationViewer.getClassName(cName)) instanceof ClassificationViewer) {
                        ClassificationViewer classificationViewer = (ClassificationViewer) dir.getViewerByClassName(ClassificationViewer.getClassName(cName));
                        SyncDataTableAndClassificationViewer.syncFormattingFromViewer2Summary(classificationViewer, getDataTable());
                    }
                }
            }

            getDataTable().setColorTable(getChartColorManager().getColorTableName(), getChartColorManager().isColorByPosition(), getChartColorManager().getHeatMapTable().getName());
            getDataTable().setColorEdits(getChartColorManager().getColorEdits());

            byte[] userState = getDataTable().getUserStateAsBytes();
            byte[] sampleAttributes = getSampleAttributeTable().getBytes();

            Map<String, byte[]> label2data = new HashMap<>();
            label2data.put(SampleAttributeTable.USER_STATE, userState);
            label2data.put(SampleAttributeTable.SAMPLE_ATTRIBUTES, sampleAttributes);
            getMeganFile().getConnector().putAuxiliaryData(label2data);
        }
    }

    /**
     * get the minimal number of reads required to keep a given taxon
     *
     * @return min support
     */
    public int getMinSupport() {
        return minSupport;
    }

    /**
     * set the minimal number of reads required to hit a givent taxon
     *
     * @param minSupport
     */
    public void setMinSupport(int minSupport) {
        this.minSupport = minSupport;
    }

    /**
     * gets the min support percentage value. If this is non-zero, overrides min support value and uses
     * given percentage of assigned reads
     *
     * @return min support percentage
     */
    public float getMinSupportPercent() {
        return minSupportPercent;
    }

    /**
     * gets the min support percentage
     *
     * @param minSupportPercent
     */
    public void setMinSupportPercent(float minSupportPercent) {
        this.minSupportPercent = minSupportPercent;
    }

    /**
     * gets the minimum complexity required of a read
     *
     * @return min complexity
     */
    public float getMinComplexity() {
        return minComplexity;
    }

    /**
     * set the minimum complexity for a read
     *
     * @param minComplexity
     */
    public void setMinComplexity(float minComplexity) {
        this.minComplexity = minComplexity;
    }

    public void setLcaAlgorithm(LCAAlgorithm lcaAlgorithm) {
        this.lcaAlgorithm = lcaAlgorithm;
    }

    public LCAAlgorithm getLcaAlgorithm() {
        return lcaAlgorithm;
    }

    /**
     * is document dirty
     *
     * @return true, if dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * set the dirty state
     *
     * @param dirty
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * get number of reads
     *
     * @return number of reads
     */
    public long getNumberOfReads() {
        return numberReads;
    }

    /**
     * set the number of reads
     *
     * @param numberReads
     */
    public void setNumberReads(long numberReads) {
        this.numberReads = numberReads;
        getDataTable().setTotalReads(numberReads);
    }

    /**
     * get additional  reads
     *
     * @return additional of reads
     */
    public long getAdditionalReads() {
        return additionalReads;
    }

    /**
     * set the additional of reads
     *
     * @param additionalReads
     */
    public void setAdditionalReads(long additionalReads) {
        this.additionalReads = additionalReads;
        getDataTable().setAdditionalReads(additionalReads);
    }

    /**
     * gets the list of samples
     *
     * @return sample names
     */
    public List<String> getSampleNames() {
        return Arrays.asList(getSampleNamesAsArray());
    }

    /**
     * get sample names as array. This also ensures that for a file containing only one sample, the sample name always reflects
     * the current file name
     *
     * @return array
     */
    public String[] getSampleNamesAsArray() {
        return getDataTable().getSampleNames().clone();
    }

    /**
     * get the number of samples associated with this document
     *
     * @return number of samples
     */
    public int getNumberOfSamples() {
        return getSampleNamesAsArray().length;
    }

    /**
     * gets the megan4 summary
     *
     * @return megan4 summary
     */
    public DataTable getDataTable() {
        return dataTable;
    }

    /**
     * gets data for taxon chart. Used by the attributes chart
     *
     * @param label2series2value
     */
    public void getTaxonName2DataSet2SummaryCount(Map<String, Map<String, Number>> label2series2value) {
        final List<String> samples = getSampleNames();
        final String[] id2sample = new String[samples.size()];
        for (int i = 0; i < samples.size(); i++)
            id2sample[i] = samples.get(i);

        final Map<Integer, float[]> class2counts = dataTable.getClassification2Class2Counts().get(ClassificationType.Taxonomy.toString());
        if (class2counts != null) {
            for (Integer taxId : class2counts.keySet()) {
                String taxonName = TaxonomyData.getName2IdMap().get(taxId);
                if (taxonName != null) {
                    Map<String, Number> series2value = label2series2value.get(taxonName);
                    if (series2value == null) {
                        series2value = new TreeMap<>();
                        label2series2value.put(taxonName, series2value);
                    }
                    float[] counts = class2counts.get(taxId);
                    if (counts != null) {
                        for (int i = 0; i < counts.length; i++) {
                            series2value.put(id2sample[i], counts[i]);
                        }
                    }
                }
            }
        }
    }

    /**
     * load the version of some file. Assumes the file ends on .info
     *
     * @param fileName
     */
    static public void loadVersionInfo(String name, String fileName) {
        try {
            fileName = Basic.replaceFileSuffix(fileName, ".info");
            InputStream ins = ResourceManager.getFileAsStream(fileName);
            BufferedReader r = new BufferedReader(new InputStreamReader(ins));
            StringBuilder buf = new StringBuilder();
            String aLine;
            while ((aLine = r.readLine()) != null)
                buf.append(aLine).append("\n");
            r.close();
            name2versionInfo.put(name, buf.toString());
        } catch (Exception ex) {
            //System.err.println("No version info for: " + name);
            name2versionInfo.put(name, null);
        }
    }

    /**
     * gets the name 2 version info table
     *
     * @return version info
     */
    static public Map<String, String> getVersionInfo() {
        return name2versionInfo;
    }

    public float getWeightedLCAPercent() {
        return weightedLCAPercent;
    }

    public void setWeightedLCAPercent(float weightedLCAPercent) {
        this.weightedLCAPercent = weightedLCAPercent;
    }


    public long getLastRecomputeTime() {
        return lastRecomputeTime;
    }

    public void setLastRecomputeTime(long time) {
        lastRecomputeTime = time;
    }

    public void setSignificanceTestCorrection(int correction) {
        significanceTestCorrection = correction;
    }

    public int getSignificanceTestCorrection() {
        return significanceTestCorrection;
    }

    public boolean isPairedReads() {
        return pairedReads;
    }

    public void setPairedReads(boolean pairedReads) {
        this.pairedReads = pairedReads;
    }

    public MeganFile getMeganFile() {
        return meganFile;
    }

    public boolean isUseIdentityFilter() {
        return useIdentityFilter;
    }

    public void setUseIdentityFilter(boolean useIdentityFilter) {
        this.useIdentityFilter = useIdentityFilter;
    }

    public boolean isHighlightContrasts() {
        return highlightContrasts;
    }

    public void setHighlightContrasts(boolean highlightContrasts) {
        this.highlightContrasts = highlightContrasts;
    }

    public SampleAttributeTable getSampleAttributeTable() {
        //if (sampleAttributeTable.getSampleOrder().size() == 0)
        //     sampleAttributeTable.getSampleOrder().addAll(getSampleNames());
        return sampleAttributeTable;
    }

    public ChartColorManager getChartColorManager() {
        return chartColorManager;
    }

    public void setChartColorManager(ChartColorManager chartColorManager) {
        this.chartColorManager = chartColorManager;
    }

    public Color getColorByIndex(int i) {
        if (colorsArray == null || i >= colorsArray.length)
            return Color.BLACK;
        else
            return colorsArray[i];
    }

    public Color[] getColorsArray() {
        if (colorsArray == null || colorsArray.length < getNumberOfSamples())
            colorsArray = new Color[getNumberOfSamples()];
        return colorsArray;
    }

    /**
     * gets the set of active functional viewers
     *
     * @return active functional viewers
     */
    public Set<String> getActiveViewers() {
        return activeViewers;
    }

    public void setPairedReadSuffixLength(int pairedReadSuffixLength) {
        this.pairedReadSuffixLength = pairedReadSuffixLength;
    }

    public int getPairedReadSuffixLength() {
        return pairedReadSuffixLength;
    }

    /**
     * gets the blast mode. If it is not yet known, infers it and sets the data table attribute, if necessary
     *
     * @return blast mode
     */
    public BlastMode getBlastMode() {
        BlastMode blastMode = dataTable.getBlastMode();
        if (blastMode == BlastMode.Unknown && meganFile.isDAAFile()) {
            dataTable.setBlastMode(0, DAAParser.getBlastMode(meganFile.getFileName()));
        }
        if (blastMode == BlastMode.Unknown && meganFile.hasDataConnector()) {
            try (IReadBlockIterator it = meganFile.getConnector().getAllReadsIterator(1, 10, true, true)) {
                while (it.hasNext()) {
                    IReadBlock readBlock = it.next();
                    if (readBlock.getNumberOfAvailableMatchBlocks() > 0) {
                        IMatchBlock matchBlock = readBlock.getMatchBlock(0);
                        if (matchBlock.getText() != null) {
                            if (matchBlock.getText().contains("Frame")) {
                                dataTable.setBlastMode(0, BlastMode.BlastX);
                                break;
                            } else if (matchBlock.getText().contains("Strand")) {
                                dataTable.setBlastMode(0, BlastMode.BlastN);
                                break;
                            } else {
                                dataTable.setBlastMode(0, BlastMode.Classifier);
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Basic.caught(e);
            }
        }
        return dataTable.getBlastMode();
    }

    public void setBlastMode(BlastMode blastMode) {
        dataTable.setBlastMode(0, blastMode);
    }

    // all the sample stuff below here is chaotic and really needs to be reimplemented...

    public void renameSample(String oldName, String newName) throws IOException {
        int pid = Basic.getIndex(oldName, getSampleNames());
        if (pid != -1)
            renameSample(pid + 1, newName);
    }

    public void renameSample(Integer pid, String newName) throws IOException {
        if (getSampleNames().contains(newName))
            throw new IOException("Can't change sample name, name already used: " + newName);
        String currentName = getDataTable().getSampleNames()[pid - 1];
        getDataTable().changeSampleName(pid - 1, newName);
        getSampleAttributeTable().renameSample(currentName, newName, true);
        setDirty(true);
        try {
            processReadHits();
        } catch (CanceledException e) {
            Basic.caught(e);
        }
        if (getDir() != null)
            getDir().getMainViewer().setDoReInduce(true);
    }

    public void duplicateSample(String srcName, String newName) throws IOException {
        if (getSampleNames().contains(newName))
            throw new IOException("Can't duplicate sample, name already used: " + newName);
        getDataTable().duplicateSample(srcName, newName);
        getSampleAttributeTable().duplicateSample(srcName, newName, true);
        setDirty(true);
        try {
            processReadHits();
        } catch (CanceledException e) {
            Basic.caught(e);
        }
        getDir().getMainViewer().setDoReInduce(true);
    }

    public void removeSamples(Set<String> samples) {
        getDataTable().removeSamples(samples);
        for (String sample : samples)
            getSampleAttributeTable().removeSample(sample);
        setDirty(true);
        try {
            processReadHits();
        } catch (CanceledException e) {
            Basic.caught(e);
        }
        if (getDir() != null)
            getDir().getMainViewer().setDoReInduce(true);
    }

    /**
     * merge the given samples to a new sample
     *
     * @param samples
     * @throws IOException
     */
    public void mergeSamples(Set<String> samples, String newName) throws IOException {
        if (getSampleNames().contains(newName))
            throw new IOException("Can't merge samples, name already used: " + newName);
        getDataTable().mergeSamples(samples, newName);
        getSampleAttributeTable().mergeSamples(samples, newName);
        setDirty(true);
        try {
            processReadHits();
        } catch (CanceledException e) {
            Basic.caught(e);
        }
        if (getDir() != null)
            getDir().getMainViewer().setDoReInduce(true);
    }

    /**
     * extract named samples from the given document
     *
     * @param samples
     * @param srcDoc
     */
    public void extractSamples(Collection<String> samples, Document srcDoc) {
        getDataTable().clear();
        srcDoc.getDataTable().extractSamplesTo(samples, getDataTable());

        getSampleAttributeTable().clear();
        getSampleAttributeTable().addTable(srcDoc.getSampleAttributeTable().extractTable(samples), false, true);
        getSampleAttributeTable().getAttributeOrder().clear();
        getSampleAttributeTable().setAttributeOrder(srcDoc.getSampleAttributeTable().getAttributeOrder());
        getSampleAttributeTable().getSampleOrder().clear();
        getSampleAttributeTable().getSampleOrder().addAll(samples);

    }

    /**
     * add named sample to given document
     *
     * @param sample
     * @param docToAdd
     */
    public void addSample(String sample, Document docToAdd) {
        getDataTable().addSample(sample, docToAdd.getDataTable());
        Set<String> samples = new HashSet<>();
        samples.add(sample);
        getSampleAttributeTable().addTable(docToAdd.getSampleAttributeTable().extractTable(samples), false, true);
    }

    /**
     * add named sample to given document
     *
     * @param sample
     * @param classification2class2counts
     */
    public void addSample(String sample, float sampleSize, int srcId, BlastMode blastMode, Map<String, Map<Integer, float[]>> classification2class2counts) {
        getDataTable().addSample(sample, sampleSize, blastMode, srcId, classification2class2counts);
    }

    public void reorderSamples(Collection<String> newOrder) throws IOException {
        getDataTable().reorderSamples(newOrder);
        getDir().getMainViewer().getSeriesList().sync(Arrays.asList(getDataTable().getOriginalSamples()), null, true);
        setDirty(true);
        try {
            processReadHits();
        } catch (CanceledException e) {
            Basic.caught(e);
        }
        getDir().getMainViewer().setDoReInduce(true);
    }

    public float getNumberOfReads(String sample) {
        int i = Basic.getIndex(sample, getDataTable().getSampleNames());
        if (i == -1)
            return -1;
        else
            return getDataTable().getSampleSizes()[i];
    }

    public Set<String> getDisabledSamples() {
        return dataTable.getDisabledSamples();
    }

    public void disableSamples(Set<String> samples) {
        dataTable.disableSamples(samples);
        setLastRecomputeTime(System.currentTimeMillis());
    }

    public void enableSamples(Set<String> samples) {
        dataTable.enableSamples(samples);
        setLastRecomputeTime(System.currentTimeMillis());
    }

    public IConnector getConnector() {
        try {
            IConnector connector = getMeganFile().getConnector(isOpenDAAFileOnlyIfMeganized());
            if (connector instanceof DAAConnector) {
                ((DAAConnector) connector).setLongReads(isLongReads());
            }
            return connector;
        } catch (IOException e) {
            Basic.caught(e);
        }
        return null;
    }

    /**
     * get the sample label getter
     *
     * @return sample label getter
     */
    public ILabelGetter getSampleLabelGetter() {
        return sampleLabelGetter;
    }

    /**
     * close connector, if there is one
     */
    public void closeConnector() {
        if (getMeganFile().hasDataConnector()) {
            try {
                if (isDirty()) {
                    if (getMeganFile().isReadOnly())
                        System.err.println("File is read-only, discarding changes");
                    else {
                        saveAuxiliaryData();
                    }
                }
                MeganFile.removeUIdFromSetOfOpenFiles(getMeganFile().getName(), getMeganFile().getConnector().getUId());
                getMeganFile().setFileName("");
            } catch (IOException e) {
                Basic.caught(e);
            }
        }
    }

    public void setOpenDAAFileOnlyIfMeganized(boolean openDAAFileOnlyIfMeganized) {
        this.openDAAFileOnlyIfMeganized = openDAAFileOnlyIfMeganized;
    }

    public boolean isOpenDAAFileOnlyIfMeganized() {
        return openDAAFileOnlyIfMeganized;
    }
}
