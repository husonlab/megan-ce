/*
 * GeneItem.java
 * Copyright (C) 2019 Daniel H. Huson
 * <p>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.genes;

import jloda.util.Basic;
import jloda.util.interval.Interval;
import megan.io.InputReader;
import megan.io.OutputWriter;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * a gene item
 * Daniel Huson, 11.2017
 */
public class GeneItem {
    private byte[] proteinId;
    private final GeneItemCreator creator;
    private boolean reverse;

    private final int[] ids;

    GeneItem(GeneItemCreator creator) {
        this.creator = creator;
        ids = new int[creator.numberOfClassifications()];
    }

    private byte[] getProteinId() {
        return proteinId;
    }

    public void setProteinId(byte[] proteinId) throws IOException {
        this.proteinId = proteinId;
        creator.map(Basic.toString(proteinId), ids);
    }

    public int getId(String classificationName) {
        return getId(creator.rank(classificationName));
    }

    private int getId(Integer rank) {
        return rank == null ? 0 : ids[rank];
    }

    public void setId(String classificationName, int id) {
        ids[creator.rank(classificationName)] = id;
    }

    public void setId(int rank, int id) {
        ids[rank] = id;
    }

    private boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder("proteinId=" + (proteinId == null ? "null" : Basic.toString(proteinId)));
        for (int i = 0; i < creator.numberOfClassifications(); i++) {
            buf.append(", ").append(creator.classification(i)).append("=").append(ids[i]);
        }
        buf.append(", reverse=").append(reverse);

        return buf.toString();
    }

    /**
     * write
     *
     * @param outs
     * @throws java.io.IOException
     */
    public void write(OutputWriter outs) throws IOException {
        if (proteinId == null || proteinId.length == 0)
            outs.writeInt(0);
        else {
            outs.writeInt(proteinId.length);
            outs.write(proteinId);
        }
        for (int i = 0; i < creator.numberOfClassifications(); i++) {
            outs.writeInt(ids[i]);
        }
        outs.write(reverse ? 1 : 0);
    }

    /**
     * read
     *
     * @param ins
     * @throws IOException
     */
    public void read(RandomAccessFile ins) throws IOException {
        int length = ins.readInt();
        if (length == 0)
            proteinId = null;
        else {
            proteinId = new byte[length];
            if (ins.read(proteinId, 0, length) != length)
                throw new IOException("read failed");
        }
        for (int i = 0; i < creator.numberOfClassifications(); i++) {
            ids[i] = ins.readInt();
        }
        reverse = (ins.read() == 1);
    }

    /**
     * read
     *
     * @param ins
     * @throws IOException
     */
    public void read(InputReader ins) throws IOException {
        int length = ins.readInt();
        if (length == 0)
            proteinId = null;
        else {
            proteinId = new byte[length];
            if (ins.read(proteinId, 0, length) != length)
                throw new IOException("read failed");
        }
        for (int i = 0; i < creator.numberOfClassifications(); i++) {
            ids[i] = ins.readInt();
        }
        reverse = (ins.read() == 1);
    }

    /**
     * get the annotation string
     *
     * @param refInterval
     * @return annotation string
     */
    public String getAnnotation(Interval<GeneItem> refInterval) {
        final StringBuilder buf = new StringBuilder();
        buf.append("pos|").append(isReverse() ? refInterval.getEnd() + ".." + refInterval.getStart() : refInterval.getStart() + ".." + refInterval.getEnd());
        buf.append("|ref|").append(Basic.toString(getProteinId()));
        for (int i = 0; i < creator.numberOfClassifications(); i++) {
            if (getId(i) > 0)
                buf.append("|").append(creator.getShortTag(i)).append(getId(i));
        }
        return buf.toString();
    }
}
