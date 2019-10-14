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
package megan.classification.data;

import jloda.util.*;
import megan.data.IName2IdMap;
import megan.io.OutputWriter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * long to integer mapping that can be loaded from and saved to a file
 * Daniel Huson, 4.2010, 4.2015
 */
public class Long2IntegerFileBasedMap implements ILong2IntegerMap, Closeable {
    public static final int MAGIC_NUMBER = 666; // write this as first number so that we can recognize file

    private final static int BITS = 10; // 2^10=1024
    private final static int SIZE = (1 << BITS);
    private final static int MASK = (SIZE - 1);

    private final IntIntMap[] maps;

    /**
     * constructor
     *
     * @param label2id
     * @param fileName
     * @param progress
     * @throws IOException
     * @throws CanceledException
     */
    public Long2IntegerFileBasedMap(final IName2IdMap label2id, final String fileName, final ProgressListener progress) throws IOException, CanceledException {
        maps = new IntIntMap[SIZE];
        for (int i = 0; i < maps.length; i++) {
            maps[i] = new IntIntMap(2 ^ 20, 0.9f); // 2^20=1048576
        }

        final File file = new File(fileName);
        System.err.println("Loading file: " + file.getName());

        int totalIn = 0;
        int totalSkipped = -100; // allow ourselves 100 skips more than totalIn before we give up...

        try (final FileLineIterator it = new FileLineIterator(file)) {
            progress.setTasks("Loading file", file.getName());
            progress.setProgress(0);
            progress.setMaximum(it.getMaximumProgress());
            while (it.hasNext()) {
                String[] tokens = it.next().split("\t");
                if (tokens[0].length() > 0 && tokens[0].charAt(0) != '#' && tokens.length == 2) {
                    long giNumber = Basic.parseLong(tokens[0]);
                    if (giNumber > 0) {
                        if (Basic.isInteger(tokens[1])) {
                            int id = Basic.parseInt(tokens[1]);
                            if (id != 0) {
                                put(giNumber, id);
                                totalIn++;
                            }
                        } else if (label2id != null) {
                            int id = label2id.get(tokens[1]);
                            if (id != 0) {
                                put(giNumber, id);
                                totalIn++;
                            }
                        }
                    }
                } else {
                    if (totalSkipped++ > totalIn)
                        throw new IOException("Failed to parse too many lines, is this really a .map or .bin file? " + fileName);
                }
                progress.setProgress(it.getProgress());
            }
        }
        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();

        System.err.println(String.format("Entries: %,10d", totalIn));
    }

    /**
     * add a value to the map maintained in memory
     *
     * @param key
     * @param value
     */
    private void put(long key, int value) {
        maps[(int) (key & MASK)].put((int) (key >>> BITS), value);
    }

    /**
     * lookup an id from a gi
     *
     * @param key
     * @return id or 0
     */
    public int get(long key) throws IOException {
        if (key <= 0)
            return 0;

        synchronized (maps) {
            final int whichArray = (int) (key & MASK);
            final int index = (int) (key >>> BITS);
            return maps[whichArray].get(index);
        }
    }

    @Override
    public void close() throws IOException {
    }

    /**
     * converts the named dmp file to a bin file
     *
     * @param dmpFile
     * @param binFile
     * @throws IOException
     */
    public static void writeToBinFile(File dmpFile, File binFile) throws IOException {
        System.err.println("Converting " + dmpFile.getName() + " to " + binFile.getName() + "...");

        long totalOut = 0;
        try (final FileLineIterator it = new FileLineIterator(dmpFile, true);
             OutputWriter outs = new OutputWriter(binFile)) {
            System.err.println("Writing file: " + binFile);
            outs.writeInt(MAGIC_NUMBER);

            long lastGi = 0;
            int lineNo = 0;
            while (it.hasNext()) {
                String aLine = it.next();
                lineNo++;
                final int pos = aLine.indexOf('\t');
                final String giString = aLine.substring(0, pos);
                final int dotPos = giString.indexOf('.');
                final long gi = Long.parseLong(dotPos > 0 ? giString.substring(0, dotPos) : giString);
                if (gi >= 0) {
                    final int taxId = Integer.parseInt(aLine.substring(pos + 1));

                    if (gi <= lastGi)
                        throw new IOException("Error, line: " + lineNo + ": GIs out of order: " + gi + " after " + lastGi);

                    // fill in missing Gis
                    final int missing = (int) (gi - 1 - lastGi);
                    for (int i = 0; i < missing; i++)
                        outs.writeInt(0);

                    outs.writeInt(taxId);
                    totalOut++;
                    lastGi = gi;
                }
            }
        }
        System.err.println("done (" + totalOut + " entries)");
    }
}
