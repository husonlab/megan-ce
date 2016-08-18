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
package megan.dialogs.export;

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.data.*;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * export taxonomy related stuff in CVS format
 * Daniel Huson, 4.2010
 */
public class CSVExportTaxonomy {
    /**
     * export taxon name to counts mapping
     *
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportTaxon2TotalLength(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final MainViewer viewer = dir.getMainViewer();

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = viewer.getDir().getDocument().getConnector();
                IClassificationBlock classificationBlock = connector.getClassificationBlock(ClassificationType.Taxonomy.toString());
                java.util.Collection<Integer> taxonIds = viewer.getSelectedIds();

                progressListener.setSubtask("Taxa to total length");
                progressListener.setMaximum(taxonIds.size());
                progressListener.setProgress(0);

                for (int taxonId : taxonIds) {
                    Set<Integer> allBelow;
                    Node v = viewer.getTaxId2Node(taxonId);
                    if (v.getOutDegree() == 0)
                        allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                    else {
                        allBelow = new HashSet<>();
                        allBelow.add(taxonId);
                    }
                    final String name = getTaxonLabelSource(dir, format, taxonId);
                    if (name != null) {
                        w.write(name);
                        long length = 0L;
                        for (int id : allBelow) {
                            if (classificationBlock.getSum(id) > 0) {
                                try (IReadBlockIterator it = connector.getReadsIterator(viewer.getClassName(), id, 0, 10000, true, false)) {
                                    while (it.hasNext()) {
                                        length += it.next().getReadLength();
                                    }
                                }
                                totalLines++;
                                progressListener.checkForCancel();
                            }

                        }
                        w.write(separator + "" + length + "\n");
                    }
                    progressListener.incrementProgress();
                }
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export taxon name to counts mapping
     *
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportTaxon2Counts(String format, Director dir, File file, char separator, boolean reportSummarized, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final MainViewer viewer = dir.getMainViewer();

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                final List<String> names = viewer.getDir().getDocument().getSampleNames();
                if (names.size() > 1) {
                    w.write("#Datasets");
                    for (String name : names) {
                        if (separator == ',')
                            name = name.replaceAll(",", "_");
                        w.write(separator + name);
                    }
                    w.write("\n");
                }

                final NodeSet selected = viewer.getSelectedNodes();

                progressListener.setSubtask("Taxa to counts");
                progressListener.setMaximum(selected.size());
                progressListener.setProgress(0);
                for (Node v = selected.getFirstElement(); v != null; v = selected.getNextElement(v)) {
                    Integer taxonId = (Integer) v.getInfo();
                    if (taxonId != null) {
                        final NodeData data = viewer.getNodeData(v);
                        final int[] counts = (reportSummarized && v.getOutDegree() > 0 ? data.getSummarized() : data.getAssigned());
                        final String name = getTaxonLabelSource(dir, format, taxonId);
                        if (name != null) {
                            if (counts.length == names.size()) {
                                w.write(name);
                                for (int num : counts)
                                    w.write(separator + "" + num);
                                w.write("\n");
                                totalLines++;
                            } else
                                System.err.println("Skipped " + name + ", number of values: " + counts.length);
                        }
                    }
                    progressListener.incrementProgress();
                }
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export readid to taxon names mapping
     *
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportReadName2Taxon(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;

        try {
            final MainViewer viewer = dir.getMainViewer();

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = viewer.getDir().getDocument().getConnector();
                IClassificationBlock classificationBlock = connector.getClassificationBlock(ClassificationType.Taxonomy.toString());
                java.util.Collection<Integer> taxonIds = viewer.getSelectedIds();

                progressListener.setSubtask("Read names to taxa");
                progressListener.setMaximum(taxonIds.size());
                progressListener.setProgress(0);

                for (int taxonId : taxonIds) {
                    Set<String> seen = new HashSet<>();
                    Set<Integer> allBelow;
                    Node v = viewer.getTaxId2Node(taxonId);
                    if (v.getOutDegree() == 0)
                        allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                    else {
                        allBelow = new HashSet<>();
                        allBelow.add(taxonId);
                    }
                    for (int id : allBelow) {
                        if (classificationBlock.getSum(id) > 0) {
                            try (IReadBlockIterator it = connector.getReadsIterator(viewer.getClassName(), id, 0, 10000, true, false)) {
                                while (it.hasNext()) {
                                    String readId = it.next().getReadName();
                                    if (!seen.contains(readId)) {
                                        seen.add(readId);
                                        w.write(readId + separator + getTaxonLabelTarget(dir, format, taxonId) + "\n");
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
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export readid to matches mapping
     *
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportReadName2Matches(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final MainViewer viewer = dir.getMainViewer();

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = viewer.getDir().getDocument().getConnector();
                IClassificationBlock classificationBlock = connector.getClassificationBlock(viewer.getClassName());

                progressListener.setSubtask("Read names to matches");

                java.util.Collection<Integer> taxonIds = viewer.getSelectedIds();
                if (taxonIds.size() > 0) {
                    progressListener.setMaximum(taxonIds.size());
                    progressListener.setProgress(0);

                    for (int taxonId : taxonIds) {
                        Set<String> seen = new HashSet<>();
                        Set<Integer> allBelow;
                        Node v = viewer.getTaxId2Node(taxonId);
                        if (v.getOutDegree() == 0)
                            allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                        else {
                            allBelow = new HashSet<>();
                            allBelow.add(taxonId);
                        }
                        for (int id : allBelow) {
                            if (classificationBlock.getSum(id) > 0) {
                                try (IReadBlockIterator it = connector.getReadsIterator(viewer.getClassName(), id, 0, 10000, true, true)) {
                                    while (it.hasNext()) {
                                        IReadBlock readBlock = it.next();
                                        String readId = readBlock.getReadName();
                                        if (!seen.contains(readId)) {
                                            seen.add(readId);
                                            writeMatches(separator, readId, readBlock, w);
                                            totalLines++;
                                        }
                                    }
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

                    try (IReadBlockIterator it = connector.getAllReadsIterator(0, 10000, true, true)) {
                        while (it.hasNext()) {
                            IReadBlock readBlock = it.next();
                            String readId = readBlock.getReadName();
                            writeMatches(separator, readId, readBlock, w);
                            totalLines++;
                            progressListener.incrementProgress();
                        }
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
     * @param separator
     * @param readName
     * @param readBlock
     * @param w
     * @return number of matches in output
     * @throws IOException
     */
    private static int writeMatches(char separator, String readName, IReadBlock readBlock, Writer w) throws IOException {

        int countMatches = 0;
        if (readBlock.getNumberOfAvailableMatchBlocks() == 0)
            w.write(String.format("%s%c\n", readName, separator));
        else {
            w.write(readName);
            for (IMatchBlock matchBlock : readBlock.getMatchBlocks()) {
                w.write(String.format("%c%d%c%.2f", separator, matchBlock.getTaxonId(), separator, matchBlock.getBitScore()));
                countMatches++;
            }
            w.write("\n");

        }
        return countMatches;
    }

