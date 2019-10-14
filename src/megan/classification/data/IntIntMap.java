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

import megan.classification.util.Tools;

import java.io.*;

/**
 * optimized int to int map, see http://java-performance.info/implementing-world-fastest-java-int-to-int-hash-map/
 */
public class IntIntMap {
    private static final int NO_VALUE = 0;
    private static final int FREE_KEY = 0;

    private static final int MAGIC_NUMBER = 123456789;

    /**
     * Do we have 'free' key in the map?
     */
    private boolean m_hasFreeKey;
    /**
     * Value of 'free' key
     */
    private int m_freeValue;

    /**
     * Fill factor, must be between (0 and 1)
     */
    private final float m_fillFactor;
    /**
     * We will resize a map once it reaches this size
     */
    private int m_threshold;
    /**
     * Current map size
     */
    private int m_size;

    /**
     * Mask to calculate the original position
     */
    private int m_mask;
    private int m_mask2;

    /**
     * Keys and values
     */
    private int[] m_data;

    /**
     * constructor
     *
     * @param size
     * @param fillFactor
     */
    public IntIntMap(final int size, final float fillFactor) {
        if (fillFactor <= 0 || fillFactor >= 1)
            throw new IllegalArgumentException("FillFactor must be in (0, 1)");
        if (size <= 0)
            throw new IllegalArgumentException("Size must be positive!");
        final int capacity = Tools.arraySize(size, fillFactor);
        m_mask = capacity - 1;
        m_mask2 = capacity * 2 - 1;
        m_fillFactor = fillFactor;

        m_data = new int[capacity * 2];
        m_threshold = (int) (capacity * fillFactor);
    }

    /**
     * get value
     *
     * @param key
     * @return get value
     */
    public int get(final int key) {
        int ptr = (Tools.phiMix(key) & m_mask) << 1;

        if (key == FREE_KEY)
            return m_hasFreeKey ? m_freeValue : NO_VALUE;

        int k = m_data[ptr];

        if (k == FREE_KEY)
            return NO_VALUE;  //end of chain already
        if (k == key) //we check FREE prior to this call
            return m_data[ptr + 1];

        while (true) {
            ptr = (ptr + 2) & m_mask2; //that's next index
            k = m_data[ptr];
            if (k == FREE_KEY)
                return NO_VALUE;
            if (k == key)
                return m_data[ptr + 1];
        }
    }

    /**
     * put value
     *
     * @param key
     * @param value
     * @return
     */
    public int put(final int key, final int value) {
        if (key == FREE_KEY) {
            final int ret = m_freeValue;
            if (!m_hasFreeKey)
                ++m_size;
            m_hasFreeKey = true;
            m_freeValue = value;
            return ret;
        }

        int ptr = (Tools.phiMix(key) & m_mask) << 1;
        int k = m_data[ptr];
        if (k == FREE_KEY) //end of chain already
        {
            m_data[ptr] = key;
            m_data[ptr + 1] = value;
            if (m_size >= m_threshold)
                rehash(m_data.length * 2); //size is set inside
            else
                ++m_size;
            return NO_VALUE;
        } else if (k == key) //we check FREE prior to this call
        {
            final int ret = m_data[ptr + 1];
            m_data[ptr + 1] = value;
            return ret;
        }

        while (true) {
            ptr = (ptr + 2) & m_mask2; //that's next index calculation
            k = m_data[ptr];
            if (k == FREE_KEY) {
                m_data[ptr] = key;
                m_data[ptr + 1] = value;
                if (m_size >= m_threshold)
                    rehash(m_data.length * 2); //size is set inside
                else
                    ++m_size;
                return NO_VALUE;
            } else if (k == key) {
                final int ret = m_data[ptr + 1];
                m_data[ptr + 1] = value;
                return ret;
            }
        }
    }

    /**
     * remove a key
     *
     * @param key
     * @return
     */
    public int remove(final int key) {
        if (key == FREE_KEY) {
            if (!m_hasFreeKey)
                return NO_VALUE;
            m_hasFreeKey = false;
            --m_size;
            return m_freeValue; //value is not cleaned
        }

        int ptr = (Tools.phiMix(key) & m_mask) << 1;
        int k = m_data[ptr];
        if (k == key) //we check FREE prior to this call
        {
            final int res = m_data[ptr + 1];
            shiftKeys(ptr);
            --m_size;
            return res;
        } else if (k == FREE_KEY)
            return NO_VALUE;  //end of chain already
        while (true) {
            ptr = (ptr + 2) & m_mask2; //that's next index calculation
            k = m_data[ptr];
            if (k == key) {
                final int res = m_data[ptr + 1];
                shiftKeys(ptr);
                --m_size;
                return res;
            } else if (k == FREE_KEY)
                return NO_VALUE;
        }
    }

    /**
     * get size
     *
     * @return size
     */
    public int size() {
        return m_size;
    }

    private int shiftKeys(int pos) {
        // Shift entries with the same hash.
        int last, slot;
        int k;
        final int[] data = this.m_data;
        while (true) {
            pos = ((last = pos) + 2) & m_mask2;
            while (true) {
                if ((k = data[pos]) == FREE_KEY) {
                    data[last] = FREE_KEY;
                    return last;
                }
                slot = (Tools.phiMix(k) & m_mask) << 1; //calculate the starting slot for the current key
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 2) & m_mask2; //go to the next entry
            }
            data[last] = k;
            data[last + 1] = data[pos + 1];
        }
    }

    private void rehash(final int newCapacity) {
        m_threshold = (int) (newCapacity / 2 * m_fillFactor);
        m_mask = newCapacity / 2 - 1;
        m_mask2 = newCapacity - 1;

        final int oldCapacity = m_data.length;
        final int[] oldData = m_data;

        m_data = new int[newCapacity];
        m_size = m_hasFreeKey ? 1 : 0;

        for (int i = 0; i < oldCapacity; i += 2) {
            final int oldKey = oldData[i];
            if (oldKey != FREE_KEY)
                put(oldKey, oldData[i + 1]);
        }
    }

//    private int getStartIdx( final int key )
//    {
//        return ( Tools.phiMix( key ) & m_mask) << 1;
//    }

    /**
     * construct from input stream
     *
     * @param inputStream
     */
    public IntIntMap(InputStream inputStream) throws IOException {
        try (DataInputStream ins = new DataInputStream(inputStream)) {
            int magicNumber = ins.readInt();
            if (magicNumber != MAGIC_NUMBER)
                throw new IOException("Wrong file type");

            m_hasFreeKey = ins.readBoolean();
            m_freeValue = ins.readInt();
            m_fillFactor = ins.readFloat();
            m_threshold = ins.readInt();
            m_size = ins.readInt();

            m_mask = ins.readInt();
            m_mask2 = ins.readInt();

            final int m_data_length = ins.readInt();
            m_data = new int[m_data_length];
            for (int i = 0; i < m_data_length; i++)
                m_data[i] = ins.readInt();
        }
    }

    /**
     * save to file
     *
     * @param file
     * @throws IOException
     */
    public void save(File file, boolean append) throws IOException {
        try (DataOutputStream outs = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, append)))) {
            outs.writeInt(MAGIC_NUMBER);

            outs.writeBoolean(m_hasFreeKey);
            outs.writeInt(m_freeValue);
            outs.writeFloat(m_fillFactor);
            outs.writeInt(m_threshold);
            outs.writeInt(m_size);
            outs.writeInt(m_mask);
            outs.writeInt(m_mask2);

            outs.writeInt(m_data.length);
            for (int a : m_data)
                outs.writeInt(a);
        }
    }
}
