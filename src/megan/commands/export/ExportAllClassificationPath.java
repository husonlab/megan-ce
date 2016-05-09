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
package megan.commands.export;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.LinkedList;

public class ExportAllClassificationPath extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export selected path file=<file>;";
    }

    public void apply(NexusStreamParser np) throws Exception {

        np.matchIgnoreCase("export selected path");
        np.matchIgnoreCase("file=");
        String file = np.getWordFileNamePunctuation();
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(file)));

        NodeSet selected = ((ClassificationViewer) getViewer()).getSelectedNodes();
        for (Node v : selected) {
            LinkedList<String> list = new LinkedList<>();
            int leafTaxId = -1;
            while (true) {
                if (leafTaxId == -1) {
                    leafTaxId = (Integer) v.getInfo();
                }
                Integer taxId = (Integer) v.getInfo();
                if (taxId != null)
                    list.add(TaxonomyData.getName2IdMap().get(taxId));
                if (v.getInDegree() > 0)
                    v = v.getFirstInEdge().getSource();
                else
                    break;
            }
            boolean first = true;
            StringBuilder buf = new StringBuilder();
            buf.append(leafTaxId).append("\t");
            for (Iterator<String> it = list.descendingIterator(); it.hasNext(); ) {
                if (first)
                    first = false;
                else
                    buf.append(";");
                buf.append(it.next());
            }
            buf.append("\n");
            writer.write(buf.toString().replaceAll("Root;", ""));

        }
        writer.close();
    }

    public void actionPerformed(ActionEvent event) {

    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Export selected path...";
    }

    public String getDescription() {
        return "Export select Path";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

}
