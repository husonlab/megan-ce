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
package megan.main;

import jloda.swing.export.ExportImageDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.PropertiesListListener;
import megan.util.ReadMagnitudeParser;

import java.awt.*;
import java.io.File;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * manages megan properties, in cooperation with jloda.director.Properties
 *
 * @author huson Date: 11-Nov-2004
 */
public class MeganProperties {
    // property names
    public static final String MEGANFILE = "OpenFile";
    public static final String SAVEFILE = "SaveFile";
    public static final String TAXONOMYFILE = "TaxonomyFile";
    public static final String CONTAMINANT_FILE = "ContaminantFile";

    public static final String MICROBIALATTRIBUTESFILE = "MicrobialAttributesFiles";
    private static final String MAPPINGFILE = "MappingFile";

    public static final String TAXONOMY_SYNONYMS_FILE = "TaxonomySynonymsMapFile";

    public static final String PARSE_TAXON_NAMES = "UseParseTextTaxonomy";

    private static final String EXPORTFILE = "ExportFile";
    public static final String RECENTFILES = "RecentFiles";
    private static final String MAXRECENTFILES = "MaxRecentFiles";
    public static final String EXTRACT_OUTFILE_DIR = "ExtractOutFileDir";
    public static final String EXTRACT_OUTFILE_TEMPLATE = "ExtractOutFileTemplate";

    public static final String FINDREAD = "FindRead";
    public static final String FINDTAXON = "FindTaxon";

    public static final String DEFAULT_PROPERTIES = "DefaultParameters";

    public static final String BLASTFILE = "BlastFile";
    public static final String READSFILE = "ReadsFile";
    public static final String CSVFILE = "CSVFile";
    public static final String BIOMFILE = "BIOMFile";
    private static final String BLASTOUTFILE = "BlastOutFile";

    public static final String COMPARISON_STYLE = "CompareStyle";

    public static final String DISABLED_TAXA = "DisabledTaxa";
    public static final int[] DISABLED_TAXA_DEFAULT = {32644, 37965, 134367, 2323, 28384, 61964, 48510, 47936, 186616, 12908, 48479, 156614, 367897};

    public static final String PVALUE_COLOR = "PValueColor";

    public static final String INSPECTOR_WINDOW_GEOMETRY = "InspectorWindowGeometry";

    public static final String CHART_WINDOW_GEOMETRY = "ChartWindowGeometry";
    public static final String TAXA_CHART_WINDOW_GEOMETRY = "TaxaChartWindowGeometry";

    public static final String NETWORK_DIRECTORY = "NetworkDirectory";

    public static final String PAIRED_READ_SUFFIX1 = "PairedRead1";
    public static final String PAIRED_READ_SUFFIX2 = "PairedRead2";

    public static final String MININUM_READS_IN_ALIGNMENT = "MinReadsInAlignment";

    public static final String DEFAULT_TAXONOMYFILE = "ncbi.tre";
    private static final String DEFAULT_MAPPINGFILE = "ncbi.map";
    private static final String DEFAULT_MICROBIALATTRIBUTESFILE = "microbialattributes.map";
    
