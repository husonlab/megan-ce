/*
 * GeneItemAccessor.java
 * Copyright (C) 2019 Daniel H. Huson
 * <p>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.genes;

import jloda.swing.util.ArgsOptions;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressPercentage;
import jloda.util.UsageException;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.classification.IdMapper;
import megan.io.InputReader;
import megan.tools.AAdderBuild;
import megan.tools.AAdderRun;

import java.io.*;

/**
 * class used to access gene items
 * Daniel Huson, 6.2018
 */
public class GeneItemAccessor {
    private final int size;
    private final long[] refIndex2FilePos;
    private final IntervalTree<GeneItem>[] refIndex2Intervals;
    private final String[] index2ref;
    private final RandomAccessFile dbRaf;

    private final GeneItemCreator creator;

    private final int syncBits = 1023;
    private final Object[] syncObjects = new Object[syncBits + 1];  // use lots of objects to synchronize on so that threads don't in each others way

    /**
     * construct the gene table from the gene-table index file
     *
     * @param indexFile
     * @param dbFile
     * @throws IOException
     */
    public GeneItemAccessor(File indexFile, File dbFile) throws IOException {
        // create the synchronization objects
        for (int i = 0; i < (syncBits + 1); i++) {
            syncObjects[i] = new Object();
        }

        try (InputReader ins = new InputReader(indexFile); ProgressPercentage progress = new ProgressPercentage("Reading file: " + indexFile)) {
            AAdderRun.readAndVerifyMagicNumber(ins, AAdderBuild.MAGIC_NUMBER_IDX);
            final String creator = ins.readString();
            if (!creator.equals("MALT"))
                throw new IOException("Gene Item index not created by MALT");
            size = ins.readInt();
            progress.setMaximum(size);
            refIndex2FilePos = new long[size];
            index2ref = new String[size];
            for (int i = 0; i < size; i++) {
                index2ref[i] = ins.readString();
                final long pos = ins.readLong();
                refIndex2FilePos[i] = pos;
                progress.incrementProgress();
            }
        }
        refIndex2Intervals = (IntervalTree<GeneItem>[]) new IntervalTree[size];

        try (InputReader dbxIns = new InputReader(dbFile)) {
            AAdderRun.readAndVerifyMagicNumber(dbxIns, AAdderBuild.MAGIC_NUMBER_DBX);
            final String[] cNames = new String[dbxIns.readInt()];
            for (int i = 0; i < cNames.length; i++) {
                cNames[i] = dbxIns.readString();
                System.err.println(cNames[i]);
            }
            creator = new GeneItemCreator(cNames, new IdMapper[0]);
        }
        dbRaf = new RandomAccessFile(dbFile, "r");
    }

    private int warned = 0;

    /**
     * get intervals for a given ref index
     *
     * @param refIndex
     * @return intervals or null
     * @throws IOException
     */
    private IntervalTree<GeneItem> getIntervals(int refIndex) {
        synchronized (syncObjects[refIndex & syncBits]) {
            if (refIndex2Intervals[refIndex] == null && refIndex2FilePos[refIndex] != 0) {
                synchronized (dbRaf) {
                    try {
                        final long pos = refIndex2FilePos[refIndex];
                        dbRaf.seek(pos);
                        int intervalsLength = dbRaf.readInt();
                        if (intervalsLength > 0) {
                            IntervalTree<GeneItem> intervals = new IntervalTree<>();
                            for (int i = 0; i < intervalsLength; i++) {
                                int start = dbRaf.readInt();
                                int end = dbRaf.readInt();
                                GeneItem geneItem = new GeneItem(creator);
                                geneItem.read(dbRaf);
                                intervals.add(start, end, geneItem);
                                //System.err.println(refIndex+"("+start+"-"+end+") -> "+geneItem);
                            }
                            refIndex2Intervals[refIndex] = intervals;
                        }
                    } catch (IOException ex) {
                        if (warned < 10) {
                            Basic.caught(ex);
                            if (++warned == 0) {
                                System.err.println("Suppressing all further such exceptions");
                            }
                        }
                    }
                }

            }
            return refIndex2Intervals[refIndex];
        }
    }

    /**
     * adds annotations to reference header
     *
     * @param referenceHeader
     * @param refIndex
     * @param alignStart
     * @param alignEnd
     * @return annotated reference header
     */
    public String annotateRefString(String referenceHeader, Integer refIndex, int alignStart, int alignEnd) {
        final IntervalTree<GeneItem> tree = getIntervals(refIndex);

        if (tree != null) {
            final Interval<Object> alignInterval = new Interval<>(alignStart, alignEnd, null);
            final Interval<GeneItem> refInterval = tree.getBestInterval(alignInterval, 0.9);

            if (refInterval != null) {
                final GeneItem geneItem = refInterval.getData();

                return Basic.swallowLeadingGreaterSign(Basic.getFirstWord(referenceHeader)) + "|" + geneItem.getAnnotation(refInterval);
            }
        }
        return referenceHeader;
    }

    private String getIndex2ref(int i) {
        return index2ref[i];
    }

    private int size() {
        return size;
    }

    /**
     * dump gene table to standard out
     *
     */
    public static void main() throws IOException, UsageException, CanceledException {
        String[] args = new String[]{"-i", "/Users/huson/data/malt/gff/index/aadd.idx"};

        final ArgsOptions options = new ArgsOptions(args, null, "GeneTableDump", "Dump gene table");
        final String idxFile = options.getOptionMandatory("i", "idxFile", "Input aadd.idx file", "index/aadd.idx");
        final String dbFile = options.getOption("d", "dbFile", "Input aadd.dbx file", Basic.replaceFileSuffix(idxFile, ".dbx"));
        final String outputFile = options.getOption("o", "output", "Output file (or stdout)", "stdout");
        options.done();

        final GeneItemAccessor geneTableAccess = new GeneItemAccessor(new File(idxFile), new File(dbFile));

        try (Writer w = new BufferedWriter(outputFile.equals("stdout") ? new OutputStreamWriter(System.out) : new FileWriter(outputFile))) {
            for (int i = 0; i < geneTableAccess.size(); i++) {
                System.err.println("ref[" + i + "]=" + geneTableAccess.getIndex2ref(i) + ":");
                final IntervalTree<GeneItem> tree = geneTableAccess.getIntervals(i);
                if (tree != null) {
                    System.err.println("Tree[" + idxFile + "]: " + Basic.abbreviateDotDotDot(tree.toString(), 1000));
                }
            }
        }
    }
}
