package Databases;

import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.classification.data.IString2IntegerMap;
import megan.fx.NotificationsInSwing;

import javax.swing.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * adapts database accession mapping
 * Daniel Huson, 5.2019
 */
public class Accession2IdAdapter {
    private AccessionMappingDatabaseAccess accessionMappingDatabaseAccess;

    private static Accession2IdAdapter instance;

    private Accession2IdAdapter() {
    }

    public static Accession2IdAdapter getInstance() {
        if (instance == null) {
            instance = new Accession2IdAdapter();
        }
        return instance;
    }

    public void setup() {
        if (ProgramProperties.get("enable-database-lookup", false) && !Accession2IdAdapter.getInstance().isSetup()) {
            final String fileName = JOptionPane.showInputDialog(null, "Enter database file name:", ProgramProperties.get("AccessionMappingDatabase", ""));
            setup(fileName);
        } else {
            ProgramProperties.remove("AccessionMappingDatabase");
            setup(null);
        }
    }

    public void setup(String fileName) {
        close();
        if (fileName != null && fileName.length() > 0)
            try {
                accessionMappingDatabaseAccess = new AccessionMappingDatabaseAccess(fileName);
                ProgramProperties.put("AccessionMappingDatabase", fileName);
                System.err.println("Supported classifications: " + Basic.toString(accessionMappingDatabaseAccess.getClassificationNames(), " "));
            } catch (Exception e) {
                Basic.caught(e);
                NotificationsInSwing.showError("Failed to open database file " + fileName + ": " + e.getMessage());
            }
    }

    public boolean isSetup() {
        return accessionMappingDatabaseAccess != null;
    }

    public boolean hasClassification(String classificationName) {
        try {
            if (isSetup()) {
                if (accessionMappingDatabaseAccess.getClassificationNames().contains(classificationName))
                    return true;
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return false;
    }

    /**
     * creates an accession to class id mapping
     *
     * @param classificationName
     * @return mapping
     */
    public IString2IntegerMap createMap(String classificationName) {
        System.err.println("Classification '" + classificationName + "' uses database file " + ProgramProperties.get("AccessionMappingDatabase", ""));
        final int classificationIndex = accessionMappingDatabaseAccess.getClassificationIndex(classificationName);
        return new IString2IntegerMap() {
            @Override
            public int get(String accession) {
                try {
                    return accessionMappingDatabaseAccess.getValueInt(classificationIndex, accession);
                } catch (Exception e) {
                    return 0;
                }
            }

            @Override
            public int size() {
                try {
                    return accessionMappingDatabaseAccess.getSize(classificationIndex);
                } catch (Exception e) {
                    Basic.caught(e);
                    return 0;
                }
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    public void close() {
        if (accessionMappingDatabaseAccess != null) {
            try {
                accessionMappingDatabaseAccess.closeDB();
            } catch (SQLException e) {
                Basic.caught(e);
            }
            accessionMappingDatabaseAccess = null;
        }
    }
}
