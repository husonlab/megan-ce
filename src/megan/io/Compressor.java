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
package megan.io;

//import jloda.util.Basic;

import jloda.util.Basic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * compress and decompress strings
 * Daniel Huson, 8.2008
 */
public class Compressor {
    private final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
    private final Inflater inflater = new Inflater(true);
    private byte[] buffer;
    public static final int MIN_SIZE_FOR_DEFLATION = 90;
    private boolean enabled = true;

    /**
     * default constructor
     */
    public Compressor() {
        this(1000000);
    }

    /**
     * constructor
     *
     * @param maxStringLength - size of buffer
     */
    public Compressor(int maxStringLength) {
        buffer = new byte[maxStringLength];
    }

    /**
     * gets a deflated string
     *
     * @param inputString
     * @return deflated string
     */
    public byte[] deflateString2ByteArray(String inputString) {
        int length = deflateString2ByteArray(inputString, buffer);
        byte[] result = new byte[Math.abs(length)];
        System.arraycopy(buffer, 0, result, 0, Math.abs(length));
        return result;
    }


    /**
     * compresses a string to an array of bytes
     *
     * @param inputString
     * @param bytes       array to write bytes to
     * @return number of bytes written  (negative number, if bytes are not deflated)
     */
    private int deflateString2ByteArray(String inputString, byte[] bytes) {
        byte[] input;
        try {
            input = inputString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
//            Basic.caught(e);
            input = new byte[inputString.length()];
            for (int i = 0; i < bytes.length; i++)
                input[i] = (byte) inputString.charAt(i);
        }
        return deflateString2ByteArray(input, 0, input.length, bytes);
    }

    /**
     * compresses a string to an array of bytes
     *
     * @param input
     * @param inputOffset
     * @param inputLength
     * @param bytes       array to write bytes to
     * @return number of bytes written  (negative number, if bytes are not deflated)
     */
    public int deflateString2ByteArray(byte[] input, int inputOffset, int inputLength, byte[] bytes) {
        if (inputLength >= MIN_SIZE_FOR_DEFLATION) {
            // Compress the bytes
            deflater.setInput(input, inputOffset, inputLength);
            deflater.finish();
            int compressedDataLength = deflater.deflate(bytes);
            deflater.reset();
            return -compressedDataLength;
        } else {
            System.arraycopy(input, inputOffset, bytes, 0, inputLength);
            return inputLength;
        }
    }

    /**
     * decompresses an array of bytes to a string
     *
     * @param numberOfBytes
     * @param bytes
     * @return decoded string
     * @throws DataFormatException
     */
    public String inflateByteArray2String(int numberOfBytes, byte[] bytes) throws DataFormatException {
        /*
        StringBuilder buf = new StringBuilder();
        for (byte aByte : input) buf.append((char) aByte);
        return buf.toString();
        */
        if (numberOfBytes == 0)
            return "";

        if (numberOfBytes < 0) // negative number means uncompressed!
        {
            try {
                return new String(bytes, 0, -numberOfBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
//                Basic.caught(e);
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < -numberOfBytes; i++)
                    buf.append((char) bytes[i]);
                return buf.toString();
            }
        }
        inflater.setInput(bytes, 0, numberOfBytes);
        if (buffer.length < 100 * bytes.length)  // try to make sure the result buffer is long enough
            buffer = new byte[100 * bytes.length];
        int resultLength = inflater.inflate(buffer);

        String outputString;
        try {
            outputString = new String(buffer, 0, resultLength, "UTF-8");
        } catch (UnsupportedEncodingException e) {
//            Basic.caught(e);
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < resultLength; i++)
                buf.append((char) buffer[i]);
            outputString = buf.toString();

        }
        inflater.reset();
        return outputString;
    }

    /**
     * decompresses an array of bytes to bytes
     *
     * @param numberOfBytes negative, if uncompressed, otherwise positive
     * @param source        input
     * @param target        output
     * @return number of bytes
     * @throws DataFormatException
     */
    public int inflateByteArray(int numberOfBytes, byte[] source, byte[] target) throws DataFormatException {
        /*
        StringBuilder buf = new StringBuilder();
        for (byte aByte : input) buf.append((char) aByte);
        return buf.toString();
        */
        if (numberOfBytes == 0)
            return 0;

        if (numberOfBytes < 0) // negative number means uncompressed!
        {
            System.arraycopy(source, 0, target, 0, source.length);
            return Math.abs(numberOfBytes);
        }
        inflater.setInput(source, 0, numberOfBytes);
        int resultLength = inflater.inflate(target);
        inflater.reset();
        return resultLength;
    }

    /**
     * interactively test deflation and inflation
     *
     * @param args
     * @throws IOException
     * @throws DataFormatException
     */
    public static void main(String[] args) throws IOException, DataFormatException {

        Compressor compression = new Compressor();

        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("> ");
        System.out.flush();

        String inputString = "";
        String aLine;
        while ((aLine = r.readLine()) != null) {
            if (aLine.length() > 0) {
                if (aLine.equals(".")) {
                    // Encode a String into bytes
                    byte[] bytes = new byte[inputString.length() + 1000];
                    int numberOfBytes = compression.deflateString2ByteArray(inputString, bytes);

                    // Decode the bytes into a String
                    String outputString;
                    if (numberOfBytes < 0)
                        outputString = compression.inflateByteArray2String(-numberOfBytes, bytes);
                    else
                        outputString = Compressor.convertUncompressedByteArray2String(numberOfBytes, bytes);
                    System.err.println("=<" + outputString + ">");
                    System.err.println("= " + outputString);
                    System.err.println("uncompressed: " + inputString.length());
                    System.err.println("compressed:   " + numberOfBytes);
                    System.err.println("decompressed: " + outputString.length());
                    {
                        byte[] target = new byte[10 * bytes.length];
                        compression.inflateByteArray(-numberOfBytes, bytes, target);
                        System.err.println("decompressed bytes: " + Basic.toString(target));
                    }

                    inputString = "";
                    System.out.print("> ");
                } else {
                    inputString += aLine + "\n";
                    System.out.print("? ");
                }
                System.out.flush();
            }
        }
    }

    /**
     * is enabled. Has no effect
     *
     * @return true, if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * set enabled state. Has no effect
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * * convert an uncompressed array of bytes to a string
     *
     * @param size
     * @param bytes
     * @return string
     */
    static public String convertUncompressedByteArray2String(int size, byte[] bytes) {
        StringBuilder buf = new StringBuilder(size);
        for (byte b : bytes) buf.append((char) b);
        return buf.toString();
    }
}
