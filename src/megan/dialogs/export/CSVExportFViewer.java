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

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.data.*;
import megan.viewer.ViewerBase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * methods for exporting FViewer data in csv format
 * Daniel Huson, 4.2010
 */
public class CSVExportFViewer {
    /**
     * export name to counts
     *
     * @param cViewer
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportName2Counts(String format, ViewerBase cViewer, File file, char separator, boolean reportSummarized, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);
            final String shortName = (cViewer.getClassName().toLowerCase().equals("taxonomy") ? "Taxon" : cViewer.getClassName());

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                final List<String> names = cViewer.getDocument().getSampleNames();
                if (names.size() > 1) {
                    w.write("#Datasets");
                    for (String name : names) {
                        if (name == null)
                            System.err.println("Internal error, sample name is null");
                        else {
                            if (separator == ',')
                                name = name.replaceAll(",", "_");
                        }
                        w.write(separator + " " + name);
                    }
                    w.write("\n");
                }

                final NodeSet selected = cViewer.getSelectedNodes();
                progressListener.setSubtask(shortName + " to counts");
                progressListener.setMaximum(selected.size());
                progressListener.setProgress(0);
                Set<Integer> seen = new HashSet<>();
                for (Node v = selected.getFirstElement(); v != null; v = selected.getNextElement(v)) {
                    final Integer id = (Integer) v.getInfo();
                    if (id != null && !seen.contains(id)) {
                        seen.add(id);
                        final NodeData data = cViewer.getNodeData(v);
                        final float[] counts = (reportSummarized || v.getOutDegree() == 0 ? data.getSummarized() : data.getAssigned());
                        final String name = getLabelSource(shortName, classification, format, v);
                        if (counts.length == names.size()) {
                            w.write(name);
                            for (float a : counts)
                                w.write(separator + " " + a);
                            w.write("\n");
                            totalLines++;
                        } else
                            System.err.println("Skipped " + name + ", number of values: " + counts.length);
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
     * export name to percentages
     *
     * @param cViewer
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportName2Percent(String format, ViewerBase cViewer, File file, char separator, boolean reportSummarized, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);
            final String shortName = (cViewer.getClassName().toLowerCase().equals("taxonomy") ? "Taxon" : cViewer.getClassName());

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                final List<String> names = cViewer.getDocument().getSampleNames();
                if (names.size() > 1) {
                    w.write("#Datasets");
                    for (String name : names) {
                        if (separator == ',')
                            name = name.replaceAll(",", "_");
                        w.write(separator + " " + name);
                    }
                    w.write("\n");
                }

                final NodeSet selected = cViewer.getSelectedNodes();
                progressListener.setSubtask(shortName + " to counts");
                progressListener.setMaximum(2 * selected.size());
                progressListener.setProgress(0);
                int[] total = new int[cViewer.getDocument().getSampleNames().size()];
                {
                    final Set<Integer> seen = new HashSet<>();
                    for (Node v = selected.getFirstElement(); v != null; v = selected.getNextElement(v)) {
                        final Integer id = (Integer) v.getInfo();
                        if (id != null && !seen.contains(id)) {
                            seen.add(id);
                            final NodeData data = cViewer.getNodeData(v);
                            final float[] values = (reportSummarized || v.getOutDegree() == 0 ? data.getSummarized() : data.getAssigned());
                            for (int i = 0; i < values.length; i++) {
                                total[i] += values[i];
                            }
                        }
                        progressListener.incrementProgress();
                    }
                }

                {
                    final Set<Integer> seen = new HashSet<>();
                    for (Node v = selected.getFirstElement(); v != null; v = selected.getNextElement(v)) {
                        final Integer id = (Integer) v.getInfo();
                        if (id != null && !seen.contains(id)) {
                            seen.add(id);
                            final NodeData data = cViewer.getNodeData(v);
                            final float[] counts = (reportSummarized || v.getOutDegree() == 0 ? data.getSummarized() : data.getAssigned());
                            final String name = getLabelSource(shortName, classification, format, v);
                            if (counts.length == names.size()) {
                                w.write(name);
                                for (int i = 0; i < counts.length; i++) {
                                    double value = (total[i] == 0 ? 0 : (100.0 * counts[i]) / (double) total[i]);
                                    w.write(String.format("%c%f", separator, (float) value));
                                }
                                w.write("\n");
                                totalLines++;
                            } else
                                System.err.println("Skipped " + name + ", number of values: " + counts.length);
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
     * export names
     *
     * @param cViewer
     * @param file
     * @param progressListener
     * @return lines written
     */
    public static int exportNames(String format, ViewerBase cViewer, File file, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);


            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                final NodeSet selected = cViewer.getSelectedNodes();
                progressListener.setSubtask(format);
                progressListener.setMaximum(selected.size());
                progressListener.setProgress(0);

