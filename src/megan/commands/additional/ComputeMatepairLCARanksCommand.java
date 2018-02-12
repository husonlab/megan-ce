/*
 *  Copyright (C) 2018 Daniel H. Huson
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
package megan.commands.additional;

import jloda.graph.Node;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;

public class ComputeMatepairLCARanksCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "mpAnalyzer what={lca-ranks|compare} infile=<filename> outfile=<filename>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("mpAnalyzer what=");
        String what = np.getWordMatchesIgnoringCase("lca-ranks compare");
        np.matchIgnoreCase("infile=");
        String infile = np.getWordFileNamePunctuation();
        np.matchIgnoreCase("outfile=");
        String outfile = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        int lines = 0;
        int warnings = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(infile))) {
            BufferedWriter w = new BufferedWriter(new FileWriter(outfile));
            try {
                String aLine;
                while ((aLine = r.readLine()) != null) {
                    String[] tokens = aLine.split("\t");
                    if (tokens.length != 3) {
                        if (warnings < 10)
                            System.err.println("Skipping line: " + aLine);
                        else if (warnings == 10)
                            System.err.println("...");
                        warnings++;
                        continue;
                    }
                    if (tokens[0].equals("ReadID"))
                        continue;

                    if (what.charAt(0) == 'l') {   // lca-ranks
                        int taxId1 = TaxonomyData.getName2IdMap().get(tokens[1].trim());
                        int taxId2 = TaxonomyData.getName2IdMap().get(tokens[2].trim());
                        if (taxId1 <= 0 || taxId2 <= 0) {
                            w.write("NA\n");
                            lines++;
                        } else {
                            HashSet<Integer> taxIds = new HashSet<>();
                            taxIds.add(taxId1);
                            taxIds.add(taxId2);
                            Integer taxId = TaxonomyData.getLCA(taxIds, false);
                            int level = 0;
                            while (level == 0) {
                                level = TaxonomyData.getTaxonomicRank(taxId);
                                if (level == 0) {
                                    Node v = TaxonomyData.getTree().getANode(taxId);
                                    if (v == null || v.getInDegree() == 0)
                                        break;
                                    v = v.getFirstInEdge().getSource();
                                    taxId = (Integer) v.getInfo();
                                }
                            }
                            if (level != 0)
                                w.write(TaxonomicLevels.getName(level) + "\n");
                            else
                                w.write("NA\n");
                            lines++;
                        }
                    } else // compare
                    {
                        String readId = tokens[0].trim();
                        String taxonName1 = tokens[1].trim();
                        String taxonName2 = tokens[2].trim();
                        int taxId1 = TaxonomyData.getName2IdMap().get(taxonName1);
                        int taxId2 = TaxonomyData.getName2IdMap().get(taxonName2);
                        if (taxId1 <= 0 || taxId2 <= 0) {
                            w.write(readId + "\tNA\tNA\n");
                            lines++;
                        } else {
                            HashSet<Integer> taxIds = new HashSet<>();
                            taxIds.add(taxId1);
                            taxIds.add(taxId2);
                            Integer taxId = TaxonomyData.getLCA(taxIds, true);
                            if (taxId == taxId1 || taxId == taxId2) {
                                w.write(readId + "\t" + TaxonomyData.getName2IdMap().get(taxId) + "\t" + TaxonomyData.getName2IdMap().get(taxId) + "\n");
                            } else {
                                w.write(readId + "\t" + taxonName1 + "\t" + taxonName2 + "\n");
                            }
                            lines++;
                        }
                    }
                }
            } finally {
                w.flush();
                w.close();
                System.err.println("lines wrote: " + lines);
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "MP-Compute";
    }

    public String getDescription() {
        return "Compute the rank at which the LCA is found for each mate-pair, or preprocess comparison";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return false;
    }
}
