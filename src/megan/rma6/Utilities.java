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
package megan.rma6;

import jloda.util.Basic;
import jloda.util.FileLineIterator;
import jloda.util.FileLineBytesIterator;
import jloda.util.Single;
import megan.io.InputReader;

import java.io.File;
import java.io.IOException;

/**
 * Some utilities for creating Rma6 files
 * Created by huson on 5/23/14.
 */
class Utilities {
    /**
     * find the query in the reads file. When found, FileLineBytesIterator is pointing to location of query in file
     *
     * @param queryName
     * @param it
     * @param isFastA
     * @return true, if found
     */
    public static boolean findQuery(byte[] queryName, int queryNameLength, FileLineBytesIterator it, boolean isFastA) {
        try {
            if (isFastA) {
                while (it.hasNext()) {
                    byte[] line = it.next();
                    if (line[0] == '>' && matchName(queryName, queryNameLength, line, it.getLineLength()))
                        return true;
                }
            } else { // assume that this is fastQ
                if (it.getLinePosition() == 0) // at beginning of file
                {
                    byte[] line = it.next();
                    // System.err.println(Basic.toString(line,it.getLineLength()));
                    if (line[0] != '@')
                        throw new IOException("Expected FastQ header line (starting with '@'), got: " + Basic.toString(line, it.getLineLength()));
                    if (matchName(queryName, queryNameLength, line, it.getLineLength()))
                        return true;
                    it.next();
                    it.next();
                    it.next();
                }
                while (it.hasNext()) {
                    byte[] line = it.next();
                    // System.err.println(Basic.toString(line,it.getLineLength()));
                    if (line[0] != '@')
                        throw new IOException("Expected FastQ header line (starting with '@'), got: " + Basic.toString(line, it.getLineLength()));
                    if (matchName(queryName, queryNameLength, line, it.getLineLength()))
                        return true;
                    it.next();
                    it.next();
                    it.next();
                }
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return false;
    }

    /**
     * assuming that the FileLineBytesIterator has just returned the header line of a fastA or fastQ record, writes the full text of the match
     *
     * @param it
     * @param isFastA
     * @return string
     */
    public static String getFastAText(FileLineBytesIterator it, boolean isFastA) {
        final StringBuilder buf = new StringBuilder();

        if (isFastA) {
            byte[] bytes = it.getLine();
            while (true) {
                for (int i = 0; i < it.getLineLength(); i++)
                    buf.append((char) bytes[i]);
                if (!it.hasNext() || it.peekNextByte() == '>')
                    break;
                bytes = it.next();
            }
        } else // fastq, copy in fastA format...
        {
            byte[] bytes = it.getLine();
            buf.append(">");
            for (int i = 1; i < it.getLineLength(); i++) // first line has header, skip the leading '@'
                buf.append((char) bytes[i]);
            if (it.hasNext()) { // second line has sequence
                bytes = it.next();
                for (int i = 0; i < it.getLineLength(); i++)
                    buf.append((char) bytes[i]);
            }
            // skip the two next lines:
            if (it.hasNext())
                it.next();
            if (it.hasNext())
                it.next();
        }
        return buf.toString();

    }

    /**
     * assuming that the FileLineBytesIterator has just returned the header line of a fastA or fastQ record, writes the full text of the match
     *
     * @param it
     * @param isFastA
     */
    public static void skipFastAText(FileLineBytesIterator it, boolean isFastA) {
        if (isFastA) {
            while (it.hasNext() && it.peekNextByte() != '>') {
                it.next();
            }
        } else // fastq
        {
            if (it.hasNext()) { // second
                it.next();
            }
            if (it.hasNext()) { // third
                it.next();
            }
            if (it.hasNext()) { // fourth line
                it.next();
            }
        }
    }


    /**
     * assuming that the FileLineBytesIterator has just returned the header line of a fastA or fastQ record, copies the full text of the match
     *
     * @param it
     * @param isFastA
     * @return size
     */
    public static int getFastAText(FileLineBytesIterator it, boolean isFastA, Single<byte[]> result) { // todo: has not been tested!
        byte[] buffer = result.get();
        if (isFastA) {
            byte[] bytes = it.getLine();
            int length = 0;
            while (true) {
                if (length + it.getLineLength() >= buffer.length) { // grow result buffer
                    byte[] tmp = new byte[2 * (length + it.getLineLength())];
                    System.arraycopy(buffer, 0, tmp, 0, length);
                    buffer = tmp;
                    result.set(buffer);
                }
                System.arraycopy(bytes, 0, buffer, length, it.getLineLength());
                length += it.getLineLength();

                if (!it.hasNext() || it.peekNextByte() == '>')
                    break;
                bytes = it.next();
            }
            return length;
        } else // fastq
        {
            byte[] bytes = it.getLine();
            if (it.getLineLength() + 1 >= buffer.length) { // grow result buffer
                buffer = new byte[2 * (it.getLineLength() + 1)];
                result.set(buffer);
            }
            int length = 0;
            buffer[length++] = '>'; // first character is '>' (not '@')
            System.arraycopy(bytes, 1, buffer, length, it.getLineLength() - 1);
            length += it.getLineLength() - 1;
            buffer[length++] = '\n';

            if (it.hasNext()) { // second line has sequence
                bytes = it.next();
                if (length + it.getLineLength() + 1 >= buffer.length) { // grow result buffer
                    byte[] tmp = new byte[2 * (length + it.getLineLength() + 1)];
                    System.arraycopy(buffer, 0, tmp, 0, length);
                    buffer = tmp;
                    result.set(buffer);
                }
                System.arraycopy(bytes, 0, buffer, length, it.getLineLength());
                length += it.getLineLength();
            }
            if (it.hasNext()) { // third
                it.next();
            }
            if (it.hasNext()) { // fourth line
                it.next();
            }
            return length;
        }
    }

    /**
     * match header line with query name
     *
     * @param queryName
     * @param line
     * @param lineLength
     * @return true, if name matches name in line
     */
    private static boolean matchName(byte[] queryName, int queryNameLength, byte[] line, int lineLength) {
        int start = 0;
        if (line[start] == '>' || line[0] == '@')
            start++;
        while (Character.isWhitespace(line[start]) && start < lineLength)
            start++;
        int end = start;
        while (!Character.isWhitespace(line[end]) && end < lineLength) {
            end++;
        }

        if (end - start != queryNameLength)
            return false; // have different lengths

        for (int i = 0; i < queryNameLength; i++) {
            if (queryName[i] != line[start + i])
                return false;
        }
        return true;
    }

    /**
     * returns a fastA or fastQ record as FastA
     *
     * @param reader
     * @param position
     * @return fastA record at current position
     */
    public static String getFastAText(InputReader reader, long position) throws IOException {
        StringBuilder buf = new StringBuilder();

        reader.seek(position);
        char letter = (char) reader.read();
        boolean isFastA = (letter == '>');
        if (!isFastA && (letter != '@'))
            throw new IOException("Expected '>' or '@' at position: " + position + ", got: " + letter);

        buf.append('>');

        if (isFastA) {
            letter = (char) reader.read();
            while (letter != '>') {
                if (letter != '\r')
                    buf.append(letter);
                letter = (char) reader.read();
            }
        } else // fastq, copy in fastA format...
        {
            boolean seenFirstEndOfLine = false;
            letter = (char) reader.read();
            while (true) {
                if (letter != '\r')
                    buf.append(letter);
                if (letter == '\n') {
                    if (!seenFirstEndOfLine)
                        seenFirstEndOfLine = true;
                    else
                        break;
                }
                letter = (char) reader.read();
            }
        }
        return buf.toString();
    }

    /**
     * get the header from a fastA record
     *
     * @param fastAText
     * @return header
     */
    public static String getFastAHeader(String fastAText) {
        int end = fastAText.indexOf('\n');
        if (end == -1)
            return fastAText;
        else
            return fastAText.substring(0, end);
    }

    /**
     * get the seqiemce from a fastA record
     *
     * @param fastAText
     * @return header
     */
    public static String getFastASequence(String fastAText) {
        int start = fastAText.indexOf('\n');
        if (start == -1)
            return "Unavailable";
        else
            return fastAText.substring(start + 1);
    }

    /**
     * is the given file a MALT or Diamond -generated SAM file?
     *
     * @param file
     * @return true, if file is MALT or Diamond generated SAM file
     */
    public static boolean IsMaltOrDiamondSAMFile(File file) {
        String suffix = Basic.getFileSuffix(Basic.getFileNameWithoutZipOrGZipSuffix(file.getName()));
        if (suffix == null)
            return false;
        if (!suffix.toLowerCase().equals(".sam"))
            return false;
        try {
            try (FileLineIterator it = new FileLineIterator(file.getPath())) {
                while (it.hasNext()) {
                    String aLine = it.next();
                    if (aLine.startsWith("@")) {
                        if (aLine.contains("PN:MALT") || (aLine.contains("PN:DIAMOND")))
                            return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    /**
     * gets the specified token from a tab separated text
     *
     * @param n      which token (starting at 0)
     * @param text
     * @param offset
     * @return n-th token
     */
    public static String getToken(int n, byte[] text, int offset) {
        int prev = offset;
        while (true) {
            while (text[offset] != '\t')
                offset++;
            if (n-- == 0)
                break;
            offset++;
            prev = offset;
        }
        return new String(text, prev, offset - prev);

    }

    /**
     * gets the position of the next newline character
     *
     * @param text
     * @param offset
     * @return next new line
     */
    public static int nextNewLine(byte[] text, int offset) {
        while (offset < text.length) {
            if (text[offset] == '\n')
                return offset;
            else
                offset++;
        }
        return offset;
    }

    /**
     * split a given text into strings
     *
     * @param text
     * @param offset
     * @param end
     * @param splitChar
     * @return strings
     */
    public static String[] split(byte[] text, int offset, int end, byte splitChar) {
        // skip leading white space:
        while (Character.isWhitespace((char) text[offset]) && offset < end)
            offset++;

        int count = 0;
        for (int i = offset; i < end; i++) {
            if (text[i] == splitChar)
                count++;
        }
        String[] result = new String[count];
        count = 0;
        for (int i = offset; i < end; i++) {
            if (text[i] == splitChar) {
                result[count++] = new String(text, offset, i - offset);
                offset = i + 1;
            }
        }
        return result;
    }
}
