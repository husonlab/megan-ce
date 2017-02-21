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

package megan.daa;

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.classification.Classification;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.core.SyncArchiveAndDataTable;
import megan.daa.connector.DAAConnector;
import megan.daa.io.DAAHeader;
import megan.daa.io.DAAParser;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * meganizes a DAA file
 * Daniel Huson, 3.2016
 */
public class Meganize {
    /**
     * meganizes a DAA file
     *
     * @param daaFile
     * @param metaDataFile
     * @param cNames
     * @param minScore
     * @param maxExpected
     * @param topPercent
     * @param minSupportPercent
     * @param minSupport
     * @param pairedReads
     * @param pairedReadsSuffixLength
     * @param lcaAlgorithm
     * @throws IOException
     * @throws CanceledException
     */
    public static void apply(final ProgressListener progress, final String daaFile, final String metaDataFile, final String[] cNames, float minScore, float maxExpected, float minPercentIdentity, float topPercent, float minSupportPercent,
                             int minSupport, boolean pairedReads, int pairedReadsSuffixLength, Document.LCAAlgorithm lcaAlgorithm, float weightedLCAPercent, boolean longReads) throws IOException, CanceledException {

        progress.setTasks("Meganizing", "init");
        DAAReferencesAnnotator.apply(daaFile, true, cNames, progress);

        final Document doc = new Document();
        doc.setOpenDAAFileOnlyIfMeganized(false);
        doc.getMeganFile().setFileFromExistingFile(daaFile, false);
        doc.getActiveViewers().add(Classification.Taxonomy);
        doc.getActiveViewers().addAll(Arrays.asList(cNames));
        doc.setMinScore(minScore);
        doc.setMaxExpected(maxExpected);
        doc.setMinPercentIdentity(minPercentIdentity);
        doc.setTopPercent(topPercent);
        doc.setMinSupportPercent(minSupportPercent);
        doc.setMinSupport(minSupport);
        doc.setPairedReads(pairedReads);
        doc.setPairedReadSuffixLength(pairedReadsSuffixLength);
        doc.setBlastMode(DAAParser.getBlastMode(daaFile));
        doc.setLcaAlgorithm(lcaAlgorithm);
        doc.setWeightedLCAPercent(weightedLCAPercent);
        doc.setLongReads(longReads);

        doc.setProgressListener(progress);

        doc.processReadHits();

        // update and then save auxiliary data:
        final String sampleName = Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(daaFile), "");
        SyncArchiveAndDataTable.syncRecomputedArchive2Summary(sampleName, "LCA", doc.getBlastMode(), doc.getParameterString(), doc.getConnector(), doc.getDataTable(), 0);
        doc.saveAuxiliaryData();

        if (metaDataFile.length() > 0) {
            final DAAConnector connector = (DAAConnector) doc.getConnector();
            try {
                System.err.println("Saving metadata:");
                SampleAttributeTable sampleAttributeTable = new SampleAttributeTable();
                sampleAttributeTable.read(new FileReader(metaDataFile), Collections.singletonList(Basic.getFileBaseName(Basic.getFileNameWithoutPath(daaFile))), false);
                Map<String, byte[]> label2data = new HashMap<>();
                label2data.put(SampleAttributeTable.SAMPLE_ATTRIBUTES, sampleAttributeTable.getBytes());
                connector.putAuxiliaryData(label2data);
                System.err.println("done");
            } catch (Exception ex) {
                Basic.caught(ex);
            }
        }

        // set meganized flag:
        final DAAHeader header = new DAAHeader(daaFile);
        header.load();
        header.setReserved3(DAAHeader.MEGAN_VERSION);
        header.save();
    }
}
