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
import jloda.gui.commands.ICommand;
import jloda.util.Pair;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.rma2.ClassReadIdIteratorRMA2;
import megan.rma2.RMA2Connector;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.*;

public class ExportTaxonomicPathForAllNodesCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export readname2taxpath file=<file>;";
    }

    public void apply(NexusStreamParser np) throws Exception {

        np.matchIgnoreCase("export readname2taxpath");
        np.matchIgnoreCase("file=");
        String file = np.getWordFileNamePunctuation();

        RMA2Connector con = (RMA2Connector) getDoc().getConnector();
        Map<Long, Integer> readKey2TaxId = new HashMap<>();
        System.out.println("Loading Classification values for all Reads");
        ClassReadIdIteratorRMA2 classreadIt = new ClassReadIdIteratorRMA2("Taxonomy", new File(getDoc().getMeganFile().getFileName()));
        while (classreadIt.hasNext()) {
            Pair<Integer, List<Long>> pair = classreadIt.next();
            int classId = pair.getFirstInt();
            List<Long> readIds = pair.get2();
            for (Long readId : readIds) {
                readKey2TaxId.put(readId, classId);
            }
        }
        classreadIt.close();
        IReadBlockIterator it = con.getAllReadsIterator(0, 10000, true, false);
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(file)));
        while (it.hasNext()) {
            IReadBlock irb = it.next();
            String readName = irb.getReadName();
            String taxPath = getTaxPath(readKey2TaxId.get(irb.getUId()));
            writer.write(readName + "\t\"" + taxPath + ";\"\n");
        }
        writer.close();

        System.out.println("done...");
    }

    private String getTaxPath(Integer taxid) {
        Node v = TaxonomyData.getTree().getANode(taxid);
        StringWriter writer = new StringWriter();

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
        for (Iterator<String> it = list.descendingIterator(); it.hasNext(); ) {
            if (first)
                first = false;
            else
                buf.append(";");
            buf.append(it.next());
        }
        writer.write(buf.toString());
        return writer.toString();
    }

    public void actionPerformed(ActionEvent event) {

    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Export readname_taxPath.";
    }

    public String getDescription() {
        return "Export readname to taxonomic path for all reads";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}
