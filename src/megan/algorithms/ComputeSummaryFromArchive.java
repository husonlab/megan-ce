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
package megan.algorithms;

import jloda.util.*;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Document;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.TaxonomyData;

import java.io.IOException;
import java.util.*;

/**
 * computes data summary from an archive
 * Daniel Huson, 11.2011
 */
public class ComputeSummaryFromArchive {
    /**
     * compute a data summary for an archive
     *
     * @param doc
     * @param applyMinSupportFilter
     * @param progressListener
     * @throws IOException
     * @throws CanceledException
     */
    static public void apply(Document doc, boolean applyMinSupportFilter, ProgressListener progressListener) throws CanceledException, IOException {
        System.err.println("Running LCA algorithm to create summary");

        DataTable table = doc.getDataTable();
        table.clear();
        table.setCreator(ProgramProperties.getProgramName());
        table.setCreationDate((new Date()).toString());
        table.setAlgorithm(ClassificationType.Taxonomy.toString(), "Summary");

        final IConnector connector = doc.getConnector();
        progressListener.setSubtask("Analyzing all matches");
        progressListener.setMaximum(doc.getNumberOfReads());
        progressListener.setProgress(0);


        Map<Integer, Integer[]> class2counts = new HashMap<>();
        Map<Integer, Integer> class2count = new HashMap<>();

        Set<Integer> taxonIds = new HashSet<>();

        IReadBlockIterator it;
        try {
            it = connector.getAllReadsIterator(doc.getMinScore(), doc.getMaxExpected(), false, true);
            progressListener.setMaximum(it.getMaximumProgress());
            while (it.hasNext()) {
                IReadBlock readBlock = it.next();

                taxonIds.clear();

                for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                    IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                    if (matchBlock.getTaxonId() > 0 && matchBlock.getBitScore() >= doc.getMinScore() && matchBlock.getExpected() <= doc.getMaxExpected() && matchBlock.getPercentIdentity() >= doc.getMinPercentIdentity()) {
                        taxonIds.add(matchBlock.getTaxonId());
                    }

                    int taxId = TaxonomyData.getLCA(taxonIds, true);

                    if (taxId != 0) {
                        Integer[] counts = class2counts.get(taxId);
                        if (counts == null) {
                            counts = new Integer[]{0};
                            class2counts.put(taxId, counts);
                        }
                        counts[0]++;
                        if (class2count.get(taxId) == null)
                            class2count.put(taxId, 1);
                        else
                            class2count.put(taxId, class2count.get(taxId) + 1);
                    }
                }
                progressListener.incrementProgress();

            }

            // run the minsupport filter

            if (applyMinSupportFilter && doc.getMinSupport() > 0) {
                MinSupportAlgorithm minSupportFilter = new MinSupportAlgorithm(class2count, doc.getMinSupport(), new ProgressSilent());
                try {
                    Map<Integer, Integer> changes = minSupportFilter.apply();
                    for (Integer oldTaxId : changes.keySet()) {
                        Integer newTaxId = changes.get(oldTaxId);
                        Integer oldCount = class2counts.get(oldTaxId)[0];

                        Integer[] newCounts = class2counts.get(newTaxId);
                        if (newCounts == null || newCounts[0] == null) {
                            newCounts = new Integer[]{oldCount};
                            class2counts.put(newTaxId, newCounts);
                        } else {
                            newCounts[0] += oldCount;
                        }
                        class2counts.remove(oldTaxId);
                    }
                } catch (CanceledException e) {
                }
            }
            table.getClassification2Class2Counts().put(ClassificationType.Taxonomy.toString(), class2counts);
        } catch (IOException e) {
            Basic.caught(e);
        }
    }
}
