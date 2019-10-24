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
package megan.dialogs.export;

import jloda.util.Basic;
import jloda.util.ProgressListener;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.viewer.ClassificationViewer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * exports Data in CSV format
 * Daniel Huson, 5.2015
 */
public class CSVExporter {
    /**
     * constructor
     *
     * @param classificationName
     */
    public static List<String> getFormats(String classificationName, Document doc) {
        final List<String> formats = new LinkedList<>();

        final String shortName;
        if (classificationName.toLowerCase().equals("taxonomy"))
            shortName = "taxon";
        else
            shortName = classificationName.toLowerCase();

        formats.add(shortName + "Name_to_count");
        if (shortName.equals("taxon")) {
            formats.add(shortName + "Id_to_count");
            formats.add(shortName + "Rank_to_count");
        }
        formats.add(shortName + "Path_to_count");

        formats.add(shortName + "Name_to_percent");
        if (shortName.equals("taxon")) {
            formats.add(shortName + "Id_to_percent");
            formats.add(shortName + "Rank_to_percent");
        }
        formats.add(shortName + "Path_to_percent");

        formats.add(shortName + "Name_to_length");
        if (shortName.equals("taxon")) {
            formats.add(shortName + "Id_to_length");
            formats.add(shortName + "Rank_to_length");
        }
        formats.add(shortName + "Path_to_length");

        if (doc.getMeganFile().hasDataConnector()) {
            formats.add("readName_to_" + shortName + "Name");
            if (shortName.equals("taxon"))
                formats.add("readName_to_" + shortName + "Id");
            formats.add("readName_to_" + shortName + "Path");
            // the results of this feature are misleading and thus we have removed this:
            /*
            if (shortName.equals("taxon")) {
                formats.add("readName_to_" + shortName + "PathPercent");
            }

             */

            if (shortName.equals("taxon")) {
                formats.add("readName_to_taxonMatches");
                formats.add("readName_to_frameShiftsPerKb");
            }

            formats.add(shortName + "Name_to_readName");
            // formats.add("readName_to_gi");
            formats.add("readName_to_refSeqIds");
            formats.add("readName_to_headers");

            if (!shortName.equals("taxon")) {
                formats.add(shortName + "Name_to_RPK");
                formats.add(shortName + "Id_to_RPK");
                formats.add(shortName + "Path_to_RPK");
            }
            if (shortName.equals("taxon"))
                formats.add("readName_to_GC");
        }

        if (shortName.equals("taxon")) {
            formats.add("listTaxonNames");
            formats.add("listTaxonIds");

            if (doc.getMeganFile().hasDataConnector() && !doc.isLongReads()) { // not implemented for long reads
                try {
                    final String[] classificationNames = doc.getConnector().getAllClassificationNames();
                    Arrays.sort(classificationNames);

                    for (String name : classificationNames) {
                        name = name.toLowerCase();
                        formats.add(name + "Name_to_taxonName");
                        formats.add(name + "Id_to_taxonName");
                        formats.add(name + "Name_to_taxonId");
                        formats.add(name + "Id_to_taxonId");
                    }

                } catch (IOException e) {
                    Basic.caught(e);
                }
            }
        }

        return formats;
    }

