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

package megan.dialogs.export;

import jloda.util.*;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.data.*;
import megan.dialogs.lrinspector.LRInspectorViewer;
import megan.dialogs.lrinspector.TableItem;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * export selected reads and their annotations in GFF format
 * Daniel Huson, 9.2017
 */
public class ExportAlignedReads2GFF3Format {
    /**
     * export aligned reads in GFF3 format
     *
     * @param file
     * @param classificationToReport
     * @param progressListener
     * @return reads and genes written
     */
    public static Pair<Long, Long> apply(final ClassificationViewer cViewer, final File file, final String classificationToReport, final boolean excludeIncompatible, final boolean excludeDominated, final ProgressListener progressListener) throws IOException, CanceledException {
        long countReads = 0;
        long countAlignments = 0;
        final BlastMode blastMode = cViewer.getDir().getDocument().getBlastMode();

        final String[] cNames = cViewer.getDocument().getActiveViewers().toArray(new String[0]);

        System.err.println("Writing file: " + file);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write(getHeader());
            final IConnector connector = cViewer.getDocument().getConnector();
            java.util.Collection<Integer> ids = cViewer.getSelectedIds();
            progressListener.setSubtask("Reads to GFF");
            progressListener.setMaximum(ids.size());
            progressListener.setProgress(0);

            final IClassificationBlock classificationBlock = connector.getClassificationBlock(cViewer.getClassName());

            final Set<Long> seen = new HashSet<>();

            if (classificationBlock != null) {
                for (int classId : ids) {
                    if (classificationBlock.getSum(classId) > 0) {
                        final int taxonId = (classificationBlock.getName().equals(Classification.Taxonomy) ? classId : 0);
                        try (IReadBlockIterator it = connector.getReadsIterator(cViewer.getClassName(), classId, 0, 10000, true, true)) {
                            while (it.hasNext()) {
                                final IReadBlock readBlock = it.next();
                                final long uid = readBlock.getUId();
                                if (!seen.contains(uid)) {
                                    if (uid != 0)
                                        seen.add(uid);
                                    final String string = createGFFLines(blastMode, readBlock, cNames, classificationToReport, taxonId, excludeIncompatible, excludeDominated);
                                    w.write(string);
                                    countAlignments += Basic.countOccurrences(string, '\n');
                                    if (string.length() > 0)
                                        countReads++;
                                }
                            }
                        }
                    }
                    progressListener.incrementProgress();
                }
            }
        }
        System.err.println("done");

