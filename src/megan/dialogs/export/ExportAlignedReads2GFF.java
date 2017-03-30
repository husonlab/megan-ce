/*
 *  Copyright (C) 2015 Daniel H. Huson
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

import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.data.*;
import megan.parsers.blast.BlastMode;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;
import megan.viewer.ClassificationViewer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * export selected reads and their annotations in GFF format
 * Created by huson on March 2017
 */
public class ExportAlignedReads2GFF {
    /**
     * export read names to GFF
     *
     * @param cNames           classifications to report
     * @param file
     * @param progressListener
     * @return lines written
     */
    public static int apply(ClassificationViewer cViewer, final String[] cNames, File file, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final BlastMode blastMode = cViewer.getDir().getDocument().getBlastMode();

            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);
            final boolean taxonomyClassification = classification.getName().equals(Classification.Taxonomy);

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write(getHeader());
                IConnector connector = cViewer.getDocument().getConnector();
                java.util.Collection<Integer> ids = cViewer.getSelectedIds();
                progressListener.setSubtask("Reads to GFF");
                progressListener.setMaximum(ids.size());
                progressListener.setProgress(0);

                final IClassificationBlock classificationBlock = connector.getClassificationBlock(cViewer.getClassName());

                if (classificationBlock != null) {
                    for (int classId : ids) {
                        final Set<String> seen = new HashSet<>();
                        final Set<Integer> allBelow;
                        Node v = classification.getFullTree().getANode(classId);
                        if (v.getOutDegree() > 0)
                            allBelow = classification.getFullTree().getAllDescendants(classId);
                        else {
                            allBelow = new HashSet<>();
                            allBelow.add(classId);
                        }

                        for (final int id : allBelow) {
                            if (classificationBlock.getSum(id) > 0) {
                                try (IReadBlockIterator it = connector.getReadsIterator(cViewer.getClassName(), id, 0, 10000, true, true)) {
                                    while (it.hasNext()) {
                                        final IReadBlock readBlock = it.next();
                                        if (!seen.contains(readBlock.getReadName())) {
                                            seen.add(readBlock.getReadName());
                                            w.write(createGFFLine(blastMode, readBlock, cNames, taxonomyClassification ? id : 0));
                                            totalLines++;
                                        }
                                    }
                                }
                                progressListener.checkForCancel();
                            }
                        }
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
     * create a GFF line for a read
     *
     * @param blastMode
     * @param readBlock
     * @param cNames
     * @param readTaxonId
     * @return
     */
    public static String createGFFLine(BlastMode blastMode, IReadBlock readBlock, String[] cNames, int readTaxonId) {
        final IntervalTree<IMatchBlock> intervals = IntervalTree4Matches.computeIntervalTree(readBlock, cNames, readTaxonId, null);
        return createGFFLine(blastMode, readBlock.getReadName(), readBlock.getReadLength(), cNames, intervals);
    }

    /**
     * get header comment line
     *
     * @return header
     */
    public static String getHeader() {
        return "# seqname source feature start end score strand frame attributes\n";
    }

    /**
     * create a GFF entry for a read
     *
     * @param blastMode
     * @param readName
     * @param cNames
     * @param intervals
     * @return GFF line
     */
    public static String createGFFLine(BlastMode blastMode, String readName, int readLength, String[] cNames, IntervalTree<IMatchBlock> intervals) {
        final Classification[] classifications = new Classification[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            classifications[i] = ClassificationManager.get(cNames[i], true);
        }

        final StringBuilder buf = new StringBuilder();

        for (Interval<IMatchBlock> interval : intervals) {
            final IMatchBlock matchBlock = interval.getData();

            // seqname source feature start end score strand frame attribute
            final int start = interval.getStart();
            final int end = interval.getEnd();
            final float score = matchBlock.getBitScore();
            final String strand = (matchBlock.getAlignedQueryStart() < matchBlock.getAlignedQueryEnd() ? "+" : "-");
            final String frame;
            if (blastMode == BlastMode.BlastX) {
                if (matchBlock.getAlignedQueryStart() < matchBlock.getAlignedQueryEnd())
                    frame = "" + (matchBlock.getAlignedQueryStart() % 3);
                else
                    frame = "" + ((readLength - matchBlock.getAlignedQueryEnd()) % 3);


            } else
                frame = ".";

            buf.append(String.format("%s\tMEGAN\tgene\t%d\t%d\t%.0f\t%s\t%s", readName, start, end, score, strand, frame));

            boolean firstAttribute = true;

            final String matchTextFirstWord = matchBlock.getTextFirstWord();
            if (matchTextFirstWord != null) {
                buf.append(String.format("\tacc=%s;", Basic.swallowLeadingGreaterSign(matchTextFirstWord.replaceAll("\\s+", " "))));
                firstAttribute = false;
            }

            for (int i = 0; i < cNames.length; i++) {
                String cName = cNames[i];
                int id = matchBlock.getId(cName);
                if (id > 0 && classifications[i] != null) {
                    String value = classifications[i].getName2IdMap().get(id);
                    if (value != null && value.length() > 0) {
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
                        if (firstAttribute) {
                            buf.append("\t");
                            firstAttribute = false;
                        } else
                            buf.append(" ");
                        buf.append(String.format("%s=%s;", shortName, value.replaceAll(";", "_")));
                    }
                }
            }
            buf.append("\n");
        }
        return buf.toString();
    }
}
