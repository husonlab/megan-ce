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
package megan.dialogs.export;

import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgressListener;
import megan.algorithms.ActiveMatches;
import megan.algorithms.TaxonPathAssignment;
import megan.classification.Classification;
import megan.classification.IdMapper;
import megan.core.Director;
import megan.core.Document;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * export a mapping of reads to weighted taxon paths (similar to RDP classifier)
 * Daniel Huson, 3.2012
 */
public class CSVExportReads2WeightedTaxonPaths {

    /**
     * export readid to weighted taxon path mapping
     *
     * @param dir
     * @param file
     * @param progressListener
     * @return lines written
     */
    public static int exportReadName2WeightedTaxonPath(Director dir, File file, ProgressListener progressListener) throws IOException {
        int totalOut = 0;
        try {
            final Document doc = dir.getDocument();
            final IConnector connector = doc.getConnector();

            try (IReadBlockIterator it = connector.getAllReadsIterator(doc.getMinScore(), doc.getMaxExpected(), true, true)) {
                progressListener.setMaximum(it.getMaximumProgress());
                progressListener.setProgress(0);

                try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                    while (it.hasNext()) {
                        IReadBlock readBlock = it.next();

                        final BitSet activeMatchesForTaxa = new BitSet();

                        boolean hasLowComplexity = readBlock.getComplexity() > 0 && readBlock.getComplexity() + 0.01 < doc.getMinComplexity();

                        final List<Pair<Integer, Float>> path;
                        if (hasLowComplexity) {
                            path = new LinkedList<>();
                            Pair<Integer, Float> pair = new Pair<>(IdMapper.LOW_COMPLEXITY_ID, 100f);
                            path.add(pair);
                        } else {
                            ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, Classification.Taxonomy, activeMatchesForTaxa);
                            path = TaxonPathAssignment.computeTaxPath(readBlock, activeMatchesForTaxa);
                        }

                        w.write(readBlock.getReadName() + "; ;");
                        for (Pair<Integer, Float> pair : path) {
                            String taxonName = TaxonomyData.getName2IdMap().get(pair.getFirst());
                            if (TaxonomyData.getTaxonomicRank(pair.getFirst()) != 0) {
                                w.write(" " + taxonName + "; " + (int) (float) pair.getSecond() + ";");
                            }
                        }
                        w.write("\n");
                        totalOut++;
                        progressListener.setProgress(it.getProgress());
                    }
                }
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalOut;
    }
}