    /**
     * export taxon name to read-ids mapping
     *
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportTaxon2ReadNames(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final MainViewer viewer = dir.getMainViewer();

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = viewer.getDir().getDocument().getConnector();
                IClassificationBlock classificationBlock = connector.getClassificationBlock(viewer.getClassName());
                java.util.Collection<Integer> taxonIds = viewer.getSelectedIds();

                progressListener.setSubtask("Taxa to read names");
                progressListener.setMaximum(taxonIds.size());
                progressListener.setProgress(0);

                for (int taxonId : taxonIds) {
                    Set<Integer> allBelow;
                    Node v = viewer.getTaxId2Node(taxonId);
                    if (v.getOutDegree() == 0)
                        allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                    else {
                        allBelow = new HashSet<>();
                        allBelow.add(taxonId);
                    }
                    final String name = getTaxonLabelSource(dir, format, taxonId);
                    if (name != null) {
                        w.write(name);
                        for (int id : allBelow) {
                            if (classificationBlock.getSum(id) > 0) {
                                try (IReadBlockIterator it = connector.getReadsIterator(viewer.getClassName(), id, 0, 10000, true, false)) {
                                    while (it.hasNext()) {
                                        String readId = it.next().getReadName();
                                        w.write(separator + "" + readId);
                                    }
                                }
                                w.write("\n");
                                totalLines++;
                                progressListener.checkForCancel();
                            }
                        }
                    }
                    progressListener.incrementProgress();
                }
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export taxon-path to number of reads assigned
     *
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     * @throws IOException
     */
    public static int exportTaxon2ReadIds(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final MainViewer viewer = dir.getMainViewer();

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = viewer.getDir().getDocument().getConnector();
                IClassificationBlock classificationBlock = connector.getClassificationBlock(viewer.getClassName());
                java.util.Collection<Integer> taxonIds = viewer.getSelectedIds();

                progressListener.setSubtask("Taxa to read Ids");
                progressListener.setMaximum(taxonIds.size());
                progressListener.setProgress(0);

                for (int taxonId : taxonIds) {
                    Set<Integer> allBelow;
                    Node v = viewer.getTaxId2Node(taxonId);
                    if (v.getOutDegree() == 0)
                        allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                    else {
                        allBelow = new HashSet<>();
                        allBelow.add(taxonId);
                    }
                    final String name = getTaxonLabelSource(dir, format, taxonId);
                    if (name != null) {
                        w.write(name);
                        for (int id : allBelow) {
                            if (classificationBlock.getSum(id) > 0) {
                                try (IReadBlockIterator it = connector.getReadsIterator(viewer.getClassName(), id, 0, 10000, true, false)) {
                                    while (it.hasNext()) {
                                        String readId = it.next().getReadName();
                                        w.write(separator + "" + readId);
                                    }
                                }
                                w.write("\n");
                                totalLines++;
                                progressListener.checkForCancel();
                            }
                        }
                    }
                    progressListener.incrementProgress();
                }
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * gets the full path to the named taxon
     *
     * @param dir
     * @param taxonId
     * @return
     */
    public static String getPath(Director dir, int taxonId) {
        List<String> path = new LinkedList<>();
        Node v = dir.getMainViewer().getTaxId2Node(taxonId);
        while (v != null && v.getInfo() != null) {
            taxonId = (Integer) v.getInfo();
            String name = TaxonomyData.getName2IdMap().get(taxonId);
            path.add(name);
            if (v.getInDegree() > 0)
                v = v.getFirstInEdge().getSource();
            else
                v = null;
        }
        StringBuilder buf = new StringBuilder();
        String[] array = path.toArray(new String[path.size()]);
        for (int i = array.length - 1; i >= 0; i--) {
            buf.append(array[i]).append(";");
        }
        return buf.toString();
    }

    /**
     * determines which type of label is desired
     *
     * @param format
     * @return label type
     */
    public static String getTaxonLabelSource(Director dir, String format, int taxonId) {
        if (format.startsWith("taxonName"))
            return Basic.getInQuotes(TaxonomyData.getName2IdMap().get(taxonId));
        else if (format.startsWith("taxonPath"))
            return Basic.getInQuotes(getPath(dir, taxonId));
        else if (format.startsWith("taxonRank")) {
            final String rankName = TaxonomicLevels.getName(TaxonomyData.getName2IdMap().getRank(taxonId));
            if (rankName != null)
                return rankName + ":" + Basic.getInQuotes(TaxonomyData.getName2IdMap().get(taxonId));
            else
                return "No_rank:" + Basic.getInQuotes(TaxonomyData.getName2IdMap().get(taxonId));
        } else
            return "" + taxonId;
    }

    /**
     * determines which type of label is desired
     *
     * @param format
     * @return label type
     */
    public static String getTaxonLabelTarget(Director dir, String format, int taxonId) {
        if (format.endsWith("taxonName"))
            return Basic.getInQuotes(TaxonomyData.getName2IdMap().get(taxonId));
        else if (format.endsWith("taxonPath"))
            return Basic.getInQuotes(getPath(dir, taxonId));
        else
            return "" + taxonId;
    }
}
