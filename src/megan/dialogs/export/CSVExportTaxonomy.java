/*
 * CSVExportTaxonomy.java Copyright (C) 2023 Daniel H. Huson
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
package megan.dialogs.export;

import jloda.util.CanceledException;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.algorithms.ActiveMatches;
import megan.algorithms.TaxonPathAssignment;
import megan.classification.Classification;
import megan.core.Director;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.*;
import java.util.*;

/**
 * export taxonomy related stuff in CVS format
 * Daniel Huson, 4.2010
 */
class CSVExportTaxonomy {
    /**
     * export taxon name to counts mapping
     *
     * @return lines written
     */
    public static int exportTaxon2TotalLength(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        var totalLines = 0;
        final var viewer = dir.getMainViewer();

        try (var w = new BufferedWriter(new FileWriter(file))) {
            final var connector = viewer.getDir().getDocument().getConnector();
            final var taxonIds = viewer.getSelectedNodeIds();

            progressListener.setSubtask("Taxa to total length");
            progressListener.setMaximum(taxonIds.size());
            progressListener.setProgress(0);

            for (var taxonId : taxonIds) {
                final Collection<Integer> allBelow;
                final var v = viewer.getTaxId2Node(taxonId);
                if (v.getOutDegree() == 0)
                    allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                else
                    allBelow = Collections.singletonList(taxonId);

                final var name = getTaxonLabelSource(format, taxonId);
                w.write(name);
                long length = 0L;

                try (var it = connector.getReadsIteratorForListOfClassIds(viewer.getClassName(), allBelow, 0, 10000, true, false)) {
                    while (it.hasNext()) {
                        length += it.next().getReadLength();
                        progressListener.checkForCancel();
                    }
                    w.write(separator + "" + length + "\n");
                    totalLines++;
                }
                progressListener.incrementProgress();
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export taxon name to counts mapping
     *
     * @return lines written
     */
    public static int exportTaxon2Counts(String format, Director dir, File file, char separator, boolean reportSummarized, ProgressListener progressListener) throws IOException {
        var totalLines = 0;
        final var viewer = dir.getMainViewer();

        try (var w = new BufferedWriter(new FileWriter(file))) {
            final var names = viewer.getDir().getDocument().getSampleNames();
            if (names.size() > 1) {
                w.write("#Datasets");
                for (var name : names) {
                    if (name == null)
                        System.err.println("Internal error, sample name is null");
                    else {
                        if (separator == ',')
                            name = name.replaceAll(",", "_");
                    }
                    w.write(separator + name);
                }
                w.write("\n");
            }

            final var selected = viewer.getSelectedNodes();

            progressListener.setSubtask("Taxa to counts");
            progressListener.setMaximum(selected.size());
            progressListener.setProgress(0);

            for (var v = selected.getFirstElement(); v != null; v = selected.getNextElement(v)) {
                var taxonId = (Integer) v.getInfo();
                if (taxonId != null) {
                    final var data = viewer.getNodeData(v);
                    final var counts = (reportSummarized || v.getOutDegree() == 0 ? data.getSummarized() : data.getAssigned());
                    final var name = getTaxonLabelSource(format, taxonId);
                    if (counts.length == names.size()) {
                        w.write(name);
                        for (var num : counts)
                            w.write(separator + "" + num);
                        w.write("\n");
                        totalLines++;
                    } else
                        System.err.println("Skipped " + name + ", number of values: " + counts.length);
                }
                progressListener.incrementProgress();
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export readid to taxon names mapping
     *
     * @return lines written
     */
    public static int exportReadName2Taxon(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        var totalLines = 0;
        final var viewer = dir.getMainViewer();

        try (var w = new BufferedWriter(new FileWriter(file))) {
            final var connector = viewer.getDir().getDocument().getConnector();
            final var taxonIds = viewer.getSelectedNodeIds();

            progressListener.setSubtask("Read names to taxa");
            progressListener.setMaximum(taxonIds.size());
            progressListener.setProgress(0);

            final var wantMatches = (format.endsWith("PathKPCOFGS")); // PathPercent has been disabled

            for (var taxonId : taxonIds) {
                final var seen = new HashSet<Long>();
                final Collection<Integer> allBelow;
                var v = viewer.getTaxId2Node(taxonId);
                if (v.getOutDegree() == 0)
                    allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                else
                    allBelow = Collections.singletonList(taxonId);

                try (var it = connector.getReadsIteratorForListOfClassIds(viewer.getClassName(), allBelow, 0, 10000, true, wantMatches)) {
                    while (it.hasNext()) {
                        final var readBlock = it.next();
                        final var uid = readBlock.getUId();
                        if (!seen.contains(uid)) {
                            if (uid != 0)
                                seen.add(uid);
                            w.write(readBlock.getReadName() + separator + getTaxonLabelTarget(dir, format, taxonId, readBlock) + "\n");
                            totalLines++;
                        }
                        progressListener.checkForCancel();
                    }
                }
                progressListener.incrementProgress();
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export readid to matches mapping
     *
     * @return lines written
     */
    public static int exportReadName2Matches(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        var totalLines = 0;
        final  var viewer = dir.getMainViewer();

        try (var w = new BufferedWriter(new FileWriter(file))) {
            final var connector = viewer.getDir().getDocument().getConnector();
            final var taxonIds = viewer.getSelectedNodeIds();

            w.write("# read-name"+separator+"taxon-id"+separator+"match-length"+separator+"bit-score"+separator+"percent-identity\n");

            progressListener.setSubtask("Read names to matches");

            if (taxonIds.size() > 0) {
                progressListener.setMaximum(taxonIds.size());
                progressListener.setProgress(0);

                for (var taxonId : taxonIds) {
                    final var seen = new HashSet<Long>();
                    final Collection<Integer> allBelow;
                    final var v = viewer.getTaxId2Node(taxonId);
                    if (v.getOutDegree() == 0)
                        allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                    else
                        allBelow = Collections.singletonList(taxonId);

                    try (var it = connector.getReadsIteratorForListOfClassIds(viewer.getClassName(), allBelow, 0, 10000, true, true)) {
                        while (it.hasNext()) {
                            final var readBlock = it.next();
                            final var uid = readBlock.getUId();
                            if (!seen.contains(uid)) {
                                if (uid != 0)
                                    seen.add(uid);
                                writeMatches(separator, readBlock.getReadName(), readBlock, w);
                                totalLines++;
                            }
                            progressListener.checkForCancel();
                        }
                    }
                    progressListener.incrementProgress();
                }
            } else // process all reads:
            {
                progressListener.setMaximum(viewer.getDir().getDocument().getNumberOfReads());
                progressListener.setProgress(0);

                try (var it = connector.getAllReadsIterator(0, 10000, true, true)) {
                    while (it.hasNext()) {
                        final var readBlock = it.next();
                        writeMatches(separator, readBlock.getReadName(), readBlock, w);
                        totalLines++;
                        progressListener.incrementProgress();
                    }
                }
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * write readname and matches to taxa
     *
	 */
    private static void writeMatches(char separator, String readName, IReadBlock readBlock, Writer w) throws IOException {
        int countMatches = 0;
        if (readBlock.getNumberOfAvailableMatchBlocks() == 0)
            w.write(String.format("%s%c\n", readName, separator));
        else {
            for (IMatchBlock matchBlock : readBlock.getMatchBlocks()) {
                w.write(String.format("%s%c%d%c%d%c%.1f%c%.1f\n", readBlock.getReadName(), separator, matchBlock.getTaxonId(), separator, matchBlock.getLength(), separator, matchBlock.getBitScore(), separator, matchBlock.getPercentIdentity()));
                countMatches++;
            }
        }
    }
    /**
     * export taxon name to read-ids mapping
     *
     * @return lines written
     */
    public static int exportTaxon2ReadNames(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        final MainViewer viewer = dir.getMainViewer();

        try (final var w = new BufferedWriter(new FileWriter(file))) {
            final var connector = viewer.getDir().getDocument().getConnector();
            final var taxonIds = viewer.getSelectedNodeIds();

            progressListener.setSubtask("Taxa to read names");
            progressListener.setMaximum(taxonIds.size());
            progressListener.setProgress(0);

            for (var taxonId : taxonIds) {
                final Collection<Integer> allBelow;
                final var v = viewer.getTaxId2Node(taxonId);
                if (v.getOutDegree() == 0)
                    allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                else
                    allBelow = Collections.singletonList(taxonId);

                final var name = getTaxonLabelSource(format, taxonId);
                w.write(name);

                try (var it = connector.getReadsIteratorForListOfClassIds(viewer.getClassName(), allBelow, 0, 10000, true, false)) {
                    while (it.hasNext()) {
                        var readId = it.next().getReadName();
                        w.write(separator + "" + readId);
                        progressListener.checkForCancel();
                    }
                    w.write("\n");
                    totalLines++;
                }
                progressListener.incrementProgress();
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export taxon-path to number of reads assigned
     *
     * @return lines written
	 */
    public static int exportTaxon2ReadIds(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        var totalLines = 0;
        final var viewer = dir.getMainViewer();

        try (var w = new BufferedWriter(new FileWriter(file))) {
            final var connector = viewer.getDir().getDocument().getConnector();
            final var taxonIds = viewer.getSelectedNodeIds();

            progressListener.setSubtask("Taxa to read Ids");
            progressListener.setMaximum(taxonIds.size());
            progressListener.setProgress(0);

            for (var taxonId : taxonIds) {
                final Collection<Integer> allBelow;
                final var v = viewer.getTaxId2Node(taxonId);
                if (v.getOutDegree() == 0)
                    allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                else
                    allBelow = Collections.singletonList(taxonId);

                final var name = getTaxonLabelSource(format, taxonId);
                w.write(name);

                try (var it = connector.getReadsIteratorForListOfClassIds(viewer.getClassName(), allBelow, 0, 10000, true, false)) {
                    while (it.hasNext()) {
                        var readId = it.next().getReadName();
                        w.write(separator + "" + readId);
                        progressListener.checkForCancel();
                    }
                    w.write("\n");
                    totalLines++;
                }
                progressListener.incrementProgress();
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * determines which type of label is desired
     *
     * @return label type
     */
    private static String getTaxonLabelSource(String format, int taxonId) {
        if (format.startsWith("taxonName"))
			return StringUtils.getInCleanQuotes(TaxonomyData.getName2IdMap().get(taxonId));
        else if (format.startsWith("taxonPath"))
			return StringUtils.getInCleanQuotes(getPath(taxonId, false));
        else if (format.startsWith("taxonPathKPCOFGS"))
            return getPath(taxonId, true);
        else if (format.startsWith("taxonRank")) {
            final String rankName = TaxonomicLevels.getName(TaxonomyData.getName2IdMap().getRank(taxonId));
            if (rankName != null)
				return rankName + ":" + StringUtils.getInCleanQuotes(TaxonomyData.getName2IdMap().get(taxonId));
            else
				return "No_rank:" + StringUtils.getInCleanQuotes(TaxonomyData.getName2IdMap().get(taxonId));
        } else
            return "" + taxonId;
    }

    /**
     * determines which type of label is desired
     *
     * @return label type
     */
    private static String getTaxonLabelTarget(Director dir, String format, int taxonId, IReadBlock readBlock) {
        if (format.endsWith("taxonName"))
			return StringUtils.getInCleanQuotes(TaxonomyData.getName2IdMap().get(taxonId));
        else if (format.endsWith("taxonPath"))
			return StringUtils.getInCleanQuotes(getPath(taxonId, false));
        else if (format.endsWith("taxonPathKPCOFGS"))
            return getPath(taxonId, true);
        else if (format.endsWith("taxonPathPercent")) // // PathPercent has been disabled
            return getPathPercent(dir, readBlock);
        else
            return "" + taxonId;
    }

    /**
     * gets the full path to the named taxon
     *
	 */
    private static String getPath(int taxonId, boolean majorRanksWithPrefixes) {
        final var path = new LinkedList<String>();
        var v = TaxonomyData.getTree().getANode(taxonId);
        while (v != null && v.getInfo() != null) {
            taxonId = (Integer) v.getInfo();
            if (!majorRanksWithPrefixes) {
                var name = TaxonomyData.getName2IdMap().get(taxonId);
                path.add(name);
            } else {
                final var rank = TaxonomyData.getTaxonomicRank(taxonId);
                if (TaxonomicLevels.isMajorRank(rank)) {
                    var name = TaxonomicLevels.getOneLetterCodeFromRank(rank) + "__" + TaxonomyData.getName2IdMap().get(taxonId);
                    path.add(name);
                }
            }
            if (v.getInDegree() > 0)
                v = v.getFirstInEdge().getSource();
            else
                v = null;
        }
        final var buf = new StringBuilder();
        final var array = path.toArray(new String[0]);
        for (var i = array.length - 1; i >= 0; i--) {
            buf.append(array[i].replaceAll(";", "_")).append(";");
        }
        return buf.toString();
    }

    /**
     * gets the full path to the named taxon with percent
     *
     * @return path
     * @deprecated
     */
    @Deprecated
    private static String getPathPercent(Director dir, IReadBlock readBlock) {
        final var doc = dir.getDocument();
        final var activeMatchesForTaxa = new BitSet();
        ActiveMatches.compute(doc.getMinScore(), Math.max(0.0001f, doc.getTopPercent()), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, Classification.Taxonomy, activeMatchesForTaxa);
        return TaxonPathAssignment.getPathAndPercent(readBlock, activeMatchesForTaxa, false, true, true, true);
    }
}
