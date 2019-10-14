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

package megan.daa.io;

/**
 * Packed sequence
 * Daniel Huson, 8.2015
 */
class PackedSequence {
    /**
     * read packed sequence from buffer
     *
     * @param buffer
     * @param query_len
     * @param bits
     * @return packed sequence
     */
    public static byte[] readPackedSequence(ByteInputBuffer buffer, int query_len, int bits) {
        int size = (query_len * bits + 7) / 8;
        //System.err.println("len=" + query_len);
        //System.err.println("b=" + bits);
        //System.err.println("size=" + size);
        return buffer.readBytes(size);
    }

    /**
     * get the unpacked sequence
     *
     * @return unpacked sequence
     */
    public static byte[] getUnpackedSequence(byte[] packed, int query_len, int bits) {
        byte[] result = new byte[query_len];
        long x = 0;
        int n = 0, l = 0;
        int mask = (1 << bits) - 1;

        for (byte b : packed) {
            x |= (b & 0xFF) << n;
            n += 8;

            while (n >= bits && l < query_len) {
                result[l] = (byte) (x & mask);
                n -= bits;
                x >>>= bits;
                l++;
            }
        }
        return result;
    }

    /**
     * report sequence in human-readable unpacked format
     *
     * @return unpacked
     */
    public static String toStringUnpacked(byte[] unpacked) {
        StringBuilder buf = new StringBuilder();
        for (byte a : unpacked) buf.append(String.format("%d", a));
        return buf.toString();
    }

    /**
     * report sequence in human-readable unpacked format
     *
     * @return unpacked
     */
    public static String toStringPacked(byte[] packed) {
        StringBuilder buf = new StringBuilder();
        for (byte a : packed) buf.append(" ").append(a & 0xFF);
        return buf.toString();
    }
}