                final boolean names = format.contains("Name");

                for (Node v : selected) {
                    if (names)
                        w.write(classification.getName2IdMap().get((Integer) v.getInfo()) + "\n");
                    else
                        w.write("" + v.getInfo() + "\n");
                    progressListener.incrementProgress();
                    totalLines++;

                }
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * export name to read length mapping
     *
     * @param cViewer
     * @param file
     * @param separator
     * @param progressListener @return lines written
     */
    public static int exportName2TotalLength(String format, ViewerBase cViewer, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);
            final String shortName = (cViewer.getClassName().toLowerCase().equals("taxonomy") ? "Taxon" : cViewer.getClassName());

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = cViewer.getDocument().getConnector();
                java.util.Collection<Integer> classIds = cViewer.getSelectedIds();
                progressListener.setSubtask(shortName + " to read names");
                progressListener.setMaximum(classIds.size());
                progressListener.setProgress(0);

                final IClassificationBlock classificationBlock = connector.getClassificationBlock(cViewer.getClassName());

                if (classificationBlock != null) {
                    for (int classId : classIds) {
                        final Collection<Integer> allBelow;
                        final Node v = classification.getFullTree().getANode(classId);
                        if (v.getOutDegree() > 0)
                            allBelow = classification.getFullTree().getAllDescendants(classId);
                        else
                            allBelow = Collections.singletonList(classId);

                        boolean hasSome = false;
                        long length = 0L;

                        try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds(cViewer.getClassName(), allBelow, 0, 10000, true, false)) {
                            while (it.hasNext()) {
                                if (!hasSome) {
                                    w.write(getLabelSource(shortName, classification, format, v));
                                    hasSome = true;
                                }
                                length += it.next().getReadLength();
                                progressListener.checkForCancel();
                            }
                        }
                        if (hasSome) {
                            w.write(separator + " " + length + "\n");
                            totalLines++;
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
     * export name to count per KB of reference sequence
     *
     * @param cViewer
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportName2CountPerKB(String format, ViewerBase cViewer, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final int lengthFactor = (cViewer.getDocument().getBlastMode().equals(BlastMode.BlastX) ? 3 : 1);

            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);
            final String shortName = (cViewer.getClassName().toLowerCase().equals("taxonomy") ? "Taxon" : cViewer.getClassName());

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = cViewer.getDocument().getConnector();
                java.util.Collection<Integer> classIds = cViewer.getSelectedIds();
                progressListener.setSubtask(shortName + " to normalized counts");
                progressListener.setMaximum(classIds.size());
                progressListener.setProgress(0);

                final IClassificationBlock classificationBlock = connector.getClassificationBlock(cViewer.getClassName());

                if (classificationBlock != null) {
                    for (int classId : classIds) {
                        final Collection<Integer> allBelow = classification.getFullTree().getAllDescendants(classId);
                        boolean hasSome = false;
                        long length = 0L;
                        long count = 0L;
                        for (int belowId : allBelow) {
                            if (classificationBlock.getSum(belowId) > 0) {
                                if (!hasSome) {
                                    w.write(getLabelSource(shortName, classification, format, classification.getFullTree().getANode(classId)));
                                    hasSome = true;
                                }
                                try (IReadBlockIterator it = connector.getReadsIterator(cViewer.getClassName(), belowId, 0, 10000, true, true)) {
                                    final IReadBlock readBlock = it.next();
                                    for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                                        final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                                        if (matchBlock.getId(cViewer.getClassName()) == belowId) {
                                            length += matchBlock.getRefLength();
                                            count++;
                                            break;
                                        }
                                    }
                                }
                                progressListener.checkForCancel();
                            }
                        }
                        if (hasSome) {
                            w.write(String.format("%c%.3f\n", separator, ((double) (1000 * count) / (double) (lengthFactor * length))));
                            totalLines++;
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
     * export readid to  names mapping
     *
     * @param cViewer
     * @param file
     * @param separator
     * @param progressListener
     * @return lines written
     */
    public static int exportReadName2Name(String format, ViewerBase cViewer, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);
            final String shortName = (cViewer.getClassName().toLowerCase().equals("taxonomy") ? "Taxon" : cViewer.getClassName());

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = cViewer.getDocument().getConnector();
                java.util.Collection<Integer> ids = cViewer.getSelectedIds();
                progressListener.setSubtask("Read names to " + shortName);
                progressListener.setMaximum(ids.size());
                progressListener.setProgress(0);

                final IClassificationBlock classificationBlock = connector.getClassificationBlock(cViewer.getClassName());

                if (classificationBlock != null) {
                    for (int classId : ids) {
                        final Set<Long> seen = new HashSet<>();
                        final Collection<Integer> allBelow;
                        final Node v = classification.getFullTree().getANode(classId);
                        if (v.getOutDegree() > 0)
                            allBelow = classification.getFullTree().getAllDescendants(classId);
                        else
                            allBelow = Collections.singletonList(classId);

                        try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds(cViewer.getClassName(), allBelow, 0, 10000, true, false)) {
                            while (it.hasNext()) {
                                final IReadBlock readBlock = it.next();
                                final long uid = readBlock.getUId();
                                if (!seen.contains(uid)) {
                                    if (uid != 0)
                                        seen.add(uid);
                                    w.write(readBlock.getReadName() + separator + " " + getLabelTarget(classification, format, v) + "\n");
                                    totalLines++;
                                }
                            }
                            progressListener.checkForCancel();
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
     * export name to read-ids mapping
     *
     * @param cViewer
     * @param file
     * @param separator
     * @param progressListener @return lines written
     */
    public static int exportName2ReadNames(String format, ViewerBase cViewer, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);
            final String shortName = (cViewer.getClassName().toLowerCase().equals("taxonomy") ? "Taxon" : cViewer.getClassName());

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                IConnector connector = cViewer.getDocument().getConnector();
                java.util.Collection<Integer> ids = cViewer.getSelectedIds();
                progressListener.setSubtask(shortName + " to read names");
                progressListener.setMaximum(ids.size());
                progressListener.setProgress(0);

                final IClassificationBlock classificationBlock = connector.getClassificationBlock(cViewer.getClassName());

                if (classificationBlock != null) {
                    for (int classId : ids) {
                        final Collection<Integer> allBelow;
                        final Node v = classification.getFullTree().getANode(classId);
                        if (v.getOutDegree() > 0)
                            allBelow = classification.getFullTree().getAllDescendants(classId);
                        else {
                            allBelow = Collections.singletonList(classId);
                        }
                        boolean hasSome = false;

                        try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds(cViewer.getClassName(), allBelow, 0, 10000, true, false)) {
                            while (it.hasNext()) {
                                if (!hasSome) {
                                    w.write(getLabelSource(shortName, classification, format, v));
                                    hasSome = true;
                                }

                                String readId = it.next().getReadName();
                                w.write(separator + " " + readId);
                                progressListener.checkForCancel();
                            }
                        }
                        if (hasSome) {
                            w.write("\n");
                            totalLines++;
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
     * get the desired label
     *
     * @param format
     * @param v
     * @return label
     */
    private static String getLabelSource(String cName, Classification classification, String format, Node v) {
        if (format.startsWith(cName.toLowerCase() + "Name"))
            return Basic.getInCleanQuotes(classification.getName2IdMap().get((Integer) v.getInfo()));
        else if (format.startsWith(cName.toLowerCase() + "Path")) {
            return Basic.getInCleanQuotes(getPath(classification, v));
        } else
            return "" + v.getInfo();
    }

    /**
     * get the desired class label
     *
     * @param format
     * @param v
     * @return class label
     */
    private static String getLabelTarget(Classification classification, String format, Node v) {
        if (format.endsWith("Name"))
            return Basic.getInCleanQuotes(classification.getName2IdMap().get((Integer) v.getInfo()));
        else if (format.endsWith("Path")) {
            return Basic.getInCleanQuotes(getPath(classification, v));
        } else
            return "" + v.getInfo();
    }


    /**
     * get the path label of a node
     *
     * @param v
     * @return path label
     */
    public static String getPath(Classification classification, Node v) {
        final List<String> path = new LinkedList<>();
        while (v != null && v.getInfo() != null) {
            path.add(classification.getName2IdMap().get((Integer) v.getInfo()));
            if (v.getInDegree() > 0)
                v = v.getFirstInEdge().getSource();
            else
                v = null;
        }
        final String[] array = path.toArray(new String[0]);
        final StringBuilder buf = new StringBuilder();
        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] != null)
                buf.append(array[i].replaceAll(";", "_")).append(";");
        }
        return buf.toString();
    }
}
