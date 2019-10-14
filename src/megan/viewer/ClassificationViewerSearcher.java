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
package megan.viewer;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.find.IObjectSearcher;
import jloda.util.ProgramProperties;
import megan.classification.ClassificationManager;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

/**
 * Class for finding and replacing node labels in an fViewer
 * Daniel Huson, 10.2015
 */
public class ClassificationViewerSearcher implements IObjectSearcher {
    private final String name;
    private final PhyloTree graph;
    private final ClassificationViewer classificationViewer;
    private final Frame frame;

    private Iterator<Integer> currentIterator = null;
    private Integer currentId = -1;

    private final Set<Integer> toSelectIds = new HashSet<>();
    private final Set<Integer> toDeSelectIds = new HashSet<>();
    private int numberOfObjects = 0;

    private final NodeSet toSelect;
    private final NodeSet toDeselect;

    private boolean searchInCollapsed = false;

    private int countCurrent = 0; // use this to cycle throug different instances of current node id

    private final AbstractButton uncollapseButton;

    /**
     * constructor
     *
     * @param
     * @param viewer
     */
    public ClassificationViewerSearcher(Frame frame, String name, ClassificationViewer viewer) {
        this.frame = frame;
        this.name = name;
        this.classificationViewer = viewer;
        this.graph = viewer.getTree();
        toSelect = new NodeSet(graph);
        toDeselect = new NodeSet(graph);
        searchInCollapsed = ProgramProperties.get("FViewerSearchInCollapsed", false);

        uncollapseButton = new JCheckBox();
        uncollapseButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchInCollapsed = uncollapseButton.isSelected();
                currentId = null;
                currentIterator = null;
                ProgramProperties.put("FViewerSearchInCollapsed", searchInCollapsed);
            }
        });
        uncollapseButton.setToolTipText("Search in collapsed nodes as well");
        uncollapseButton.setText("Uncollapse");
        uncollapseButton.setSelected(searchInCollapsed);
    }

    /**
     * get the parent component
     *
     * @return parent
     */
    public Component getParent() {
        return frame;
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
     * goto the first object
     */
    public boolean gotoFirst() {
        countCurrent = 0;

        if (!searchInCollapsed) {
            final List<Integer> list = classificationViewer.computeDisplayedIdsInSearchOrder();
            currentIterator = list.iterator();
            numberOfObjects = list.size();
        } else {
            final List<Integer> list = classificationViewer.computeAllIdsInSearchOrder();
            currentIterator = list.iterator();
            numberOfObjects = list.size();
        }
        currentId = (currentIterator.hasNext() ? currentIterator.next() : null);

        return isCurrentSet();
    }

    /**
     * goto the next object
     */
    public boolean gotoNext() {
        if (currentIterator == null) {
            gotoFirst();
        } else if (currentIterator.hasNext())
            currentId = currentIterator.next();
        else {
            currentIterator = null;
            currentId = null;
        }

        return isCurrentSet();
    }

    /**
     * goto the last object
     * Not implemented
     */
    public boolean gotoLast() {
        currentIterator = null;
        currentId = null;
        return isCurrentSet();
    }

    /**
     * goto the previous object
     */
    public boolean gotoPrevious() {
        currentIterator = null;
        currentId = null;
        return isCurrentSet();
    }

    /**
     * is the current object selected?
     *
     * @return true, if selected
     */
    public boolean isCurrentSelected() {
        return isCurrentSet() && toSelectIds.contains(currentId);
    }

    /**
     * set selection state of current object
     *
     * @param select
     */
    public void setCurrentSelected(boolean select) {
        if (currentId != null) {
            if (select)
                toSelectIds.add(currentId);
            else
                toDeSelectIds.add(currentId);
        }
        if (currentId != null && select) {
            Set<Node> set = classificationViewer.getNodes(currentId);
            if (set.size() > 1 && countCurrent < set.size()) {
                final Node[] nodes = classificationViewer.getNodes(currentId).toArray(new Node[set.size()]);
                classificationViewer.setFoundNode(nodes[countCurrent++]);
            } else
                classificationViewer.setFoundNode(classificationViewer.getANode(currentId));
        } else
            classificationViewer.setFoundNode(null);
    }

    /**
     * set select state of all objects
     *
     * @param select
     */
    public void selectAll(boolean select) {
        classificationViewer.selectAllNodes(select);
        classificationViewer.repaint();
    }

    /**
     * get the label of the current object
     *
     * @return label
     */
    public String getCurrentLabel() {
        if (currentId == null)
            return null;
        final Node v = classificationViewer.getANode(currentId);
        if (v != null)
            return classificationViewer.getNV(v).getLabel();
        else
            return ClassificationManager.get(name, true).getName2IdMap().get(currentId);
    }

    /**
     * set the label of the current object
     *
     * @param newLabel
     */
    public void setCurrentLabel(String newLabel) {
        // not implemented
    }

    /**
     * is a global find possible?
     *
     * @return true, if there is at least one object
     */
    public boolean isGlobalFindable() {
        return true;
    }

    /**
     * is a selection find possible
     *
     * @return true, if at least one object is selected
     */
    public boolean isSelectionFindable() {
        return classificationViewer.getSelectedNodes().size() > 0;
    }

    /**
     * is the current object set?
     *
     * @return true, if set
     */
    public boolean isCurrentSet() {
        return currentIterator != null && currentId != null;
    }

    /**
     * something has been changed or selected, rescan view
     */
    public void updateView() {
        Set<Integer> needToBeUncollapsed = new HashSet<>();

        for (Integer id : toSelectIds) {
            if (classificationViewer.getANode(id) == null) {
                needToBeUncollapsed.add(id);
            }
        }

        if (needToBeUncollapsed.size() > 0) {
            Set<Integer> toDelete = new HashSet<>();
            for (int t : needToBeUncollapsed) {
                for (Node v : ClassificationManager.get(name, true).getFullTree().getNodes(t)) {
                    while (v.getInDegree() > 0) {
                        v = v.getFirstInEdge().getSource();
                        int vt = (Integer) v.getInfo();
                        toDelete.add(vt);
                    }
                }
            }
            needToBeUncollapsed.removeAll(toDelete); // is above something that needs to be uncollapsed

            // uncollapse all nodes that we want to see
            for (int t : needToBeUncollapsed) {
                for (Node v : ClassificationManager.get(name, true).getFullTree().getNodes(t)) {
                    while (v.getInDegree() > 0) {
                        Node w = v.getFirstInEdge().getSource();
                        int wt = (Integer) v.getInfo();
                        if (classificationViewer.getCollapsedIds().contains(wt)) {
                            classificationViewer.getCollapsedIds().remove(wt);
                            break;
                        }
                        for (Edge e = w.getFirstOutEdge(); e != null; e = w.getNextOutEdge(e)) {
                            Node u = e.getTarget();
                            if (u != v) {
                                classificationViewer.getCollapsedIds().add((Integer) u.getInfo());
                            }
                        }
                        v = w;
                    }
                }
            }

            classificationViewer.updateTree();
            classificationViewer.updateView(Director.ALL);
        }

        toSelect.clear();
        for (int t : toSelectIds) {
            final Set<Node> nodes = classificationViewer.getNodes(t);
            if (nodes != null) {
                toSelect.addAll(nodes);
            }
        }
        toDeselect.clear();
        for (int t : toDeSelectIds) {
            final Set<Node> nodes = classificationViewer.getNodes(t);
            if (nodes != null) {
                toDeselect.addAll(nodes);
            }
        }

        classificationViewer.selectedNodes.addAll(toSelect);
        classificationViewer.fireDoSelect(toSelect);
        Node v = classificationViewer.getFoundNode();
        if (v == null)
            v = toSelect.getLastElement();

        if (v != null) {
            try {
                final Point p = classificationViewer.trans.w2d(classificationViewer.getLocation(v));
                classificationViewer.scrollRectToVisible(new Rectangle(p.x - 60, p.y - 25, 500, 100));
            } catch (Exception ex) {// happens occasionally
            }
        }
        classificationViewer.selectedNodes.removeAll(toDeselect);
        classificationViewer.fireDoDeselect(toDeselect);
        toSelect.clear();
        toDeselect.clear();

        classificationViewer.repaint();

        toSelectIds.clear();
        toDeSelectIds.clear();

        classificationViewer.repaint();
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
     * how many objects are there?
     *
     * @return number of objects or -1
     */
    public int numberOfObjects() {
        return numberOfObjects;
    }

    @Override
    public Collection<AbstractButton> getAdditionalButtons() {
        return Collections.singletonList(uncollapseButton);
    }
}
