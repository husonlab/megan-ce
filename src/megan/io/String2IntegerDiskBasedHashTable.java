/*
 *  Copyright (C) 2015 Daniel H. Huson
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

package megan.io;

import malt.util.MurmurHash3;

import java.io.*;

/**
 * a disk-based string-to-int hash table
 * Daniel Huson, 3.2016
 */
public class String2IntegerDiskBasedHashTable implements Closeable {
    public static int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 5;

    public static final byte[] MAGIC_NUMBER = {'S', 'I', '1'};

    private final ByteFileGetterMappedMemory dataByteBuffer;

    private final long dataStartPos;

    private final int size;
    private final int mask;

    private final int cacheBits = 15;
    private final int cacheSize = (1 << cacheBits);
    private final byte[][] cacheKeys = new byte[cacheSize][];
    private final int[] cacheValues = new int[cacheSize];

    /**
     * constructor
     *
     * @param fileName
     * @throws FileNotFoundException
     */
    public String2IntegerDiskBasedHashTable(String fileName) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(fileName, "r")) {
            for (int i = 0; i < 3; i++) {
                int b = raf.read();
                if (b != MAGIC_NUMBER[i])
                    throw new IOException("File has wrong magic number");
            }
            byte bits = (byte) raf.read();
            if (bits <= 0 || bits >= 30)
                throw new IOException("Bits out of range: " + bits);
            mask = (1 << bits) - 1;
            dataStartPos = 4 * (mask + 2);

            dataByteBuffer = new ByteFileGetterMappedMemory(new File(fileName));
            size = dataByteBuffer.getInt(dataByteBuffer.limit() - 4);

            if (size < 0)
                throw new IOException("Bad size: " + size);

            /*
            for(int i=4;i< dataStartPos;i+=4) {
                System.err.println("buf: "+i + " -> " + dataByteBuffer.getInt(i));
                raf.seek(i);
                System.err.println("raf: "+i + " -> " + raf.readInt());
            }
            */
        }
    }

    public int size() {
        return size;
    }


    /**
     * get the value for a key
     */
    public int get(String keyString) throws IOException {
        byte[] key = keyString.getBytes();
        final int keyHash = computeHash(key, 0, key.length, mask);
        long dataOffset = dataByteBuffer.getInt(4 * (keyHash + 1));
        if (dataOffset == 0)
            return 0;
        // cache:
        final int cacheIndex = (keyHash & cacheBits);
        synchronized (cacheKeys)
        {
            final byte[] cacheKey = cacheKeys[cacheIndex];
            if (cacheKey != null && equal(key, cacheKey))
                return cacheValues[cacheIndex];
        }

        if (dataOffset < 0) { // need to expand
            dataOffset = (long) Integer.MAX_VALUE + (dataOffset & (Integer.MAX_VALUE)) + 1;
        }

        dataOffset += dataStartPos;

            while (true) {
                final int numberOfBytes = readAndCompareBytes0Terminated(key, key.length, dataOffset, dataByteBuffer);
                if (numberOfBytes == 0)
                    break;
                else if (numberOfBytes < 0)
                    dataOffset += -numberOfBytes + 5; //  add 1 for terminating 0 and 4 for value
                else {
                    dataOffset += numberOfBytes + 1;    //  add 1 for terminating 0
                    //System.err.println("saw: " + Basic.toString(bytes, 0, numberOfBytes) + " value=" + value);
                    final int value = dataByteBuffer.getInt(dataOffset);
                    // cache:
                    synchronized (cacheKeys)
                    {
                        cacheKeys[cacheIndex] = key;
                        cacheValues[cacheIndex] = value;
                    }
                    return value;
                }
            }

        return 0;
    }

    /**
     * equalOverShorterOfBoth keys?
     *
     * @param key1
     * @param key2
     * @return true if the same
     */
    private boolean equal(byte[] key1, byte[] key2) {
        if (key1.length != key2.length)
            return false;
        else {
            for (int i = 0; i < key1.length; i++) {
                if (key1[i] != key2[i])
                    return false;
            }
            return true;
        }
    }

    @Override
    public void close() throws IOException {
        dataByteBuffer.close();
    }

    /**
     * compute the hash value for a given key
     *
     * @param key
     * @param mask
     * @return hash
     */
    public static int computeHash(byte[] key, int mask) {
        return Math.abs(MurmurHash3.murmurhash3x8632(key, 0, key.length, 666) & mask);
    }

    /**
     * compute the hash value for a given key
     *
     * @param key
     * @param mask
     * @return hash
     */
    public static int computeHash(byte[] key, int offset, int length, int mask) {
        return Math.abs(MurmurHash3.murmurhash3x8632(key, offset, length, 666) & mask);
    }

    /**
     * read and compare 0-terminated bytes,
     *
     * @param byteBuffer
     * @return number of bytes read excluding termining 0, if match, or -number of bytes read, if no match
     * @throws IOException
     */
    private int readAndCompareBytes0Terminated(byte[] key, int keyLength, long pos, ByteFileGetterMappedMemory byteBuffer) throws IOException {
        int i = 0;
        boolean equal = true;
        // byte[] got=new byte[10000];

        while (true) {
            final byte b = (byte) byteBuffer.get(pos++);
            //got[i]=b;
            if (b == 0)
                break;
            if (i < keyLength) {
                if (equal && b != key[i]) {
                    equal = false;
                }
            }
            i++;
        }

        //System.err.println("Looking for: "+ Basic.toString(key,0,keyLength)+", got: "+Basic.toString(got,0,i));
        return (equal && i == keyLength) ? i : -i; // negative means no match
    }

    public static void main(String[] args) throws IOException {
        try (String2IntegerDiskBasedHashTable table = new String2IntegerDiskBasedHashTable("/Users/huson/mapping/ncbi-March2016/nucl_acc2tax-March2016.abin")) {
            String accession = "NC_009085";
            System.err.println(accession + " -> " + table.get(accession));
        }
    }
}
