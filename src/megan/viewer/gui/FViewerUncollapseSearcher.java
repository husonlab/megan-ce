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
package megan.viewer.gui;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.gui.director.ProjectManager;
import jloda.gui.find.ITextSearcher;
import jloda.phylo.PhyloTree;
import megan.core.Director;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * search for nodes and uncollapses them
 * Daniel Huson, 7.2008
 */
public class FViewerUncollapseSearcher implements ITextSearcher {
    private String name;
    private ClassificationViewer classificationViewer;
    private final Director dir;

    /**
     * constructor
     *
     * @param dir
     * @param classificationViewer
     */
    public FViewerUncollapseSearcher(Director dir, ClassificationViewer classificationViewer) {
        this.name = "Collapsed Nodes (" + classificationViewer.getName() + ")";
        this.dir = dir;
        this.classificationViewer = classificationViewer;
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
        return classificationViewer.getTree().getNumberOfNodes() > 0;
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
        if (classificationViewer == null)
            classificationViewer = (ClassificationViewer) dir.getViewerByClass(ClassificationViewer.class);
        if (classificationViewer == null)
            return;
        classificationViewer.updateView(Director.ALL);
    }

    /**
     * set select state of all objects
     *
     * @param select
     */
    public void selectAll(boolean select) {
        if (classificationViewer == null)
            classificationViewer = (ClassificationViewer) dir.getViewerByClass(ClassificationViewer.class);
        if (classificationViewer == null)
            return;
        classificationViewer.selectAllNodes(select);
    }

    /**
     * find and open the next node matching the regularExpression
     *
     * @param regularExpression
     * @param all               - uncollapse all
     * @return count
     */
    private int findAndUncollapseNext(String regularExpression, boolean reverse, boolean all) {
        System.err.println("Find and uncollapse");
        final Pattern pattern = Pattern.compile(regularExpression);

        ProjectManager.getPreviouslySelectedNodeLabels().clear();

        if (classificationViewer == null)
            classificationViewer = (ClassificationViewer) dir.getViewerByClass(ClassificationViewer.class);
        if (classificationViewer == null)
            return 0;

        final PhyloTree tree = classificationViewer.getTree();
        final Set<Node> matches = new HashSet<>();
        for (Integer fId : classificationViewer.getClassification().getName2IdMap().getIds()) {
            String name = classificationViewer.getClassification().getName2IdMap().get(fId);
            boolean ok = false;
            if (name != null) {
                Matcher matcher = pattern.matcher(name);
                if (matcher.find())
                    ok = true;
                else {
                    matcher = pattern.matcher(fId.toString());
                    if (matcher.find())
                        ok = true;
                }
                if (ok) {
                    ProjectManager.getPreviouslySelectedNodeLabels().add(name);
                    matches.addAll(classificationViewer.getClassification().getFullTree().getNodes(fId));
                }
            }
        }

        Set<Integer> toSelect = new HashSet<>();
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
                    Integer wFId = (Integer) w.getInfo();
                    if (firstCollapsedAncestor != null) {
                        classificationViewer.getCollapsedIds().remove(wFId);
                        count_uncollapsed++;
                        found = true;

                        for (Iterator it = w.getOutEdges(); it.hasNext(); ) {
                            Node u = ((Edge) it.next()).getOpposite(w);
                            if (!ancestors.contains(u)) {
                                Integer uFId = (Integer) u.getInfo();
                                if (uFId != null && !classificationViewer.getCollapsedIds().contains(uFId)) {
                                    classificationViewer.getCollapsedIds().add(uFId);
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
                toSelect.add((Integer) v.getInfo());
                if (!all && found)
                    break;
            }
        }
        if (count_uncollapsed > 0 || count_collapsed > 0) {
            boolean alreadyLocked = classificationViewer.isLocked();
            if (!alreadyLocked)
                classificationViewer.setLocked(true);
            classificationViewer.updateTree();
            classificationViewer.setSelectedIds(toSelect, true);
            if (!alreadyLocked)
                classificationViewer.setLocked(false);
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
        if (classificationViewer == null)
            classificationViewer = (ClassificationViewer) dir.getViewerByClass(ClassificationViewer.class);
        if (classificationViewer == null)
            return null;
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
            Integer fId = (Integer) u.getInfo();
            if (fId != null && classificationViewer.getCollapsedIds().contains(fId))
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
        if (classificationViewer == null && dir != null)
            classificationViewer = (ClassificationViewer) dir.getViewerByClass(ClassificationViewer.class);
        if (classificationViewer == null)
            return null;
        return classificationViewer.getFrame();
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

    /**
     * set the f viewer
     *
     * @param classificationViewer
     */
    public void setFViewer(ClassificationViewer classificationViewer) {
        this.classificationViewer = classificationViewer;
    }

    @Override
    public Collection<AbstractButton> getAdditionalButtons() {
        return null;
    }
}
