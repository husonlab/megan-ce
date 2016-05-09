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
package megan.util;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.gui.director.ProjectManager;
import jloda.gui.find.ITextSearcher;
import jloda.phylo.PhyloTree;
import megan.core.Director;
import megan.core.Document;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * search for nodes and uncollapses them
 * Daniel Huson, 7.2008
 */
public class UncollapseSearcher implements ITextSearcher {

    private final String name;
    private final Document doc;
    private final Director dir;

    /**
     * constructor
     *
     * @param name
     * @param dir
     * @param doc
     */
    public UncollapseSearcher(String name, Director dir, Document doc) {
        this.dir = dir;
        this.doc = doc;
        this.name = name;
    }


    /**
     * get the name for this type of search
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Find first instance
     *
     * @param regularExpression
     * @return - returns boolean: true if text found, false otherwise
     */
    public boolean findFirst(String regularExpression) {
        return findAndUncollapseNext(regularExpression, false, false) > 0;
    }

    /**
     * Find next instance
     *
     * @param regularExpression
     * @return - returns boolean: true if text found, false otherwise
     */
    public boolean findNext(String regularExpression) {
        return findFirst(regularExpression);
    }


    /**
     * Find previous instance
     *
     * @param regularExpression
     * @return - returns boolean: true if text found, false otherwise
     */
    public boolean findPrevious(String regularExpression) {
        return findAndUncollapseNext(regularExpression, true, false) > 0;
    }

    /**
     * Replace the next instance with current. Does nothing if selection invalid.
     *
     * @param regularExpression
     */
    public boolean replaceNext(String regularExpression, String replaceText) {
        System.err.println("not implemented");
        return false;
    }


    /**
     * Replace all occurrences of text in document, subject to options.
     *
     * @param regularExpression
     * @param replaceText
     * @param selectionOnly
     * @return number of instances replaced
     */
    public int replaceAll(String regularExpression, String replaceText, boolean selectionOnly) {
        System.err.println("not implemented");
        return 0;
    }

    /**
     * Selects all occurrences of text in document, subject to options and constraints of document type
     *
     * @param regularExpression
     */
    public int findAll(String regularExpression) {
        return findAndUncollapseNext(regularExpression, false, true);
    }

    /**
     * is a global find possible?
     *
     * @return true, if there is at least one object
     */
    public boolean isGlobalFindable() {
        return doc.getDir().getMainViewer().getTree().getNumberOfNodes() > 0;
    }

    /**
     * is a selection find possible
     *
     * @return true, if at least one object is selected
     */
    public boolean isSelectionFindable() {
        return false;
    }

    /**
     * does this searcher support find all?
     *
     * @return true, if find all supported
     */
    public boolean canFindAll() {
        return true;
    }

    /**
     * something has been changed or selected, rescan view
     */
    public void updateView() {
        dir.execute("update reinduce=true;select nodes=previous;", dir.getMainViewer().getCommandManager());
    }

    /**
     * set select state of all objects
     *
     * @param select
     */
    public void selectAll(boolean select) {
        dir.getMainViewer().selectAllNodes(select);
    }

    /**
     * find and open a the next node matching the regularExpression
     *
     * @param regularExpression
     * @param all               - uncollapse all
     * @return count
     */
    private int findAndUncollapseNext(String regularExpression, boolean reverse, boolean all) {
        System.err.println("Find and uncollapse");
        final Pattern pattern = Pattern.compile(regularExpression);

        MainViewer mainViewer = doc.getDir().getMainViewer();
        PhyloTree tree = mainViewer.getTree();

        ProjectManager.getPreviouslySelectedNodeLabels().clear();

        final NodeSet matches = new NodeSet(tree);
        for (String name : TaxonomyData.getName2IdMap().getNames()) {
            boolean ok = false;
            if (name != null) {
                final int taxId = TaxonomyData.getName2IdMap().get(name);
                if (taxId != 0) {
                    Matcher matcher = pattern.matcher(name);
                    if (matcher.find())
                        ok = true;
                    else {
                        matcher = pattern.matcher("" + taxId);
                        if (matcher.find())
                            ok = true;
                    }
                    if (ok) {
                        ProjectManager.getPreviouslySelectedNodeLabels().add(name);
                        Node v = mainViewer.getTaxId2Node(taxId);
                        if (v != null && (doc.getNumberOfReads() == 0 || ((NodeData) v.getData()).getCountSummarized() > 0))
                            matches.add(v);
                    }
                }
            }
        }

        int count_uncollapsed = 0;
        int count_collapsed = 0;
        final NodeSet ancestors = new NodeSet(tree);
        // for (Node v = matches.getFirstElement(); v != null; v = matches.getNextElement(v)) {
        for (Node v = (reverse ? tree.getLastNode() : tree.getFirstNode()); v != null; v = (reverse ? v.getPrev() : v.getNext())) {
            if (matches.contains(v)) {
                boolean found = false;
                Node firstCollapsedAncestor = getFirstCollapsedAncestor(v);
                Node w = v;
                while (w != null) {
                    ancestors.add(w);
                    Integer wTaxId = (Integer) w.getInfo();
                    if (firstCollapsedAncestor != null) {
                        doc.getDir().getMainViewer().getCollapsedIds().remove(wTaxId);
                        count_uncollapsed++;
                        found = true;

                        for (Iterator it = w.getOutEdges(); it.hasNext(); ) {
                            Node u = ((Edge) it.next()).getOpposite(w);
                            if (!ancestors.contains(u)) {
                                Integer uTaxId = (Integer) u.getInfo();
                                if (uTaxId != null && !doc.getDir().getMainViewer().getCollapsedIds().contains(uTaxId)) {
                                    doc.getDir().getMainViewer().getCollapsedIds().add(uTaxId);
                                    count_collapsed++;
                                }
                            }
                        }
                        if (w == firstCollapsedAncestor)
                            firstCollapsedAncestor = null;
                    }
                    if (w.getInDegree() != 0) {
                        w = w.getInEdges().next().getOpposite(w);
                    } else
                        w = null;
                }
                if (!all && found)
                    break;
            }
        }
        return matches.size();
    }

    /**
     * returns the first collapsed node on the path from the root to v
     *
     * @param v
     * @return first collapsed node or null
     */
    private Node getFirstCollapsedAncestor(Node v) {
        List<Node> path = new LinkedList<>();
        while (v != null) {
            Iterator it = v.getInEdges();
            if (it.hasNext()) {
                v = ((Edge) it.next()).getOpposite(v);
                if (v != null)
                    path.add(0, v);
            } else
                v = null;
        }
        for (Node u : path) {
            Integer uTaxId = (Integer) u.getInfo();
            if (uTaxId != null && doc.getDir().getMainViewer().getCollapsedIds().contains(uTaxId))
                return u;
        }
        return null;
    }


    /**
     * get the parent component
     *
     * @return parent
     */
    public Component getParent() {
        return dir.getMainViewer().getFrame();
    }


    /**
     * set scope global rather than selected
     *
     * @param globalScope
     */
    public void setGlobalScope(boolean globalScope) {
    }

    /**
     * get scope global rather than selected
     *
     * @return true, if search scope is global
     */
    public boolean isGlobalScope() {
        return true;
    }

    @Override
    public Collection<AbstractButton> getAdditionalButtons() {
        return null;
    }
}
