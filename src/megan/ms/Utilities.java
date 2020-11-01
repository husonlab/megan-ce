/*
 * Copyright (C) 2020. Daniel H. Huson
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
 *
 */

package megan.ms;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jloda.util.Basic;
import megan.daa.connector.ClassificationBlockDAA;
import megan.daa.io.ByteInputStream;
import megan.daa.io.ByteOutputStream;
import megan.daa.io.InputReaderLittleEndian;
import megan.daa.io.OutputWriterLittleEndian;
import megan.data.IClassificationBlock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * megan server utilities
 * Daniel Huson, 8.2020
 */
public class Utilities {
    private static final byte[] SALT="7DFjUnE9p2uDeDu0".getBytes();

    public static final String SERVER_ERROR = "401 Error:";

    /**
     * construct classification block from bytes
     */
    public static IClassificationBlock getClassificationBlockFromBytes(byte[] bytes) throws IOException {
        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new ByteInputStream(bytes, 0, bytes.length))) {
            final String classificationName = ins.readNullTerminatedBytes();
            final ClassificationBlockDAA classificationBlock = new ClassificationBlockDAA(classificationName);
            int numberOfClasses = ins.readInt();

            for (int c = 0; c < numberOfClasses; c++) {
                int classId = ins.readInt();
                classificationBlock.setWeightedSum(classId, ins.readInt());
                classificationBlock.setSum(classId, ins.readInt());
            }
            return classificationBlock;
        }
    }

    /**
     * save classification block to bytes
     */
    public static byte[] writeClassificationBlockToBytes(IClassificationBlock classificationBlock) throws IOException {
        try (ByteOutputStream stream = new ByteOutputStream();
             OutputWriterLittleEndian outs = new OutputWriterLittleEndian(stream)) {
            outs.writeNullTerminatedString(classificationBlock.getName().getBytes());
            final int numberOfClasses = classificationBlock.getKeySet().size();
            outs.writeInt(numberOfClasses);
            for (int classId : classificationBlock.getKeySet()) {
                outs.writeInt(classId);
                outs.writeInt((int) classificationBlock.getWeightedSum(classId));
                outs.writeInt(classificationBlock.getSum(classId));
            }
            return stream.getExactLengthCopy();
        }
    }

    public static Map<String, byte[]> getAuxiliaryDataFromBytes(byte[] bytes) throws IOException {
        final Map<String, byte[]> label2data = new TreeMap<>();

        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new ByteInputStream(bytes, 0, bytes.length))) {
            final int numberOfLabels = ins.readInt();
            for (int i = 0; i < numberOfLabels; i++) {
                final String label = ins.readNullTerminatedBytes();
                final int size = ins.readInt();
                final byte[] data = new byte[size];
                final int length = ins.read_available(data, 0, size);
                if (length < size) {
                    final byte[] tmp = new byte[length];
                    System.arraycopy(data, 0, tmp, 0, length);
                    label2data.put(label, tmp);
                    throw new IOException("buffer underflow");
                }
                label2data.put(label, data);
            }
        }
        return label2data;
    }

    public static byte[] writeAuxiliaryDataToBytes(Map<String, byte[]> label2data) throws IOException {
        try (ByteOutputStream stream = new ByteOutputStream();
             OutputWriterLittleEndian outs = new OutputWriterLittleEndian(stream)) {
            outs.writeInt(label2data.size());
            for (String label : label2data.keySet()) {
                outs.writeNullTerminatedString(label.getBytes());
                final byte[] data = label2data.get(label);
                outs.writeInt(data.length);
                outs.write(data, 0, data.length);
            }
            return stream.getExactLengthCopy();
        }
    }

    public static byte[] getBytesLittleEndian(int a) {
        return new byte[]{(byte) a, (byte) (a >> 8), (byte) (a >> 16), (byte) (a >> 24)};
    }

    public static byte[] getBytesLittleEndian(long a) {
        return new byte[]{(byte) a, (byte) (a >> 8), (byte) (a >> 16), (byte) (a >> 24), (byte) (a >> 32), (byte) (a >> 40), (byte) (a >> 48), (byte) (a >> 56)};
    }

    public static  String computeBCryptHash(byte[] password) {
        return Basic.toString(BCrypt.withDefaults().hash(6, SALT, password));
    }

    public static boolean verify (char[] password,String bcryptHash) {
        return BCrypt.verifyer().verify(password,bcryptHash).verified;
    }
}
