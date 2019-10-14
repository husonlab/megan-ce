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

import jloda.util.Pair;
import megan.io.IOutputWriter;

import java.io.IOException;
import java.util.*;

/**
 * Used to define the format of a binary data block in an RMA3 file
 * Created by huson on 5/13/14.
 */
public class FormatDefinition {
    public enum Type {
        Integer, Long, Float, String, Character, Byte;

        static Type getType(Object object) throws IOException {
            if (object instanceof java.lang.Integer) return Integer;
            if (object instanceof java.lang.Long) return Long;
            if (object instanceof java.lang.Float) return Float;
            if (object instanceof java.lang.String) return String;
            if (object instanceof java.lang.Character) return Character;
            if (object instanceof java.lang.Byte) return Byte;
            throw new IOException("Unknown type: " + object.getClass().toString());

        }
    }

    private final List<Pair<String, Type>> list = new LinkedList<>();
    private final Map<String, Pair<String, Type>> map = new HashMap<>();

    private Iterator<Pair<String, Type>> writerIterator = null;

    private void addItem(String word, Type type) {
        Pair<String, Type> pair = new Pair<>(word, type);
        list.add(pair);
        map.put(word, pair);
    }

    public void removeItem(String word) {
        Pair<String, Type> pair = map.get(word);
        if (pair != null) {
            list.remove(pair);
            map.keySet().remove(word);
        }
    }

    public Type getItemType(String word) {
        Pair<String, Type> pair = map.get(word);
        if (pair != null)
            return pair.getSecond();
        else
            return null;
    }

    public boolean hasItem(String word) {
        return map.containsKey(word);
    }

    public void clear() {
        list.clear();
        map.clear();
    }

    public Iterator<Pair<String, Type>> iterator() {
        return list.iterator();
    }

    public List<Pair<String, Type>> getList() {
        return list;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        boolean first = true;
        for (Pair<String, Type> pair : list) {
            if (first)
                first = false;
            else
                buf.append(" ");
            buf.append(pair.get1()).append(":").append(pair.get2().toString());
        }
        return buf.toString();
    }

    /**
     * create a format definition from a defining string
     *
     * @param string
     * @return format definition
     */
    public static FormatDefinition fromString(String string) {
        FormatDefinition formatDefinition = new FormatDefinition();
        String[] tokens = string.split(" ");
        for (String word : tokens) {
            int pos = word.indexOf(":");
            formatDefinition.addItem(word.substring(0, pos), Type.valueOf(word.substring(pos + 1)));
        }
        return formatDefinition;
    }

    /**
     * call this when starting to write formatted output
     */
    public void startWrite() {
        writerIterator = null;
    }

    /**
     * write an item to the output writer. Throws an exception of doesn't make the
     * defined format
     *
     * @param outputWriter
     * @param label
     * @param value
     */
    public void write(IOutputWriter outputWriter, String label, Object value) throws IOException {
        if (writerIterator == null)
            writerIterator = list.iterator();
        Pair<String, Type> current = writerIterator.next();
        if (current == null || !label.equals(current.getFirst()))
            throw new IOException("write(): Unexpected label: " + label);
        switch (Type.getType(value)) {
            case Integer:
                outputWriter.writeInt((Integer) value);
                break;
            case Long:
                outputWriter.writeLong((Long) value);
                break;
            case Float:
                outputWriter.writeFloat((Float) value);
                break;
            case String:
                outputWriter.writeStringNoCompression((String) value);
                break;
            case Byte:
                outputWriter.write((Byte) value);
                break;
            case Character:
                outputWriter.writeChar((Character) value);
                break;
            default:
                throw new IOException("Invalid type: " + value);
        }
    }

    public void finishWrite() throws IOException {
        if (writerIterator == null)
            throw new IOException("finishWrite(): nothing written");
        if (writerIterator.hasNext())
            throw new IOException("finishWrite(): not finished");
        writerIterator = null;
    }

    public static void main(String[] args) {

        String test = "taxon:Integer seed:Integer kegg:Integer bitScore:Float name:String";

        FormatDefinition def = FormatDefinition.fromString(test);

        System.err.println("Got: " + def.toString());
    }
}
