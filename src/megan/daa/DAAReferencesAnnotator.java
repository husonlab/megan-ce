/*
 * DAAReferencesAnnotator.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.daa;

import javafx.beans.property.SimpleObjectProperty;
import jloda.fx.util.ProgramExecutorService;
import jloda.swing.util.ProgramProperties;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.accessiondb.AccessAccessionMappingDatabase;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdParser;
import megan.daa.io.*;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/**
 * adds reference annotations to DAA file
 * Daniel Huson, 8.2105
 */
class DAAReferencesAnnotator {
    /**
     * add reference annotations to a DAA file
     *
	 */
    public static void apply(String daaFile, boolean doTaxonomy, Collection<String> cNames0, final ProgressListener progress) throws IOException {
        DAAModifier.removeAllMEGANData(daaFile);

        final var header = new DAAHeader(daaFile);
        header.load();
        header.loadReferences(false);

        final String[] cNames;
        {
            final var fNamesList = new LinkedList<>(cNames0);
            if (doTaxonomy && !fNamesList.contains(Classification.Taxonomy)) {
                fNamesList.add(Classification.Taxonomy);
            } else if (!doTaxonomy)
                fNamesList.remove(Classification.Taxonomy);
            cNames = fNamesList.toArray(new String[0]);
        }

        final var cName2ref2class = new int[cNames.length][header.getNumberOfReferences()];

        final var service = Executors.newCachedThreadPool();
        var exception = new SimpleObjectProperty<Exception>();

        try {
            if (ClassificationManager.canUseMeganMapDBFile()) {
                System.err.println("Annotating DAA file using FAST mode (accession database and first accession per line)");
                progress.setSubtask("Annotating references");

                final var chunkSize = ProgramProperties.get("AccessionChunkSize",5000); // don't make this too big, query will exceed SQLITE size limitation

                final var numberOfTasks = (int) Math.ceil((double) header.getNumberOfReferences() / chunkSize);

                final var countDownLatch = new CountDownLatch(numberOfTasks);

                final var numberOfThreads = Math.min(numberOfTasks, ProgramExecutorService.getNumberOfCoresToUse());

                //System.err.println("Number of tasks: "+numberOfTasks);
                //System.err.println("Number of threads: "+numberOfThreads);

                progress.setMaximum(numberOfTasks / numberOfThreads);
                progress.setProgress(0);

                for (var t = 0; t < numberOfThreads; t++) {
                    final var task = t;
                    service.submit(() -> {
                        try (final var accessAccessionMappingDatabase = new AccessAccessionMappingDatabase(ClassificationManager.getMeganMapDBFile())) {
                            final var mapClassificationId2DatabaseRank = accessAccessionMappingDatabase.setupMapClassificationId2DatabaseRank(cNames);

                            final var queries = new String[chunkSize];
                            for (var r = task * chunkSize; r < header.getNumberOfReferences(); r += numberOfThreads * chunkSize) {
                                try {
                                    if (exception.get() != null)
                                        return;
                                    for (var i = 0; i < chunkSize; i++) {
                                        final var a = r + i;
                                        if (a < header.getNumberOfReferences()) {
                                            queries[i] = getFirstWord(header.getReference(a, null));
                                        } else
                                            break;
                                    }
                                    final var size = Math.min(chunkSize, header.getNumberOfReferences() - r);
                                    final var query2ids = accessAccessionMappingDatabase.getValues(queries, size);
                                    for (var q = 0; q < size; q++) {
                                        final var ids = query2ids.get(queries[q]);
                                        if (ids != null) {
                                            for (var c = 0; c < cNames.length; c++) {
                                                final var dbRank = mapClassificationId2DatabaseRank[c];
                                                if (dbRank < ids.length)
                                                    cName2ref2class[c][r + q] = ids[dbRank];
                                            }
                                        }
                                    }
                                } finally {
                                    if (task == 0)
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

                final var numberOfThreads = Math.max(1, Math.min(header.getNumberOfReferences(), Math.min(ProgramExecutorService.getNumberOfCoresToUse(), Runtime.getRuntime().availableProcessors())));
                final var countDownLatch = new CountDownLatch(numberOfThreads);

                progress.setSubtask("Annotating references");
                progress.setMaximum(header.getNumberOfReferences());
                progress.setProgress(0);

                // determine the names for references:
                for (var t = 0; t < numberOfThreads; t++) {
                    final var task = t;
                    service.submit(() -> {
                        try {
                            final var idParsers = new IdParser[cNames.length];

                            // need to use only one database per thread.

                            for (var i = 0; i < cNames.length; i++) {
                                idParsers[i] = ClassificationManager.get(cNames[i], true).getIdMapper().createIdParser();
                            }

                            for (var r = task; r < header.getNumberOfReferences(); r += numberOfThreads) {
                                final var ref = StringUtils.toString(header.getReference(r, null));
                                for (var i = 0; i < idParsers.length; i++) {
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

            // get all  bytes:
            final var cName2Bytes = new byte[cNames.length][];
            final var cName2Size = new int[cNames.length];

            final var countDownLatch2 = new CountDownLatch(cNames.length);
            for (var t = 0; t < cNames.length; t++) {
                final var task = t;
                service.submit(() -> {
                    try {
                        final var outs = new ByteOutputStream(1048576);
                        final var w = new OutputWriterLittleEndian(outs);
                        w.writeNullTerminatedString(cNames[task].getBytes());
                        final var ref2class = cName2ref2class[task];

                        if (task == 0) {
                            progress.setSubtask("Writing");
                            progress.setMaximum(ref2class.length);
                            progress.setProgress(0);
                        }

                        for (var classId : ref2class) {
                            w.writeInt(classId);
                            if (task == 0)
                                progress.incrementProgress();
                        }

                        cName2Bytes[task] = outs.getBytes();
                        cName2Size[task] = outs.size();
                    } catch (Exception ex) {
                        System.err.println("Exception during preparation of block: " + cNames[task]);
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
            progress.reportTaskCompleted();
        } finally {
            service.shutdownNow();
        }
    }

    private static String getFirstWord(byte[] bytes) {
        var a = 0;
        while (a < bytes.length && (bytes[a] == '>' || Character.isWhitespace(bytes[a]))) {
            a++;
        }
        var b = a;
        while (b < bytes.length && (bytes[b] == '_' || Character.isLetterOrDigit(bytes[b]))) {
            b++;
        }
        return new String(bytes, a, b - a);
    }
}
