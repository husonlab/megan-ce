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
package megan.dialogs.attributes;

import jloda.graph.Node;
import jloda.graph.NodeData;
import megan.core.Director;
import megan.viewer.TaxonomyData;

import java.util.Hashtable;

/**
 * Reports the attributes of a taxon as a string
 * Daniel Huson, 11.2008
 */
public class AttributeReporter {
    private final Director dir;

    /**
     * constructor
     *
     * @param dir
     */
    public AttributeReporter(Director dir) {
        this.dir = dir;
    }

    /**
     * gets the microbial attributes for a taxon
     *
     * @param taxonId
     * @return microbial attributes or null
     */
    public String apply(Integer taxonId) {
        final String taxonName = TaxonomyData.getName2IdMap().get(taxonId);
        final Hashtable<String, String> attributes2Properties
                = AttributeData.getInstance().getTaxaName2Attributes2Properties().get(taxonName);

        if (attributes2Properties != null) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("---- Attributes: ----\n");
            buffer.append("Name:       \t").append(taxonName).append("\n");
            buffer.append("TaxId:      \t").append(taxonId).append("\n");
            buffer.append("Kingdom:    \t").append(attributes2Properties.get(AttributeData.attributeList[11])).append("\n");
            buffer.append("Group:      \t").append(attributes2Properties.get(AttributeData.attributeList[10])).append("\n");
            buffer.append("Genome size:\t").append(attributes2Properties.get(AttributeData.attributeList[8])).append(" MB\n");
            buffer.append("GC Content: \t").append(attributes2Properties.get(AttributeData.attributeList[9])).append(" %\n");
            int i = 0;
            for (String attribute : AttributeData.attributeList) {
                i++;
                if (i < 8)
                    buffer.append(attribute).append(":\t").append(attributes2Properties.get(attribute)).append("\n");
            }
            final Node v = dir.getMainViewer().getTaxId2Node(taxonId);
            if (v != null) {
                buffer.append("Reads assigned:\t").append(((NodeData) v.getData()).getCountAssigned()).append("\n");
                buffer.append("Summarized:    \t").append(((NodeData) v.getData()).getCountSummarized()).append("\n");
            }
            return buffer.toString();
        }
        return "Unknown\n";
    }
}
