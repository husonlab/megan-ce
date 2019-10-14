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
package megan.commands.export;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class ExportCSVForAllLevelsCommand extends CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {

        np.matchIgnoreCase("export taxonname_count");

        char separator = '\t';
        if (np.peekMatchIgnoreCase("separator")) {
            np.matchIgnoreCase("separator=");
            if (np.getWordMatchesIgnoringCase("comma tab").equalsIgnoreCase("comma"))
                separator = ',';
        }


        np.matchIgnoreCase("folder=");
        String outputFile = np.getAbsoluteFileName().trim();
        np.matchIgnoreCase("file=");
        String rmaFile = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        DataTable table = new DataTable();
        BufferedReader reader = new BufferedReader(new FileReader(new File(rmaFile)));
        table.read(reader, false);
        reader.close();

        List<String> levels = TaxonomicLevels.getAllNames();
        for (String level : levels) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(outputFile + "/" + new File(rmaFile).getName() + "." + level + ".txt"))) {
                Map<Integer, float[]> tab = table.getClass2Counts(ClassificationType.Taxonomy.toString());
                for (Entry<Integer, float[]> taxid2count : tab.entrySet()) {
                    Integer taxId = taxid2count.getKey();
                    float count = taxid2count.getValue()[0];
                    if (TaxonomyData.getTaxonomicRank(taxId) != TaxonomicLevels.getId(level)) {
                        continue;
                    }
                    if (count == 0) {
                        continue;
                    }
                    w.write("\"" + TaxonomyData.getName2IdMap().get(taxId) + "\"");
                    w.write(separator + "" + count);
                    w.write("\n");
                }
            }
        }

    }

    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "export taxonname_count separator={comma|tab} folder=<foldername>";
    }

    public void actionPerformed(ActionEvent event) {

    }

    public String getName() {
        return "Export Taxon_Path Format...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export assignments";
    }
}