        return new Pair<>(countReads, countAlignments);
    }

    /**
     * export aligned reads in GFF3 format
     *
     * @param viewer
     * @param file
     * @param classificationToReport
     * @param excludeIncompatible
     * @param excludeDominated
     * @param progressListener
     * @return
     * @throws IOException
     * @throws CanceledException
     */
    public static Pair<Long, Long> apply(final LRInspectorViewer viewer, File file, final String classificationToReport, final boolean excludeIncompatible, final boolean excludeDominated, final ProgressListener progressListener) throws IOException, CanceledException {
        long countReads = 0;
        long countAlignments = 0;
        final BlastMode blastMode = viewer.getDir().getDocument().getBlastMode();

        if (viewer.getController() != null) {
            final int taxonId = (viewer.getClassificationName().equals(Classification.Taxonomy) ? viewer.getClassId() : 0);
            System.err.println("Writing file: " + file);
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                progressListener.setSubtask("Reads to GFF");
                progressListener.setMaximum(viewer.getController().getTableView().getSelectionModel().getSelectedItems().size());
                progressListener.setProgress(0);
                w.write(ExportAlignedReads2GFF3Format.getHeader());
                for (TableItem item : viewer.getController().getTableView().getSelectionModel().getSelectedItems()) {
                    final String[] cNames = item.getPane().getClassificationLabelsShowing().toArray(new String[0]);
                    if (cNames.length == 0)
                        System.err.println("Skipping '" + item.getReadName() + "': no classification showing");
                    else if (classificationToReport != null && !Basic.contains(cNames, classificationToReport))
                        System.err.println("Skipping '" + item.getReadName() + "': selected classification not showing");
                    else {
                        final String string = createGFFLines(blastMode, item.getReadName(), item.getReadLength(), cNames, classificationToReport, item.getPane().getIntervals(), taxonId, excludeIncompatible, excludeDominated);
                        w.write(string);
                        countAlignments += Basic.countOccurrences(string, '\n');
                        if (string.length() > 0)
                            countReads++;
                    }
                    progressListener.incrementProgress();
                }
            }
            System.err.println("done");
        }
        return new Pair<>(countReads, countAlignments);
    }

    /**
     * create a GFF line for a read
     *
     * @param blastMode
     * @param readBlock
     * @param cNames
     * @return
     */
    private static String createGFFLines(final BlastMode blastMode, final IReadBlock readBlock, final String[] cNames, final String classificationToReport, final int taxonId, boolean excludeIncompatible, boolean excludeDominated) throws CanceledException {
        final IntervalTree<IMatchBlock> intervals;
        intervals = IntervalTree4Matches.computeIntervalTree(readBlock, null, null);
        return createGFFLines(blastMode, readBlock.getReadName(), readBlock.getReadLength(), cNames, classificationToReport, intervals, taxonId, excludeIncompatible, excludeDominated);
    }

    /**
     * create all GFF entries for a read
     *
     * @param blastMode
     * @param readName
     * @param cNames
     * @param intervals
     * @return GFF line
     */
    private static String createGFFLines(final BlastMode blastMode, final String readName, final int readLength, final String[] cNames, final String classificationToReport,
                                         IntervalTree<IMatchBlock> intervals, final int readTaxonId, final boolean excludeIncompatible, final boolean excludeDominated) {

        if (excludeDominated) {
            intervals = IntervalTree4Matches.extractDominatingIntervals(intervals, cNames, classificationToReport);
        }

        final Classification[] classifications = new Classification[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            classifications[i] = ClassificationManager.get(cNames[i], true);
        }

        final StringBuilder buf = new StringBuilder();

        buf.append(String.format("##sequence-region %s %d %d\n", readName, 1, readLength));
        if (readTaxonId > 0) {
            buf.append(String.format("##species http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=%d\n", readTaxonId));
            buf.append(String.format("# Taxon for %s: id=%d name=%s\n", readName, readTaxonId, TaxonomyData.getName2IdMap().get(readTaxonId)));
        }

        final boolean reportAllClassifications = (classificationToReport.equalsIgnoreCase("all"));
        final int classificationToReportIndex = Basic.getIndex(classificationToReport, cNames);

        for (Interval<IMatchBlock> interval : intervals) {
            final IMatchBlock matchBlock = interval.getData();

            final String matchBlockFirstWord = matchBlock.getTextFirstWord();
            if (matchBlockFirstWord == null)
                continue;

            // if we not reporting all alignments, then determine whether this one has an id for the classification to report, otherwise skip it
            final int idToReport;
            if (!reportAllClassifications) {
                idToReport = matchBlock.getId(classificationToReport);
                if (idToReport <= 0)
                    continue; // don't report this alignment
            } else
                idToReport = 0;

            final String taxRel;
            if (readTaxonId > 0) {
                final int matchTaxonId = matchBlock.getTaxonId();
                if (matchTaxonId == readTaxonId)
                    taxRel = "equal";
                else if (TaxonomyData.isAncestor(readTaxonId, matchTaxonId))
                    taxRel = "below";
                else if (TaxonomyData.isAncestor(matchTaxonId, readTaxonId))
                    taxRel = "above";
                else {
                    taxRel = "incompatible";
                    if (excludeIncompatible)
                        continue; // don't report this one
                }
            } else
                taxRel = null;

            if (!reportAllClassifications && !(classificationToReportIndex >= 0))
                continue; // something wrong here...

            // seqname source feature start end score strand frame attribute
            final int start = interval.getStart();
            final int end = interval.getEnd();
            final float score = matchBlock.getBitScore();
            final String strand = (matchBlock.getAlignedQueryStart() < matchBlock.getAlignedQueryEnd() ? "+" : "-");
            final String frame;
            if (blastMode == BlastMode.BlastX) {
                if (matchBlock.getAlignedQueryStart() < matchBlock.getAlignedQueryEnd())
                    frame = "" + (matchBlock.getAlignedQueryStart() % 3);
                else {
                    frame = "" + ((readLength - matchBlock.getAlignedQueryEnd()) % 3);
                }
            } else {
                frame = ".";
            }

            buf.append(String.format("%s\t%s\t%s\t%d\t%d\t%.0f\t%s\t%s",
                    readName, ProgramProperties.getProgramName(), "CDS", start, end, score, strand, frame));

            try {
                final String acc = Basic.swallowLeadingGreaterSign(matchBlockFirstWord.replaceAll("\\s+", "_"));
                buf.append(String.format("\tId=%s; acc=%s;", acc, acc));

                final StringBuilder nameBuffer = new StringBuilder();

                // if not reporting all and have an id, then use it.
                if (!reportAllClassifications) {
                    if (idToReport > 0 && classificationToReportIndex >= 0) {
                        final String value = classifications[classificationToReportIndex].getName2IdMap().get(idToReport);
                        nameBuffer.append(value != null ? value.replaceAll("\\s+", "_") : getShortName(classificationToReport) + idToReport);
                    } else
                        continue; // something not correct, skip this (shouldn't ever happen...)
                }

                for (int i = 0; i < cNames.length; i++) {
                    final String cName = cNames[i];
                    final String shortName = getShortName(cName);

                    int id = matchBlock.getId(cName);
                    if (id > 0 && classifications[i] != null) {
                        final String value = classifications[i].getName2IdMap().get(id);
                        if (value != null && value.length() > 0) {
                            final String displayValue = Basic.abbreviateDotDotDot(value.replaceAll("\t", "%09").replaceAll(";", "%3B").replaceAll("=", "%3D").replaceAll(" ", "_"), 80);
                            buf.append(String.format(" %s=%s;", shortName, displayValue));
                            if (taxRel != null) {
                                buf.append(" taxRel=").append(taxRel).append(";");
                            }
                        }

                        if (reportAllClassifications) {
                            if (nameBuffer.length() > 0)
                                nameBuffer.append(",_");
                            nameBuffer.append(value != null ? value : shortName + id);
                        }
                    }
                }

                if (nameBuffer.length() > 0)
                    buf.append(" Name=").append(nameBuffer.toString()).append(";");
            } finally {
                buf.append("\n");
            }
        }
        return buf.toString();
    }

    /**
     * get the classification short name
     *
     * @param cName
     * @return short name
     */
    public static String getShortName(String cName) {
        String shortName;
        switch (cName.toLowerCase()) {
            case "taxonomy":
                shortName = "tax";
                break;
            case "interpro2go":
                shortName = "ipr";
                break;
            case "eggnog":
                shortName = "cog";
                break;
            default:
                shortName = cName.toLowerCase();
        }
        return shortName;
    }

    /**
     * get header comment line
     *
     * @return header
     */
    private static String getHeader() {
        return "##gff-version 3.2.1\n";
    }
}
