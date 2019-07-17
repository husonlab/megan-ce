package Databases;

import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.classification.data.IString2IntegerMap;
import megan.fx.NotificationsInSwing;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * adapts database accession mapping
 * Daniel Huson, 5.2019
 */
public class Accession2IdAdapter {
    public final static boolean DATABASE_USES_WRONG_NAMES = true;

    private AccessionMappingDatabaseAccess accessionMappingDatabaseAccess;
    private File file;

    private static Accession2IdAdapter instance;

    private Accession2IdAdapter() {
    }

    public static Accession2IdAdapter getInstance() {
        if (instance == null) {
            instance = new Accession2IdAdapter();
        }
        return instance;
    }

    public void setup(String fileName) {
        close();
        if (fileName != null && fileName.length() > 0)
            try {
                accessionMappingDatabaseAccess = new AccessionMappingDatabaseAccess(fileName);
                ProgramProperties.put("AccessionMappingDatabase", fileName);
                System.err.println("Supported classifications: " + Basic.toString(accessionMappingDatabaseAccess.getClassificationNames(), " "));
                this.file = new File(fileName);
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

    public Collection<String> getClassificationNames() {
        try {
            if (isSetup()) {
                return accessionMappingDatabaseAccess.getClassificationNames();
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return new ArrayList<>();
    }

    /**
     * creates an accession to class id mapping
     *
     * @param classificationName
     * @return mapping
     */
    public IString2IntegerMap createMap(String classificationName) {

        if (DATABASE_USES_WRONG_NAMES) {
            // todo: need to change database to use uppercase names:
            classificationName = classificationName.toLowerCase();
            if (classificationName.endsWith("taxonomy"))
                classificationName = "protacc";
        }

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
        file = null;
    }

    public boolean isDBFile(String fileName) {
        if (!fileName.endsWith(".db"))
            return false;

        if (this.file != null && (new File(fileName)).equals(this.file))
            return true;
        try {
            final AccessionMappingDatabaseAccess accessionMappingDatabaseAccess = new AccessionMappingDatabaseAccess(fileName);
            accessionMappingDatabaseAccess.closeDB();
        } catch (Exception ex) {
            return false;
        }
        return true;

    }
}
