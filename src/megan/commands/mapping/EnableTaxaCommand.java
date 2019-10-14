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
package megan.commands.mapping;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class EnableTaxaCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "enable taxa={selected|all|<name,...>};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("enable taxa=");
        String name = np.getWordRespectCase();

        final MainViewer viewer = (MainViewer) getViewer();

        if (name.equalsIgnoreCase("selected")) {
            np.matchIgnoreCase(";");
            NodeSet selected = viewer.getSelectedNodes();
            for (Node v : selected) {
                int taxId = (Integer) v.getInfo();
                if (taxId > 0) {
                    TaxonomyData.getDisabledInternalTaxa().remove(taxId);
                }
            }
            TaxonomyData.getDisabledTaxa().clear();
            TaxonomyData.setDisabledInternalTaxa(TaxonomyData.getDisabledInternalTaxa());
        } else if (name.equalsIgnoreCase("all")) {
            np.matchIgnoreCase(";");
            TaxonomyData.getDisabledTaxa().clear();
            TaxonomyData.getDisabledInternalTaxa().clear();

        } else {
            while (!name.equals(";")) {
                int taxonId = Basic.isInteger(name) ? Integer.parseInt(name) : TaxonomyData.getName2IdMap().get(name);
                if (taxonId > 0)
                    TaxonomyData.getDisabledTaxa().remove(taxonId);
                if (np.peekMatchIgnoreCase(",")) {
                    np.matchIgnoreCase(",");
                }
                name = np.getWordRespectCase();
            }
        }

        System.err.println("Disabled taxa: " + TaxonomyData.getDisabledTaxa().size());
        viewer.setDoReInduce(true);
    }

    public void actionPerformed(ActionEvent event) {
        final MainViewer viewer = (MainViewer) getViewer();

        if (viewer.getSelectedNodes().size() > 0) {
            execute("enable taxa=selected;");
        } else {
            String input = JOptionPane.showInputDialog(viewer.getFrame(), "Enter names or IDs of taxa to enable");
            if (input != null) {
                StringBuilder buffer = new StringBuilder();
                String[] names = input.split(",");
                boolean first = true;
                for (String name : names) {
                    name = name.trim();
                    int taxonId = Basic.isInteger(name) ? Integer.parseInt(name) : TaxonomyData.getName2IdMap().get(name);
                    if (taxonId <= 0) {
                        NotificationsInSwing.showError(viewer.getFrame(), "Unknown taxon: " + name);
                        return;
                    }
                    if (first)
                        first = false;
                    else
                        buffer.append(", ");
                    buffer.append("'").append(name).append("'");
                }
                execute("enable taxa=" + buffer.toString() + ";");
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof MainViewer && !getDoc().getMeganFile().isReadOnly();
    }

    public String getName() {
        return "Enable...";
    }

    public String getDescription() {
        return "Enable all selected taxa or all named ones";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}

