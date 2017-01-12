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
import jloda.util.ProgressPercentage;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdParser;
import megan.daa.io.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * adds reference annotations to DAA file
 * Daniel Huson, 8.2105
 */
public class DAAReferencesAnnotator {

    /**
     * add reference annotations to a DAA file
     *
     * @throws IOException
     */
    public static void apply(String daaFile, boolean doTaxonomy, String[] fNames0, final ProgressListener progress) throws IOException, CanceledException {
        DAAModifier.removeAllMEGANData(daaFile);

        final DAAHeader header = new DAAHeader(daaFile);
        header.load();
        header.loadReferences(false);

        final String[] cNames;
        {
            final List<String> fNamesList = new LinkedList<>();
            fNamesList.addAll(Arrays.asList(fNames0));
            if (doTaxonomy && !fNamesList.contains(Classification.Taxonomy)) {
                fNamesList.add(Classification.Taxonomy);
            } else if (!doTaxonomy && fNamesList.contains(Classification.Taxonomy))
                fNamesList.remove(Classification.Taxonomy);
            cNames = fNamesList.toArray(new String[fNamesList.size()]);
        }

        final int[][] cName2ref2class = new int[cNames.length][header.getNumberOfReferences()];

        final int numberOfThreads = 8;
        final ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        progress.setSubtask("Annotating references");
        progress.setMaximum(header.getNumberOfReferences() / numberOfThreads + cNames.length);
        progress.setProgress(0);

        // determine the names for references:
        for (int t = 0; t < numberOfThreads; t++) {
            final int task = t;
            service.submit(new Runnable() {
                public void run() {
                    try {
                        final IdParser[] idParsers = new IdParser[cNames.length];

                        for (int i = 0; i < cNames.length; i++) {
                            idParsers[i] = ClassificationManager.get(cNames[i], true).getIdMapper().createIdParser();
                        }

                        for (int r = task; r < header.getNumberOfReferences(); r += numberOfThreads) {
                            String ref = Basic.toString(header.getReference(r, null));
                            for (int i = 0; i < idParsers.length; i++) {
                                try {
                                    cName2ref2class[i][r] = idParsers[i].getIdFromHeaderLine(ref);
                                } catch (IOException e) {
                                    Basic.caught(e);
                                }
                            }
                            if (task == 0)
                                progress.incrementProgress();
                        }
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Basic.caught(e);
        }

        // get all into bytes:
        final byte[][] cName2Bytes = new byte[cNames.length][];
        final int[] cName2Size = new int[cNames.length];

        final CountDownLatch countDownLatch2 = new CountDownLatch(cNames.length);
        for (int t = 0; t < cNames.length; t++) {
            final int task = t;
            service.submit(new Runnable() {
                public void run() {
                    try {
                        final ByteOutputStream outs = new ByteOutputStream();
                        final OutputWriterLittleEndian w = new OutputWriterLittleEndian(outs);
                        w.writeNullTerminatedString(cNames[task].getBytes());
                        final int[] ref2class = cName2ref2class[task];
                        for (int ref2clas : ref2class)
                            w.writeInt(ref2clas);
                        cName2Bytes[task] = outs.getBytes();
                        cName2Size[task] = outs.size();
                        progress.incrementProgress();
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    } finally {
                        countDownLatch2.countDown();
                    }
                }
            });
        }
        try {
            countDownLatch2.await();
        } catch (InterruptedException e) {
            Basic.caught(e);
        }
        service.shutdownNow();

        DAAModifier.appendBlocks(header, BlockType.megan_ref_annotations, cName2Bytes, cName2Size);
        if (progress instanceof ProgressPercentage) {
            ((ProgressPercentage) progress).reportTaskCompleted();
        }
    }
}
