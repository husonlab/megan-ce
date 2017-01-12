/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.util;

import jloda.util.Triplet;

import java.util.*;

/**
 * a two dimensional table, similar to Guave Table
 * Daniel Huson, 12.2012
 */
public class Table<R, C, V> {
    private final Map<R, Map<C, V>> dataMap = new HashMap<>();

    /**
     * constructor
     */
    public Table() {
    }

    /**
     * does table contain cell
     *
     * @param rowKey
     * @param columnKey
     */
    public boolean contains(R rowKey, C columnKey) {
        if (rowKey == null || columnKey == null)
            return false;
        Map<C, V> row = dataMap.get(rowKey);
        return row != null && row.containsKey(columnKey);
    }

    /**
     * row contained?
     *
     * @param rowKey
     * @return
     */
    public boolean containsRow(R rowKey) {
        return dataMap.containsKey(rowKey);
    }

    /**
     * column contained?
     *
     * @param columnKey
     * @return
     */
    public boolean containsColumn(C columnKey) {
        if (columnKey == null)
            return false;
        for (Map<C, V> row : dataMap.values()) {
            if (row.containsKey(columnKey))
                return true;
        }
        return false;
    }

    /**
     * does table contain given value
     *
     * @param value
     * @return
     */
    public boolean containsValue(V value) {
        if (value == null)
            return false;
        for (Map<C, V> row : dataMap.values()) {
            if (row.containsValue(value))
                return true;
        }
        return false;

    }

    /**
     * get the value or null
     *
     * @param rowKey
     * @param columnKey
     * @return
     */
    public V get(R rowKey, C columnKey) {
        if (rowKey == null || columnKey == null)
            return null;
        Map<C, V> row = dataMap.get(rowKey);
        if (row == null)
            return null;
        return row.get(columnKey);
    }

    /**
     * is table empty?
     *
     * @return
     */
    public boolean isEmpty() {
        return dataMap.isEmpty();
    }

    /**
     * get the size of the table
     *
     * @return
     */
    public int size() {
        int size = 0;
        for (Map<C, V> row : dataMap.values()) {
            size += row.size();
        }
        return size;
    }

    /**
     * compares equality
     * todo: this needs to be fixed
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        return (obj instanceof Table) && dataMap.equals(obj);
    }

    /**
     * Returns the hash code for this table. The hash code of a table is defined
     * as the hash code of its cell view, as returned by {@link #cellSet}.
     */
    public int hashCode() {
        return dataMap.hashCode();
    }

    // Mutators

    /**
     * erase
     */
    public void clear() {
        dataMap.clear();
    }

    /**
     * put the value for a cell
     *
     * @param rowKey
     * @param columnKey
     * @param value
     * @return old value, if present, otherwise new value
     */
    public V put(R rowKey, C columnKey, V value) {
        if (rowKey != null && columnKey != null) {
            Map<C, V> row = row(rowKey);
            if (row == null) {
                row = new HashMap<>();
                dataMap.put(rowKey, row);
            }
            V oldValue = row.get(columnKey);
            row.put(columnKey, value);
            if (oldValue != null)
                return oldValue;
            else
                return value;
        }
        return null;
    }

    /**
     * put all values
     *
     * @param table
     */
    public void putAll(Table<R, C, V> table) {
        for (R rowKey : table.rowKeySet()) {
            Map<C, V> row = table.row(rowKey);
            for (C columnKey : row.keySet()) {
                put(rowKey, columnKey, row.get(columnKey));
            }
        }
    }

    /**
     * removes the given cell, returns the old value or null
     *
     * @param rowKey
     * @param columnKey
     * @return
     */
    public V remove(R rowKey, C columnKey) {
        if (rowKey == null || columnKey == null)
            return null;
        Map<C, V> row = row(rowKey);
        if (row == null)
            return null;
        V oldValue = get(rowKey, columnKey);
        row.remove(columnKey);
        return oldValue;
    }

    // Views

    /**
     * get a row. Changes to this set affect the table and vice versa
     *
     * @param rowKey
     * @return row or null
     */
    public Map<C, V> row(R rowKey) {
        if (rowKey == null)
            return null;
        return dataMap.get(rowKey);
    }

    /**
     * gets a column. Changes to this map do not affect the table
     *
     * @param columnKey
     * @return
     */
    public Map<R, V> column(final C columnKey) {
        HashMap<R, V> map = new HashMap<>();
        for (R rowKey : dataMap.keySet()) {
            Map<C, V> row = dataMap.get(rowKey);
            V value = row.get(columnKey);
            if (value != null)
                map.put(rowKey, value);
        }
        return map;
    }

    /**
     * get current set of all cells. Changes to this set do not affect the table
     *
     * @return
     */
    public Set<Triplet<R, C, V>> cellSet() {
        Set<Triplet<R, C, V>> set = new HashSet<>();
        for (R rowKey : dataMap.keySet()) {
            Map<C, V> row = dataMap.get(rowKey);
            for (C columnKey : row.keySet()) {
                set.add(new Triplet<>(rowKey, columnKey, row.get(columnKey)));
            }
        }
        return set;
    }

    /**
     * get the set of all row keys. Changes to this set affect the Table and vice versa
     *
     * @return
     */
    public Set<R> rowKeySet() {
        return dataMap.keySet();
    }

    /**
     * gets current column keys. Changes to this set do not affect the Table
     *
     * @return
     */
    public Set<C> columnKeySet() {
        Set<C> set = new HashSet<>();
        for (Map<C, V> row : dataMap.values()) {
            set.addAll(row.keySet());
        }
        return set;
    }

    /**
     * remove a given column
     * @param columnKey
     * @return true, if something removed
     */
    public boolean removeColumn(C columnKey) {
        boolean changed = false;
        for (R rowKey : dataMap.keySet()) {
            Map<C, V> row = dataMap.get(rowKey);
            if (row.containsKey(columnKey)) {
                row.remove(columnKey);
                row.keySet().remove(columnKey);
                changed = true;
            }
        }
        return true;
    }

    /**
     * gets all current values. Changes to this collection do not affect the Table
     *
     * @return values
     */
    public Collection<V> values() {
        Collection<V> values = new LinkedList<>();
        for (Map<C, V> row : dataMap.values()) {
            values.addAll(row.values());
        }
        return values;
    }

    /**
     * compute table with tranposed rows and cols
     */
    public Table<C, R, V> computeTransposedTable() {
        final Table<C, R, V> transposed = new Table<>();
        for (R row : rowKeySet())
            for (C col : columnKeySet())
                transposed.put(col, row, get(row, col));
        return transposed;
    }

    /**
     * returns a copy
     *
     * @return copy
     */
    public Table<R, C, V> copy() {
        final Table<R, C, V> copy = new Table<>();
        for (R row : rowKeySet())
            for (C col : columnKeySet())
                copy.put(row, col, get(row, col));
        return copy;
    }
}
