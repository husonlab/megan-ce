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
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgressListener;
import megan.classification.Classification;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * exports the matches signatures for all reads assigned to a specific taxonomic node
 * Daniel Huson, 6.2010
 */
public class MatchSignaturesExporter {
    /**
     * export all matches in file
     *
     * @param connector
     * @param fileName
     * @param progressListener
     * @throws java.io.IOException
     * @throws jloda.util.CanceledException
     */
    public static void export(IConnector connector, int taxonId, String rank, float minScore, float maxExpected, float minPercentIdentity, float topPercent, String fileName, ProgressListener progressListener) throws IOException, CanceledException {
        final String name = TaxonomyData.getName2IdMap().get(taxonId);

        final Map<String, Integer> read2rank = new HashMap<>();
        final Set<Integer> allTaxa = new TreeSet<>();

        final List<Pair<String, Set<Integer>>> readsAndTaxa = new LinkedList<>();

        int readRank = 0;
        try (IReadBlockIterator it = connector.getReadsIterator(Classification.Taxonomy, taxonId, minScore, maxExpected, true, true)) {
            progressListener.setTasks("Export", "Computing match signatures for '" + name + "'");
            progressListener.setMaximum(it.getMaximumProgress());
            progressListener.setProgress(0);

            while (it.hasNext()) {
                final IReadBlock readBlock = it.next();
                String readName = readBlock.getReadName();
                read2rank.put(readName, readRank);

                final HashSet<Integer> taxa = new HashSet<>();
                readsAndTaxa.add(new Pair<>(readName, taxa));

                double useMinScore = -1;
                for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                    IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                    if (matchBlock.getBitScore() >= minScore && matchBlock.getExpected() <= maxExpected &&
                            (matchBlock.getPercentIdentity() == 0 || matchBlock.getPercentIdentity() >= minPercentIdentity)) {
                        {
                            if (useMinScore == -1)
                                useMinScore = Math.max(minScore, (1f - topPercent) * matchBlock.getBitScore());
                            if (matchBlock.getBitScore() >= useMinScore) {
                                int taxId = mapToRank(rank, matchBlock.getTaxonId());
                                if (taxId > 0)
                                    taxa.add(taxId);
                            }
                        }
                    }
                }
                allTaxa.addAll(taxa);
                progressListener.setProgress(it.getProgress());
            }

            if (fileName.contains("%t"))
                fileName = fileName.replaceAll("%t", Basic.replaceSpaces(name, '_'));
            if (fileName.contains("%i"))
                fileName = fileName.replaceAll("%i", "" + taxonId);

            progressListener.setTasks("Export", "Writing to file: " + Basic.getFileBaseName(fileName));
            progressListener.setMaximum(readsAndTaxa.size());
            progressListener.setProgress(0);

            try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName))) {
                w.write("# " + rank + "-level patterns for '" + name + "'\n");
                w.write("# Number of reads: " + read2rank.keySet().size() + "\n");
                w.write("# Taxon ids:");
                for (Integer taxon : allTaxa) {
                    w.write(String.format("\t%d", taxon));
                }
                w.write("\n");
                w.write("# Taxon names:");
                for (Integer taxon : allTaxa) {
                    w.write(String.format("\t%s", TaxonomyData.getName2IdMap().get(taxon)));
                }
                w.write("\n");

                // write read to signature:
                Map<String, List<String>> signature2reads = new TreeMap<>();
                for (Pair<String, Set<Integer>> pair : readsAndTaxa) {
                    w.write(pair.get1() + "\t");
                    StringBuilder buf = new StringBuilder();
                    for (Integer taxon : allTaxa) {
                        if (pair.get2().contains(taxon))
                            buf.append("1");
                        else
                            buf.append("0");
                    }
                    String signature = buf.toString();
                    w.write(signature);
                    List<String> reads = signature2reads.computeIfAbsent(signature, k -> new LinkedList<>());
                    reads.add(pair.get1());
                    w.write("\n");
                }

                w.write("# Number of unique signatures: " + signature2reads.keySet().size() + "\n");
                // write signature to reads:
                for (String signature : signature2reads.keySet()) {
                    List<String> reads = signature2reads.get(signature);
                    w.write(String.format("%s\t%d", signature, reads.size()));
                    for (String read : reads) {
                        w.write("\t" + read);
                    }
                    w.write("\n");
                }
            }
        }
        System.err.println("Total reads: " + readsAndTaxa.size());
        System.err.println("Total taxa:  " + allTaxa.size());
    }

    /**
     * makes the given taxon to the ancestor taxon of the given rank
     *
     * @param rank
     * @param taxonId
     * @return ancestor taxon of specified rank or 0
     */
    private static int mapToRank(String rank, int taxonId) {
        int targetLevel = TaxonomicLevels.getId(rank);

        Node v = TaxonomyData.getTree().getANode(taxonId);
        while (v != null) {
            int level = TaxonomyData.getTaxonomicRank(taxonId);
            if (level != 0 && level == targetLevel)
                return taxonId;
            if (v.getInDegree() == 0)
                v = null;
            else {
                v = v.getFirstInEdge().getSource();
                taxonId = (Integer) v.getInfo();
            }
        }
        return 0;
    }
}