    /**
     * sets the program properties
     *
     * @param propertiesFile
     */
    public static void initializeProperties(String propertiesFile) {
        ProgramProperties.setPropertiesFileName(propertiesFile);
        ProgramProperties.setProgramIcons(ResourceManager.getIcons("megan16.gif", "megan32.gif", "megan48.gif", "megan64.gif", "megan128.gif"));

        // first set all necessary defaults:
        ProgramProperties.put(MEGANFILE, System.getProperty("user.dir"));
        ProgramProperties.put(SAVEFILE, System.getProperty("user.dir"));
        ProgramProperties.put(EXPORTFILE, System.getProperty("user.dir"));

        ProgramProperties.put(TAXONOMYFILE, DEFAULT_TAXONOMYFILE);
        ProgramProperties.put(MAPPINGFILE, DEFAULT_MAPPINGFILE);
        ProgramProperties.put(MICROBIALATTRIBUTESFILE, DEFAULT_MICROBIALATTRIBUTESFILE);

        ProgramProperties.put(BLASTFILE, "");
        ProgramProperties.put(BLASTOUTFILE, "");

        ProgramProperties.put(PVALUE_COLOR, Color.YELLOW);

        ProgramProperties.put(RECENTFILES, "");
        ProgramProperties.put(MAXRECENTFILES, 30);
        ProgramProperties.put(ExportImageDialog.GRAPHICSFORMAT, ".pdf");
        ProgramProperties.put(ExportImageDialog.GRAPHICSDIR, System.getProperty("user.dir"));

        ProgramProperties.put(DISABLED_TAXA, DISABLED_TAXA_DEFAULT);

        // then read in file to override defaults:
        ProgramProperties.load(propertiesFile);

        if (!ProgramProperties.get("Version", "").equals(ProgramProperties.getProgramName())) {
            // System.err.println("malt.Version has changed, resetting path to initialization files");
            // malt.Version has changed, reset paths to taxonomy
            ProgramProperties.put("Version", ProgramProperties.getProgramName());
            // make sure we find the initialization files:
            ProgramProperties.put(TAXONOMYFILE, DEFAULT_TAXONOMYFILE);
            ProgramProperties.put(MAPPINGFILE, DEFAULT_MAPPINGFILE);
            ProgramProperties.put(MICROBIALATTRIBUTESFILE, DEFAULT_MICROBIALATTRIBUTESFILE);
        }

        ProgramProperties.put(MeganProperties.DEFAULT_PROPERTIES, "");

        ReadMagnitudeParser.setEnabled(ProgramProperties.get("allow-read-weights", false));
        ReadMagnitudeParser.setUnderScoreEnabled(ProgramProperties.get("allow-read-weights-underscore", false));
    }

    /**
     * add a file to the recent files list
     *
     * @param file c
     */
    public static void addRecentFile(File file) {
        addRecentFile(file.getPath());
    }

    /**
     * add a file to the recent files list
     *
     * @param pathName
     */
    public static void addRecentFile(String pathName) {
        int maxRecentFiles = ProgramProperties.get(MAXRECENTFILES, 20);
        StringTokenizer st = new StringTokenizer(ProgramProperties.get(RECENTFILES, ""), ";");
        int count = 1;
        java.util.List<String> recentFiles = new LinkedList<>();
        recentFiles.add(pathName);
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            if (!pathName.equals(next)) {
                recentFiles.add(next);
                if (++count == maxRecentFiles)
                    break;
            }
        }
        StringBuilder buf = new StringBuilder();
        for (String recentFile : recentFiles) buf.append(recentFile).append(";");
        ProgramProperties.put(RECENTFILES, buf.toString());
        notifyListChange(RECENTFILES);
    }

    /**
     * clears the list of recent files
     */
    public static void clearRecentFiles() {
        String str = ProgramProperties.get(RECENTFILES, "");
        if (str.length() != 0) {
            ProgramProperties.put(RECENTFILES, "");
            notifyListChange(RECENTFILES);
        }
    }

    private static final java.util.List<PropertiesListListener> propertieslistListeners = new LinkedList<>();

    /**
     * notify listeners that list of values for the given name has changed
     *
     * @param name such as RecentFiles
     */
    public static void notifyListChange(String name) {
        java.util.List<String> list = new LinkedList<>();
        StringTokenizer st = new StringTokenizer(ProgramProperties.get(name, ""), ";");
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        synchronized (propertieslistListeners) {
            for (PropertiesListListener listener : propertieslistListeners) {
                if (listener.isInterested(name))
                    listener.hasChanged(list);
            }
        }
    }

    /**
     * add recent file listener
     *
     * @param listener
     */
    public static void addPropertiesListListener(PropertiesListListener listener) {
        if (!propertieslistListeners.contains(listener)) {
            synchronized (propertieslistListeners) {
                propertieslistListeners.add(listener);
            }
        }
    }

    /**
     * remove recent file listener
     *
     * @param listener
     */
    public static void removePropertiesListListener(PropertiesListListener listener) {
        synchronized (propertieslistListeners) {
            propertieslistListeners.remove(listener);
        }
    }
}
