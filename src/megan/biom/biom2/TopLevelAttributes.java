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

package megan.biom.biom2;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import jloda.util.Basic;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * top level attributes of file in biom2.1 format
 * Daniel Huson, 9.2017
 */
public class TopLevelAttributes {
    public enum Type {
        OTU_table("OTU table"), Pathway_table("Pathway table"), Function_table("Function table"), Ortholog_table("Ortholog table"), Gene_table("Gene table"), Metabolite_table("Metabolite table"), Taxon_table("Taxon table");
        private final String name;

        Type(String s) {
            name = s;
        }

        boolean equalsName(String otherName) {
            return name.equals(otherName);
        }

        public String toString() {
            return this.name;
        }

        static Type valueOfName(String name) {
            for (Type value : Type.values())
                if (value.equalsName(name))
                    return value;
            return null;
        }
    }

    private String id;
    private String type;
    private String formatURL;
    private int[] formatVersion;
    private String generatedBy;
    private String creationDate;
    private int[] shape;
    private int nnz;

    /**
     * constructor
     */
    public TopLevelAttributes() {
    }

    /**
     * construct from reader
     *
     * @param reader
     * @throws IOException
     */
    public TopLevelAttributes(IHDF5Reader reader) throws IOException {
        read(reader);
    }

    /**
     * read top-level properties from biom2.1 file
     *
     * @param reader
     * @throws IOException
     */
    private void read(IHDF5Reader reader) throws IOException {
        try {
            id = reader.getStringAttribute("/", "id");
            type = reader.getStringAttribute("/", "type");
            formatURL = reader.getStringAttribute("/", "format-url");
            formatVersion = reader.getIntArrayAttribute("/", "format-version");
            generatedBy = reader.getStringAttribute("/", "generated-by");
            creationDate = reader.getStringAttribute("/", "creation-date");
            shape = reader.getIntArrayAttribute("/", "shape");
            nnz = reader.getIntAttribute("/", "nnz");
        } catch (Exception ex) {
            System.err.println("BIOM2 parser: Some required top-level attribute(s) missing.");
            throw new IOException(ex);
        }
        if (!isValidType(type))
            throw new IOException("Invalid type: " + type);

    }

    public void write(IHDF5Writer writer) throws IOException {
        throw new IOException("Not implemented");
    }

    public String toString() {
        return "id:            " + id + "\n" +
                "type:          " + type + "\n" +
                "formatURL:     " + formatURL + "\n" +
                "formatVersion: " + Basic.toString(formatVersion, ".") + "\n" +
                "generatedBy:   " + generatedBy + "\n" +
                "creationDate:  " + creationDate + "\n" +
                "shape:         " + Basic.toString(shape, ",") + "\n" +
                "nnz:           " + nnz + "\n";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormatURL() {
        return formatURL;
    }

    public void setFormatURL(String formatURL) {
        this.formatURL = formatURL;
    }

    public int[] getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int major, int minor) {
        this.formatVersion = new int[]{major, minor};
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public void setCreationDate(Date date) {
        this.creationDate = getISO8601StringForDate(date);
    }

    public int[] getShape() {
        return shape;
    }

    public void setShape(int[] shape) {
        this.shape = shape;
    }

    public int getNnz() {
        return nnz;
    }

    public void setNnz(int nnz) {
        this.nnz = nnz;
    }

    /**
     * Return an ISO 8601 combined date and time string for specified date/time
     *
     * @param date Date
     * @return String with format "yyyy-MM-dd'T'HH:mm:ss'Z'"
     */
    private static String getISO8601StringForDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    private static boolean isValidType(String type) {
        return Type.valueOfName(type) != null;
    }
}