    /**
     * apply the desired exporter
     *
     * @param dir
     * @param file
     * @param format
     */
    public static int apply(Director dir, ProgressListener progressListener, File file, String format, char separator, boolean reportSummarized) throws IOException {
        progressListener.setTasks("Export in CSV format", "Initializing");

        final Collection<String> cNames = ClassificationManager.getAllSupportedClassifications();
        int count = 0;

        if (format.equalsIgnoreCase("readName_to_GC"))
            count = CSVExportGCPercent.apply(dir.getMainViewer(), file, separator, progressListener);
        if (format.equalsIgnoreCase("taxonName_to_count") || format.equalsIgnoreCase("taxonId_to_count") || format.equalsIgnoreCase("taxonRank_to_count") || format.equalsIgnoreCase("taxonPath_to_count")) {
            count = CSVExportTaxonomy.exportTaxon2Counts(format, dir, file, separator, reportSummarized, progressListener);
        } else if (format.equalsIgnoreCase("taxonName_to_readName") || format.equalsIgnoreCase("taxonId_to_ReadName") || format.equalsIgnoreCase("taxonPath_to_readName")) {
            count = CSVExportTaxonomy.exportTaxon2ReadNames(format, dir, file, separator, progressListener);
        } else if (format.equalsIgnoreCase("taxonName_to_readId") || format.equalsIgnoreCase("taxonId_to_readId") || format.equalsIgnoreCase("taxonPath_to_readId")) {
            count = CSVExportTaxonomy.exportTaxon2ReadIds(format, dir, file, separator, progressListener);
        } else if (format.equalsIgnoreCase("taxonName_to_length") || format.equalsIgnoreCase("taxonId_to_length") || format.equalsIgnoreCase("taxonRank_to_length") || format.equalsIgnoreCase("taxonPath_to_length")) {
            count = CSVExportTaxonomy.exportTaxon2TotalLength(format, dir, file, separator, progressListener);
        } else if (format.equalsIgnoreCase("taxonName_to_percent") || format.equalsIgnoreCase("taxonId_to_percent") || format.equalsIgnoreCase("taxonRank_to_percent") || format.equalsIgnoreCase("taxonPath_to_percent")) {
            count = CSVExportFViewer.exportName2Percent(format, dir.getMainViewer(), file, separator, true, progressListener);
        } else if (format.equalsIgnoreCase("readName_to_taxonName") || format.equalsIgnoreCase("readName_to_taxonId") || format.equalsIgnoreCase("readName_to_taxonPath")) {
                // disabled: || format.equalsIgnoreCase("readName_to_taxonPathPercent")) {
            count = CSVExportTaxonomy.exportReadName2Taxon(format, dir, file, separator, progressListener);
        } else if (format.equalsIgnoreCase("readName_to_taxonMatches")) {
            count = CSVExportTaxonomy.exportReadName2Matches(format, dir, file, separator, progressListener);
        } else if (format.equalsIgnoreCase("readName_to_refSeqIds")) {
            count = CSVExportRefSeq.exportReadName2Accession(dir.getMainViewer(), file, separator, progressListener);
        } else if (format.equalsIgnoreCase("readName_to_headers")) {
            count = CSVExportHeaders.exportReadName2Headers(dir.getMainViewer(), file, separator, progressListener);
        } else if (format.equalsIgnoreCase("readName_to_frameShiftsPerKb")) {
            count = CSVExportFrameShiftsPerKb.apply(dir.getMainViewer(), file, separator, dir.getDocument().isLongReads(), progressListener);
        } else if (format.equalsIgnoreCase("reference_to_readName")) {
            count = CSVExportReference2Read.exportReference2ReadName(dir.getMainViewer(), file, separator, progressListener);
        } else if (Basic.endsWithIgnoreCase(format, "Name_to_count") || Basic.endsWithIgnoreCase(format, "Path_to_count")) {
            for (String cName : cNames) {
                if (format.startsWith(cName.toLowerCase())) {
                    ClassificationViewer viewer = (ClassificationViewer) dir.getViewerByClassName(ClassificationViewer.getClassName(cName));
                    count = CSVExportFViewer.exportName2Counts(format, viewer, file, separator, true, progressListener);
                    break;
                }
            }
        } else if (format.startsWith("keggName_to_taxon") || format.startsWith("keggId_to_taxon")) {
            count = ExportFunctionalClassIds2TaxonIds.export("KEGG", format, dir, file, separator, progressListener);
        } else if (format.startsWith("seedName_to_taxon") || format.startsWith("seedId_to_taxon")) {
            count = ExportFunctionalClassIds2TaxonIds.export("SEED", format, dir, file, separator, progressListener);
        } else if (format.startsWith("eggnogName_to_taxon") || format.startsWith("eggnogId_to_taxon")) {
            count = ExportFunctionalClassIds2TaxonIds.export("EGGNOG", format, dir, file, separator, progressListener);
        } else if (format.startsWith("interpro2goName_to_taxon") || format.startsWith("interpro2Id_to_taxon")) {
            count = ExportFunctionalClassIds2TaxonIds.export("INTERPRO2GO", format, dir, file, separator, progressListener);
        } else if (format.startsWith("listTaxon")) {
            count = CSVExportFViewer.exportNames(format, dir.getMainViewer(), file, progressListener);
        } else {
            for (String cName : cNames) {
                if (format.startsWith(cName.toLowerCase())) {
                    final ClassificationViewer viewer = (ClassificationViewer) dir.getViewerByClassName(ClassificationViewer.getClassName(cName));
                    if (Basic.endsWithIgnoreCase(format, "Name_to_count") || Basic.endsWithIgnoreCase(format, "Path_to_count")) {
                        count = CSVExportFViewer.exportName2Counts(format, viewer, file, separator, true, progressListener);
                        break;
                    } else if (Basic.endsWithIgnoreCase(format, "Name_to_readName") || Basic.endsWithIgnoreCase(format, "Path_to_readName")) {
                        count = CSVExportFViewer.exportName2ReadNames(format, viewer, file, separator, progressListener);
                        break;
                    } else if (Basic.endsWithIgnoreCase(format, "Name_to_length") || Basic.endsWithIgnoreCase(format, "Path_to_length")) {
                        count = CSVExportFViewer.exportName2TotalLength(format, viewer, file, separator, progressListener);
                        break;
                    } else if (Basic.endsWithIgnoreCase(format, "Name_to_RPK") || Basic.endsWithIgnoreCase(format, "Id_to_RPK") || Basic.endsWithIgnoreCase(format, "Path_to_RPK")) {
                        count = CSVExportFViewer.exportName2CountPerKB(format, viewer, file, separator, progressListener);
                        break;
                    }
                    if (Basic.endsWithIgnoreCase(format, "Name_to_percent") || Basic.endsWithIgnoreCase(format, "Id_to_percent") || Basic.endsWithIgnoreCase(format, "Path_to_percent")) {
                        count = CSVExportFViewer.exportName2Percent(format, viewer, file, separator, true, progressListener);
                        break;
                    }
                } else if (format.contains(cName.toLowerCase())) {
                    final ClassificationViewer viewer = (ClassificationViewer) dir.getViewerByClassName(ClassificationViewer.getClassName(cName));
                    if (format.startsWith("readName_to_")) {
                        count = CSVExportFViewer.exportReadName2Name(format, viewer, file, separator, progressListener);
                        break;
                    }
                }
            }
        }
        return count;
    }
}
