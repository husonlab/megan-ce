/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.commands.mapping;

import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;

public class ChangeTaxonMappingCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "changeMapping taxName=<taxon-name> taxId=<taxon-id>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("changeMapping taxName=");
        String taxonName = np.getWordRespectCase();
        np.matchIgnoreCase("taxId=");
        int taxId = np.getInt();
        np.matchIgnoreCase(";");

        System.err.println("Changing taxon mapping of '" + taxonName + " from " + TaxonomyData.getName2IdMap().get(taxonName) + " to " + taxId);
        TaxonomyData.getName2IdMap().put(taxonName, taxId);
        TaxonomyData.setTaxonomicRank(taxId, (byte) 0);

        Collection<Pair<String, String>> mappingFixes = new LinkedList<>();
        mappingFixes = ProgramProperties.get(MeganProperties.TAXON_MAPPING_CHANGES, mappingFixes);
        boolean found = false;
        for (Pair<String, String> pair : mappingFixes) {
            if (pair.getFirst().equals(taxonName)) {
                pair.setSecond("" + taxId);
                found = true;
                break;
            }
        }
        if (!found)
            mappingFixes.add(new Pair<>(taxonName, "" + taxId));
        ProgramProperties.put(MeganProperties.TAXON_MAPPING_CHANGES, mappingFixes);
    }

    public void actionPerformed(ActionEvent event) {
        String result = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter taxon name followed by taxon ID", "Enter taxon mapping fix", JOptionPane.QUESTION_MESSAGE);
        if (result != null) {
            int pos = result.length() - 1;
            while (pos >= 0 && !Character.isWhitespace(result.charAt(pos)))
                pos--;
            if (pos > 0 && Basic.isInteger(result.substring(pos + 1))) {
                String taxName = result.substring(0, pos).trim();
                String taxId = result.substring(pos + 1).trim();
                executeImmediately("changeMapping taxName='" + taxName + "' taxId=" + taxId + ";");
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof MainViewer;
    }

    public String getName() {
        return "Add A Change...";
    }

    public String getDescription() {
        return "Change the taxon name to taxon id mapping for a given taxon";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return null;
    }
}

