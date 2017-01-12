/*
 *  Copyright (C) 2017 Daniel H. Huson
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
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.fx.NotificationsInSwing;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class DisableTaxaCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "disable taxa={selected|<name,...>};";

    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("disable taxa=");

        final MainViewer viewer = (MainViewer) getViewer();

        String name = np.getWordRespectCase();
        if (name.equalsIgnoreCase("selected")) {
            NodeSet selected = viewer.getSelectedNodes();
            for (Node v : selected) {
                int taxId = (Integer) v.getInfo();
                if (taxId > 0)
                    TaxonomyData.getDisabledTaxa().add(taxId);
            }
        } else {
            while (true) {
                int taxId = Basic.isInteger(name) ? Integer.parseInt(name) : TaxonomyData.getName2IdMap().get(name);
                if (taxId > 0)
                    TaxonomyData.getDisabledTaxa().add(taxId);
                if (np.peekMatchIgnoreCase(",")) {
                    np.matchIgnoreCase(",");
                    name = np.getWordRespectCase();
                } else
                    break;
            }
        }
        np.matchIgnoreCase(";");
        System.err.println("Disabled taxa: " + TaxonomyData.getDisabledTaxa().size());
        viewer.setDoReInduce(true);
    }

    public void actionPerformed(ActionEvent event) {
        final MainViewer viewer = (MainViewer) getViewer();

        if (viewer.getSelectedNodes().size() > 0) {
            execute("disable taxa=selected;");
        } else {
            if (viewer.getSelectedNodes().size() == 0) {
                String input = JOptionPane.showInputDialog(viewer.getFrame(), "Enter names or IDs of taxa to disable");
                if (input != null) {
                    StringBuilder buffer = new StringBuilder();
                    String[] names = input.split(",");
                    boolean first = true;
                    for (String name : names) {
                        name = name.trim();
                        int taxonId = Basic.isInteger(name) ? Integer.parseInt(name) : TaxonomyData.getName2IdMap().get(name);
                        if (taxonId <= 0) {
                            NotificationsInSwing.showWarning(viewer.getFrame(), "Unknown taxon: " + name);
                            return;
                        }
                        if (first)
                            first = false;
                        else
                            buffer.append(", ");
                        buffer.append("'").append(name).append("'");
                    }
                    execute("disable taxa=" + buffer.toString() + ";");
                }
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof MainViewer && !getDoc().getMeganFile().isReadOnly();
    }

    public String getName() {
        return "Disable...";
    }

    public String getDescription() {
        return "Disable all selected taxa or all named ones";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}

