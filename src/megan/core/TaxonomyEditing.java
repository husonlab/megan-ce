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
package megan.core;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.parse.NexusStreamParser;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * supports taxonomy editing
 * Daniel Huson, 2.2011
 */
class TaxonomyEditing {
    private final List<Edit> list = new LinkedList<>();
    private final HashMap<Integer, String> newTaxId2TaxName = new HashMap<>();

    /**
     * applies all edit operations to given tree
     *
     * @param tree
     * @param taxId2Node
     */
    public void apply(PhyloTree tree, Map<Integer, Node> taxId2Node) {
        for (Edit edit : list) {
            switch (edit.type) {
                case Edit.APPEND:
                    if (taxId2Node.get(edit.taxId) != null)
                        System.err.println("Can't append node, taxId already present: " + edit.taxId);
                    else if (taxId2Node.get(edit.parentId) == null)
                        System.err.println("Can't append node, parentId not present: " + edit.parentId);
                    else {
                        Node v = taxId2Node.get(edit.parentId);
                        if (v != null) {
                            Node w = tree.newNode();
                            tree.setInfo(w, edit.taxId);
                            tree.newEdge(v, w);
                            taxId2Node.put(edit.taxId, w);
                            System.err.println("Appended node " + edit.taxId + " below " + edit.parentId);
                        }
                    }
                    break;
                case Edit.DELETE:
                    if (taxId2Node.get(edit.taxId) == null)
                        System.err.println("Can't delete node, taxId not present: " + edit.taxId);
                    else {
                        Node v = taxId2Node.get(edit.taxId);
                        if (v != null) {
                            if (v.getInDegree() == 1) {
                                Node p = v.getFirstInEdge().getOpposite(v);
                                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                                    Node w = e.getOpposite(v);
                                    tree.newEdge(p, w);
                                }
                                tree.deleteNode(v);
                                taxId2Node.remove(edit.taxId);
                                System.err.println("Removed node " + edit.taxId);
                            }
                        }
                    }
                    break;
                case Edit.RENAME: // do nothing
                    break;
                default:
                    System.err.println("Unknown edit type: " + edit.type);
                    break;
            }
        }
    }

    /**
     * apply the edits to the tax mapping
     *
     * @param taxId2TaxName
     * @param taxName2TaxId
     */
    public void apply(Map<Integer, String> taxId2TaxName, Map<String, Integer> taxName2TaxId) {
        for (Edit edit : list) {
            switch (edit.type) {
                case Edit.APPEND:
                    taxId2TaxName.put(edit.taxId, edit.taxName);
                    taxName2TaxId.put(edit.taxName, edit.taxId);
                    break;
                case Edit.RENAME:
                    taxId2TaxName.put(edit.taxId, edit.taxName);
                    taxName2TaxId.put(edit.taxName, edit.taxId);
                    break;
                case Edit.DELETE:
                    taxId2TaxName.remove(edit.taxId);
                    taxName2TaxId.remove(edit.taxName);

                    break;
                default:
                    break;
            }
        }
    }

    public void addRemove(int taxId) {
        list.add(new Edit(taxId));
    }

    public void addAppend(int parentId, int taxId, String taxName) {
        list.add(new Edit(parentId, taxId, taxName));
        newTaxId2TaxName.put(taxId, taxName);
    }

    public Iterator<Edit> iterator() {
        return list.iterator();
    }

    public String toString() {
        StringWriter w = new StringWriter();
        for (Edit edit : list) {
            w.write(edit.toString() + ";");
        }
        return w.toString();
    }

    public int fromString(String string) throws IOException {
        NexusStreamParser np = new NexusStreamParser(new StringReader(string));

        int count = 0;
        while (np.peekMatchAnyTokenIgnoreCase("A R")) {
            Edit edit = Edit.parse(np);
            if (edit != null) {
                list.add(edit);
                np.matchIgnoreCase(";");
            }
            count++;
        }
        return count;
    }


}

class Edit {
    public final static int DELETE = 1;
    public final static int APPEND = 2;
    public final static int RENAME = 3;
    public int parentId;
    public int taxId;
    public String taxName;
    public int type;

    public Edit() {
    }

    public Edit(int taxId) {
        this.taxId = taxId;
        type = DELETE;
    }

    private Edit(int taxId, String taxName) {
        this.taxId = taxId;
        this.taxName = taxName;
        type = RENAME;
    }

    public Edit(int parentId, int taxId, String taxName) {
        this.parentId = parentId;
        this.taxId = taxId;
        this.taxName = taxName;
        type = APPEND;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getTaxId() {
        return taxId;
    }

    public void setTaxId(int taxId) {
        this.taxId = taxId;
    }

    public String getTaxName() {
        return taxName;
    }

    public void setTaxName(String taxName) {
        this.taxName = taxName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    /**
     * write the edit
     *
     * @return
     */
    public String toString() {
        switch (type) {
            case APPEND:
                return "A " + parentId + " " + taxId + " '" + taxName + "'";
            case DELETE:
                return "D " + taxId;
            case RENAME:
                return "R " + taxId + " '" + taxName + "'";
            default:
                return "NONE";
        }
    }


    /**
     * attempts to parse an edit string
     *
     * @param string
     * @return edit or null
     * @throws IOException
     */
    public static Edit fromString(String string) throws IOException {
        return parse(new NexusStreamParser(new StringReader(string)));
    }

    /**
     * parse an edit object
     *
     * @param np
     * @return edit object
     * @throws IOException
     */
    public static Edit parse(NexusStreamParser np) throws IOException {
        if (np.peekMatchIgnoreCase("A")) {
            np.matchIgnoreCase("A");
            return new Edit(np.getInt(), np.getInt(), np.getWordRespectCase());

        } else if (np.peekMatchIgnoreCase("D")) {
            np.matchIgnoreCase("D");
            return new Edit(np.getInt());
        } else if (np.peekMatchIgnoreCase("R")) {
            np.matchIgnoreCase("R");
            return new Edit(np.getInt(), np.getWordRespectCase());
        } else return null;
    }
}
