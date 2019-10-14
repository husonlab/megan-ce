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

/**
 * stores data required to perform LCA and functional analysis
 * Daniel Huson, 4.2015
 */
public class MatchLineRMA6 {
    private float bitScore;
    private float expected;
    private float percentIdentity;
    private final int[] fIds;
    private final int taxonomyIndex; // index of the fId that contains the taxon index

    /**
     * constructor
     *
     * @param numberOfFNames
     */
    public MatchLineRMA6(int numberOfFNames, int taxonomyIndex) {
        fIds = new int[numberOfFNames];
        this.taxonomyIndex = taxonomyIndex;
    }

    /**
     * parse SAM match that starts at given offset (ignoring any further matches that come after a newline)
     *
     * @param matchesText
     * @param offset
     */
    public void parse(byte[] matchesText, int offset) {
        bitScore = 0;
        expected = 0;
        percentIdentity = 0;

        int end = Utilities.nextNewLine(matchesText, offset);

        offset = skipTabs(matchesText, offset, 11);

        if (offset > -1) {
            String[] tokens = Utilities.split(matchesText, offset, end, (byte) '\t');
            for (String token : tokens) {
                if (token.startsWith("AS:i:"))
                    bitScore = Integer.parseInt(token.substring(5));
                else if (token.startsWith("ZE:f:"))
                    expected = Float.parseFloat(token.substring(5));
                else if (token.startsWith("ZI:i:"))
                    percentIdentity = Float.parseFloat(token.substring(5));
            }
        }
    }

    /**
     * skip a given count of tabs
     *
     * @param text
     * @param offset
     * @param n
     * @return position of n-th tab after offset
     */
    private static int skipTabs(byte[] text, int offset, int n) {
        while (n > 0) {
            if (offset == text.length)
                return -1;
            if (text[offset] == '\t')
                n--;
            offset++;
        }
        return offset - 1;
    }

    public float getBitScore() {
        return bitScore;
    }

    public void setBitScore(float bitScore) {
        this.bitScore = bitScore;
    }

    public float getExpected() {
        return expected;
    }

    public void setExpected(float expected) {
        this.expected = expected;
    }

    public float getPercentIdentity() {
        return percentIdentity;
    }

    public void setPercentIdentity(float percentIdentity) {
        this.percentIdentity = percentIdentity;
    }

    public int getTaxId() {
        return fIds[taxonomyIndex];
    }

    public void setTaxId(int taxId) {
        fIds[taxonomyIndex] = taxId;
    }

    public int getFId(int i) {
        return fIds[i];
    }

    public void setFId(int i, int id) {
        fIds[i] = id;
    }
}

