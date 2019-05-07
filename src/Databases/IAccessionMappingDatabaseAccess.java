package Databases;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Collection;

/**
 * interface for accessing accession mapping values
 * Daniel Huson, 3.2019
 */
public interface IAccessionMappingDatabaseAccess {
    /**
     * each classification has values that are either STRING or integers
     */
    enum ValueType {STRING, integers}

    /**
     * set the database URL
     *
     * @param url
     */
    void openDatabase(String url) throws FileNotFoundException, ClassNotFoundException, SQLException;

    /**
     * get the info string associated with the database
     *
     * @return
     */
    String getInfo() throws SQLException, ClassNotFoundException;

    /**
     * get the number of accessions in the database
     *
     * @return
     */
    int getSize() throws SQLException, ClassNotFoundException;

    /**
     * get the list of classification names
     *
     * @return
     */
    Collection<String> getClassificationNames() throws ClassNotFoundException;

    /**
     * get the index (column) for a classification. In the methods below, we will reference classifications by their index
     *
     * @param classificationName
     * @return
     */
    int getClassificationIndex(String classificationName);

    /**
     * get the type for a given classification index
     *
     * @param classificationIndex
     * @return
     */
    ValueType getType(int classificationIndex);

    /**
     * get the size for a given classification index
     *
     * @param classificationIndex
     * @return
     */
    int getSize(int classificationIndex) throws ClassNotFoundException, SQLException;

    /**
     * get the info string for a given classification index
     *
     * @param classificationIndex
     * @return
     */
    String getInfo(int classificationIndex) throws ClassNotFoundException, SQLException;

    /**
     * get the value for a classification of type integers
     *
     * @param classificationIndex
     * @param accession
     * @return
     */
    int getValueInt(int classificationIndex, String accession);

    /**
     * get the value for a classification of type STRING
     *
     * @param classificationIndex
     * @param accession
     * @return
     */
    String getValueString(int classificationIndex, String accession);
}