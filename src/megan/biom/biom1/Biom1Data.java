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
package megan.biom.biom1;

import com.google.gson.Gson;
import jloda.util.Basic;
import jloda.util.ProgramProperties;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * biom data
 * Daniel Huson, 7.2012
 */
public class Biom1Data {
    public enum AcceptableTypes {
        OTU_table("OTU table"),
        Pathway_table("Pathway table"),
        Function_table("Function table"),
        Ortholog_table("Ortholog table"),
        Gene_table("Gene table"),
        Metabolite_table("Metabolite table"),
        Taxon_table("Taxon table"),
        MEGAN_table("MEGAN table");

        private final String value;

        AcceptableTypes(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    public enum AcceptableMatrixTypes {sparse, dense}

    public enum AcceptableMatrixElementTypes {
        Int("int"), Float("float"), Unicode("unicode");
        private final String value;

        AcceptableMatrixElementTypes(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    /*
   Required files (Biom-format 1.0)
   id                  : <string or null> a field that can be used to id a table (or null)
format              : <string> The name and version of the current biom format
format_url          : <url> A string with a static URL providing format details
type                : <string> Table type (a controlled vocabulary)
                     Acceptable values:
                      "OTU table"
                      "Pathway table"
                      "Function table"
                      "Ortholog table"
                      "Gene table"
                      "Metabolite table"
                      "Taxon table"
generated_by        : <string> Package and revision that built the table
date                : <datetime> Date the table was built (ISO 8601 format)
rows                : <list of objects> An ORDERED list of obj describing the rows
                     (explained in detail below)
columns             : <list of objects> An ORDERED list of obj  describing the columns
                     (explained in detail below)
matrix_type         : <string> Type of matrix data representation (a controlled vocabulary)
                     Acceptable values:
                      "sparse" : only non-zero values are specified
                      "dense" : every element must be specified
matrix_element_type : Value type in matrix (a controlled vocabulary)
                     Acceptable values:
                      "int" : integer
                      "float" : floating point
                      "unicode" : unicode string
shape               : <list of ints>, the number of rows and number of columns in data
data                : <list of lists>, counts of observations by sample
                      if matrix_type is "sparse", [[row, column, value],
                                                   [row, column, value],
                                                   ...]
                      if matrix_type is "dense",  [[value, value, value, ...],
                                                   [value, value, value, ...],
                                                   ...]
    */

    private String comment;
    private String classification;
    private String id;
    private String format;
    private String format_url;
    private String type;
    public String generated_by;
    private String date;
    // public Date date;    // todo: should be date
    private Map[] rows;
    private Map[] columns;
    private String matrix_type;
    private String matrix_element_type;
    private int[] shape;
    private float[][] data;

    /**
     * default constructor
     */
    public Biom1Data() {
    }

    /**
     * constructor to be used when generating new biome files
     *
     * @param id
     */
    public Biom1Data(String id) {
        this.id = id;
        format = "Biological Observation Matrix 1.0.0";
        format_url = "http://biom-format.org";
        generated_by = ProgramProperties.getProgramName();
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        df.setTimeZone(tz);
        date = df.format(new Date());
    }

    /**
     * check whether data is acceptable
     *
     * @throws IOException
     */
    private void check() throws IOException {
        boolean ok = false;
        if (type != null) {
            for (AcceptableTypes acceptable : AcceptableTypes.values()) {
                if (acceptable.toString().equalsIgnoreCase(type)) {
                    ok = true;
                    break;
                }
            }
        }
        if (!ok)
            throw new IOException("type=" + type + ", must be one of: " + Basic.toString(AcceptableTypes.values(), ", ").replaceAll("_", " "));

        ok = false;
        if (matrix_type != null) {
            for (AcceptableMatrixTypes acceptable : AcceptableMatrixTypes.values()) {
                if (acceptable.toString().equalsIgnoreCase(matrix_type)) {
                    ok = true;
                    break;
                }
            }
        }
        if (!ok)
            throw new IOException("matrix_type=" + matrix_type + ", must be one of: " + Basic.toString(AcceptableMatrixTypes.values(), ", "));

        ok = false;
        if (matrix_element_type != null) {
            for (AcceptableMatrixElementTypes acceptable : AcceptableMatrixElementTypes.values()) {
                if (acceptable.toString().equalsIgnoreCase(matrix_element_type)) {
                    ok = true;
                    break;
                }
            }
        }
        if (!ok)
            throw new IOException("matrix_element_type=" + matrix_element_type + ", must be one of: " + Basic.toString(AcceptableMatrixElementTypes.values(), ", "));
    }

    /**
     * read
     *
     * @param reader
     * @throws IOException
     */
    public static Biom1Data fromReader(Reader reader) throws IOException {
        Gson gson = new Gson();
        Biom1Data biom1Data = gson.fromJson(reader, Biom1Data.class);
        biom1Data.check();
        return biom1Data;
    }

    /**
     * write
     *
     * @param writer
     * @throws IOException
     */
    public void write(Writer writer) throws IOException {
        final Gson gson = new Gson();
        gson.toJson(this, writer);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormatUrl() {
        return format_url;
    }

    public void setFormatUrl(String url) {
        this.format_url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGenerated_by() {
        return generated_by;
    }

    public void setGenerated_by(String generated_by) {
        this.generated_by = generated_by;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Map[] getRows() {
        return rows;
    }

    public void setRows(Map[] rows) {
        this.rows = rows;
    }

    public Map[] getColumns() {
        return columns;
    }

    public void setColumns(Map[] columns) {
        this.columns = columns;
    }

    public String[] getColumnIds() {
        String[] ids = new String[getColumns().length];
        for (int i = 0; i < getColumns().length; i++) {
            ids[i] = (String) columns[i].get("id");
        }
        return ids;
    }

    public String getMatrix_type() {
        return matrix_type;
    }

    public void setMatrix_type(String matrix_type) {
        this.matrix_type = matrix_type;
    }

    public String getMatrix_element_type() {
        return matrix_element_type;
    }

    public void setMatrix_element_type(String matrix_element_type) {
        this.matrix_element_type = matrix_element_type;
    }

    public int[] getShape() {
        return shape;
    }

    public float[][] getData() {
        return data;
    }

    public void setShape(int[] shape) {
        this.shape = shape;
    }

    public void setData(float[][] data) {
        this.data = data;
    }

    public boolean isTaxonomyData() {
        return AcceptableTypes.Taxon_table.toString().equalsIgnoreCase(type);
    }

    public boolean isOTUData() {
        return AcceptableTypes.OTU_table.toString().equalsIgnoreCase(type);
    }

    public boolean isKEGGData() {
        if (!AcceptableTypes.Function_table.toString().equalsIgnoreCase(type))
            return false;
        return comment != null && comment.contains("KEGG");
    }

    public boolean isSEEDData() {
        if (!AcceptableTypes.Function_table.toString().equalsIgnoreCase(type))
            return false;
        if (rows.length == 0)
            return false;
        return comment != null && comment.contains("SEED");
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }
}
