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
package megan.daa;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import jloda.fx.util.ProgramExecutorService;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;
import megan.accessiondb.AccessAccessionMappingDatabase;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdParser;
import megan.daa.io.*;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * adds reference annotations to DAA file
 * Daniel Huson, 8.2105
 */
class DAAReferencesAnnotator {
    /**
     * add reference annotations to a DAA file
     *
     * @throws IOException
     */
    public static void apply(String daaFile, boolean doTaxonomy, Collection<String> cNames0, final ProgressListener progress) throws IOException, CanceledException {
        DAAModifier.removeAllMEGANData(daaFile);

        final DAAHeader header = new DAAHeader(daaFile);
        header.load();
        header.loadReferences(false);

        final String[] cNames;
        {
            final List<String> fNamesList = new LinkedList<>(cNames0);
            if (doTaxonomy && !fNamesList.contains(Classification.Taxonomy)) {
                fNamesList.add(Classification.Taxonomy);
            } else if (!doTaxonomy)
                fNamesList.remove(Classification.Taxonomy);
            cNames = fNamesList.toArray(new String[0]);
        }

        final int[][] cName2ref2class = new int[cNames.length][header.getNumberOfReferences()];

        final ExecutorService service = Executors.newCachedThreadPool();
        ObjectProperty<Exception> exception = new SimpleObjectProperty<>();

        try {
            if (ClassificationManager.canUseMeganMapDBFile()) {
                System.err.println("Annotating DAA file using FAST mode (accession database and first accession per line)");
                progress.setSubtask("Annotating references");

                final int chunkSize = 100000;

                final int numberOfTasks = (int) Math.ceil((double) header.getNumberOfReferences() / chunkSize);

                 final CountDownLatch countDownLatch = new CountDownLatch(numberOfTasks);

                final int numberOfThreads = Math.min(numberOfTasks, ProgramExecutorService.getNumberOfCoresToUse());

                //System.err.println("Number of tasks: "+numberOfTasks);
                //System.err.println("Number of threads: "+numberOfThreads);

                progress.setMaximum(numberOfTasks/numberOfThreads);
                progress.setProgress(0);

                for (int t = 0; t < numberOfThreads; t++) {
                    final int task = t;
                    service.submit(() -> {
                        try (final AccessAccessionMappingDatabase accessAccessionMappingDatabase = new AccessAccessionMappingDatabase(ClassificationManager.getMeganMapDBFile())) {
                            final int[] mapClassificationId2DatabaseRank = accessAccessionMappingDatabase.setupMapClassificationId2DatabaseRank(cNames);

                            final String[] queries = new String[chunkSize];
                            for (int r = task * chunkSize; r < header.getNumberOfReferences(); r += numberOfThreads * chunkSize) {
                                try {
                                    if (exception.get() != null)
                                        return;
                                    for (int i = 0; i < chunkSize; i++) {
                                        final int a = r + i;
                                        if (a < header.getNumberOfReferences()) {
                                            queries[i] = getFirstWord(header.getReference(a, null));
                                        } else
                                            break;
                                    }
                                    final int size = Math.min(chunkSize, header.getNumberOfReferences() - r);
                                    final Map<String, int[]> query2ids = accessAccessionMappingDatabase.getValues(queries, size);
                                    for (int q = 0; q < size; q++) {
                                        final int[] ids = query2ids.get(queries[q]);
                                        if (ids != null) {
                                            for (int c = 0; c < cNames.length; c++) {
                                                final int dbRank = mapClassificationId2DatabaseRank[c];
                                                if (dbRank < ids.length)
                                                    cName2ref2class[c][r + q] = ids[dbRank];
                                            }
                                        }
                                    }
                                }
                                finally {
                                    if(task==0)
                                        progress.incrementProgress();
                                    countDownLatch.countDown();
                                }
                            }
                        } catch (Exception ex) {
                            synchronized (exception) {
                                exception.set(ex);
                            }
                            while (countDownLatch.getCount() > 0)
                                countDownLatch.countDown();
                            service.shutdownNow();
                        }
                    });
                }

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    Basic.caught(e);
                }

            } else {
                System.err.println("Annotating DAA file using EXTENDED mode");

                final int numberOfThreads = Math.max(1, Math.min(header.getNumberOfReferences(), Math.min(ProgramExecutorService.getNumberOfCoresToUse(), Runtime.getRuntime().availableProcessors())));
                final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

                progress.setSubtask("Annotating references");
                progress.setMaximum(header.getNumberOfReferences());
                progress.setProgress(0);

                // determine the names for references:
                for (int t = 0; t < numberOfThreads; t++) {
                    final int task = t;
                    service.submit(() -> {
                        try {
                            final IdParser[] idParsers = new IdParser[cNames.length];

                           // need to use only one database per thread.

                            for (int i = 0; i < cNames.length; i++) {
                                    idParsers[i] = ClassificationManager.get(cNames[i], true).getIdMapper().createIdParser();
                            }

                            for (int r = task; r < header.getNumberOfReferences(); r += numberOfThreads) {
                                final String ref = Basic.toString(header.getReference(r, null));
                                for (int i = 0; i < idParsers.length; i++) {
                                    try {
                                        cName2ref2class[i][r] = idParsers[i].getIdFromHeaderLine(ref);
                                    } catch (IOException e) {
                                        Basic.caught(e);
                                    }
                                }
                                if (task == 0)
                                    progress.setProgress(r);
                            }
                        } catch (Exception ex) {
                            synchronized (exception) {
                                exception.set(ex);
                            }
                            while (countDownLatch.getCount() > 0)
                                countDownLatch.countDown();
                            service.shutdownNow();
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                }

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    Basic.caught(e);
                }
            }
            if (exception.get() != null) {
                if (exception.get() instanceof CanceledException)
                    throw (CanceledException) exception.get();
                else
                    throw new IOException(exception.get());
            }

            // get all into bytes:
            final byte[][] cName2Bytes = new byte[cNames.length][];
            final int[] cName2Size = new int[cNames.length];

            final CountDownLatch countDownLatch2 = new CountDownLatch(cNames.length);
            for (int t = 0; t < cNames.length; t++) {
                final int task = t;
                service.submit(() -> {
                    try {
                        final ByteOutputStream outs = new ByteOutputStream();
                        final OutputWriterLittleEndian w = new OutputWriterLittleEndian(outs);
                        w.writeNullTerminatedString(cNames[task].getBytes());
                        final int[] ref2class = cName2ref2class[task];

                        if (task == 0) {
                            progress.setSubtask("Writing");
                            progress.setMaximum(ref2class.length);
                            progress.setProgress(0);
                        }

                        for (int classId : ref2class) {
                            w.writeInt(classId);
                            if (task == 0)
                                progress.incrementProgress();
                        }

                        cName2Bytes[task] = outs.getBytes();
                        cName2Size[task] = outs.size();
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    } finally {
                        countDownLatch2.countDown();
                    }
                });
            }
            try {
                countDownLatch2.await();
            } catch (InterruptedException e) {
                Basic.caught(e);
            }

            DAAModifier.appendBlocks(header, BlockType.megan_ref_annotations, cName2Bytes, cName2Size);
            if (progress instanceof ProgressPercentage) {
                ((ProgressPercentage) progress).reportTaskCompleted();
            }
        } finally {
            service.shutdownNow();
        }
    }

    private static String getFirstWord(byte[] bytes) {
        int a = 0;
        while (a < bytes.length && (bytes[a] == '>' || Character.isWhitespace(bytes[a]))) {
            a++;
        }
        int b = a;
        while (b < bytes.length && (bytes[b] == '_' || Character.isLetterOrDigit(bytes[b]))) {
            b++;
        }
        return new String(bytes, a, b - a);
    }
}
