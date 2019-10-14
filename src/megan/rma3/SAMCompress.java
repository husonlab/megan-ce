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
package megan.rma3;

/**
 * deflate and inflate SAM line based on previous one
 * Created by huson on 5/24/14.
 */
class SAMCompress {
    /**
     * deflate current SAM line by replacing all fields that equalOverShorterOfBoth previous line by an ampersand
     *
     * @param previous
     * @param current
     * @return deflated SAM line
     */
    public static int deflate(byte[] previous, int previousLength, byte[] current, int currentLength, byte[] result) {
        if (previous == null) {
            System.arraycopy(current, 0, result, 0, currentLength);
            return currentLength;
        }

        int length = 0;

        int start1 = 0;
        int start2 = 0;
        while (start1 < previousLength && start2 < currentLength) {
            int end1 = start1;
            int end2 = start2;

            while (end1 <= previousLength && end2 <= currentLength) {
                byte c1 = (end1 < previousLength ? previous[end1] : (byte) '\t');
                byte c2 = (end2 < currentLength ? current[end2] : (byte) '\t');
                if (c1 == c2) {
                    if (c1 == '\t') // at end of a common block
                    {
                        result[length++] = '&';
                        break;
                    } else {
                        end1++;
                        end2++;
                    }
                } else // c1!=c2
                {
                    while (end1 < previousLength && previous[end1] != '\t') {
                        end1++;
                    }

                    while (end2 < currentLength && current[end2] != '\t') {
                        end2++;
                    }
                    for (int i = start2; i < end2; i++)
                        result[length++] = current[i];
                    break;
                }
            }
            start1 = end1 + 1;
            start2 = end2 + 1;
            if (start2 < currentLength)
                result[length++] = '\t';


        }
        return length;
    }

    /**
     * inflate the current SAM line by replacing all & by the corresponding field in the previous SAM line
     *
     * @param previous
     * @param current
     * @return inflated SAM line
     */
    public static int inflate(byte[] previous, int previousLength, byte[] current, int currentLength, byte[] result) {
        if (previous == null) {
            System.arraycopy(current, 0, result, 0, currentLength);
            return currentLength;
        }

        int length = 0;

        int start1 = 0;
        int start2 = 0;
        while (start1 < previousLength && start2 < currentLength) {
            int end1 = start1;
            while (end1 < previousLength && previous[end1] != '\t') {
                end1++;
            }
            int end2 = start2;
            while (end2 < currentLength && current[end2] != '\t') {
                end2++;
            }
            if (current[start2] == '&' && end2 == start2 + 1) {
                for (int i = start1; i < end1; i++)
                    result[length++] = previous[i];
            } else {
                for (int i = start2; i < end2; i++)
                    result[length++] = current[i];
            }
            if (end2 < currentLength)
                result[length++] = '\t';
            start1 = end1 + 1;
            start2 = end2 + 1;
        }
        return length;
    }

    /**
     * deflate current SAM line by replacing all fields that equalOverShorterOfBoth previous line by an ampersand
     *
     * @param previous
     * @param current
     * @return deflated SAM line
     */
    public static String deflate(String previous, String current) {
        if (previous == null)
            return current;

        StringBuilder buf = new StringBuilder();

        int start1 = 0;
        int start2 = 0;
        while (start1 < previous.length() && start2 < current.length()) {
            int end1 = start1;
            int end2 = start2;

            while (end1 <= previous.length() && end2 <= current.length()) {
                int c1 = (end1 < previous.length() ? previous.charAt(end1) : '\t');
                int c2 = (end2 < current.length() ? current.charAt(end2) : '\t');
                if (c1 == c2) {
                    if (c1 == '\t') // at end of a common block
                    {
                        buf.append("&");
                        break;
                    } else {
                        end1++;
                        end2++;
                    }
                } else // c1!=c2
                {
                    while (end1 < previous.length() && previous.charAt(end1) != '\t') {
                        end1++;
                    }

                    while (end2 < current.length() && current.charAt(end2) != '\t') {
                        end2++;
                    }
                    for (int i = start2; i < end2; i++)
                        buf.append(current.charAt(i));
                    break;
                }
            }
            start1 = end1 + 1;
            start2 = end2 + 1;
            if (start2 < current.length())
                buf.append("\t");
        }
        return buf.toString();
    }

    /**
     * inflate the current SAM line by replacing all & by the corresponding field in the previous SAM line
     *
     * @param previous
     * @param current
     * @return inflated SAM line
     */
    public static String inflate(String previous, String current) {
        if (previous == null)
            return current;

        StringBuilder buf = new StringBuilder();

        int start1 = 0;
        int start2 = 0;
        while (start1 < previous.length() && start2 < current.length()) {
            int end1 = start1;
            while (end1 < previous.length() && previous.charAt(end1) != '\t') {
                end1++;
            }
            int end2 = start2;
            while (end2 < current.length() && current.charAt(end2) != '\t') {
                end2++;
            }
            if (current.charAt(start2) == '&' && end2 == start2 + 1) {
                for (int i = start1; i < end1; i++)
                    buf.append(previous.charAt(i));
            } else {
                for (int i = start2; i < end2; i++)
                    buf.append(current.charAt(i));
            }
            if (end2 < current.length())
                buf.append("\t");
            start1 = end1 + 1;
            start2 = end2 + 1;
        }
        return buf.toString();
    }
}
