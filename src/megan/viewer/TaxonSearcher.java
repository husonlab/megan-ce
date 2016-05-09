/*
 *  Copyright (C) 2015 Daniel H. Huson
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
import jloda.gui.find.IObjectSearcher;
import jloda.phylo.PhyloTree;
import jloda.util.ProgramProperties;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Class for finding and replacing node labels in a graph
 * Daniel Huson, 7.2008
 */
public class TaxonSearcher implements IObjectSearcher {
    private final String name;
    final PhyloTree graph;
    final MainViewer mainViewer;
    final Frame frame;

    private Iterator<Integer> currentTaxonIterator = null;
    private Integer currentTaxonId = -1;

    private final Set<Integer> toSelectTaxonIds = new HashSet<>();
    private final Set<Integer> toDeSelectTaxonIds = new HashSet<>();
    private int numberOfObjects = 0;

    final NodeSet toSelect;
    final NodeSet toDeselect;

    boolean searchInCollapsed = false;

    public static final String SEARCHER_NAME = "Nodes";

    /**
     * constructor
     *
     * @param viewer
     */
    public TaxonSearcher(MainViewer viewer) {
        this(viewer.getFrame(), SEARCHER_NAME, viewer);
        searchInCollapsed = ProgramProperties.get("TaxonomySearchInCollapsed", searchInCollapsed);
    }


    /**
     * constructor
     *
     * @param
     * @param viewer
     */
    public TaxonSearcher(Frame frame, String name, MainViewer viewer) {
        this.frame = frame;
        this.name = name;
        this.mainViewer = viewer;
        this.graph = viewer.getTree();
        toSelect = new NodeSet(graph);
        toDeselect = new NodeSet(graph);
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
        if (!searchInCollapsed) {
            final List<Integer> list = mainViewer.computeDisplayedIdsInSearchOrder();
            currentTaxonIterator = list.iterator();
            numberOfObjects = list.size();
        } else {
            final List<Integer> list = mainViewer.computeAllIdsInSearchOrder();
            currentTaxonIterator = list.iterator();
            numberOfObjects = list.size();
        }
        currentTaxonId = (currentTaxonIterator.hasNext() ? currentTaxonIterator.next() : null);

        return isCurrentSet();
    }

    /**
     * goto the next object
     */
    public boolean gotoNext() {
        if (currentTaxonIterator == null) {
            gotoFirst();
        } else if (currentTaxonIterator.hasNext())
            currentTaxonId = currentTaxonIterator.next();
        else {
            currentTaxonIterator = null;
            currentTaxonId = null;
        }

        return isCurrentSet();
    }

    /**
     * goto the last object
     * Not implemented
     */
    public boolean gotoLast() {
        currentTaxonIterator = null;
        currentTaxonId = null;
        return isCurrentSet();
    }

    /**
     * goto the previous object
     */
    public boolean gotoPrevious() {
        currentTaxonIterator = null;
        currentTaxonId = null;
        return isCurrentSet();
    }

    /**
     * is the current object selected?
     *
     * @return true, if selected
     */
    public boolean isCurrentSelected() {
        return isCurrentSet() && toSelectTaxonIds.contains(currentTaxonId);
    }

    /**
     * set selection state of current object
     *
     * @param select
     */
    public void setCurrentSelected(boolean select) {
        if (currentTaxonId != null) {
            if (select)
                toSelectTaxonIds.add(currentTaxonId);
            else
                toDeSelectTaxonIds.add(currentTaxonId);
        }
        if (currentTaxonId != null && select)
            mainViewer.setFoundNode(mainViewer.getTaxId2Node(currentTaxonId));
        else
            mainViewer.setFoundNode(null);
    }

    /**
     * set select state of all objects
     *
     * @param select
     */
    public void selectAll(boolean select) {
        mainViewer.selectAllNodes(select);
        mainViewer.repaint();
    }

    /**
     * get the label of the current object
     *
     * @return label
     */
    public String getCurrentLabel() {
        if (currentTaxonId == null)
            return null;

        final Node v = mainViewer.getTaxId2Node(currentTaxonId);
        if (v != null)
            return mainViewer.getNV(v).getLabel();
        else
            return TaxonomyData.getName2IdMap().get(currentTaxonId);
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
        return mainViewer.getSelectedNodes().size() > 0;
    }

    /**
     * is the current object set?
     *
     * @return true, if set
     */
    public boolean isCurrentSet() {
        return currentTaxonIterator != null && currentTaxonId != null;
    }

    /**
     * something has been changed or selected, rescan view
     */
    public void updateView() {
        Set<Integer> needToBeUncollapsed = new HashSet<>();

        for (Integer taxId : toSelectTaxonIds) {
            if (mainViewer.getTaxId2Node(taxId) == null) {
                needToBeUncollapsed.add(taxId);
            }
        }

        if (needToBeUncollapsed.size() > 0) {
            Set<Integer> toDelete = new HashSet<>();
            for (int t : needToBeUncollapsed) {
                Node v = TaxonomyData.getTree().getANode(t);
                while (v.getInDegree() > 0) {
                    v = v.getFirstInEdge().getSource();
                    int vt = (Integer) v.getInfo();
                    toDelete.add(vt);
                }
            }
            needToBeUncollapsed.removeAll(toDelete); // is above something that needs to be uncollapsed

            // uncollapse all nodes that we want to see
            for (int t : needToBeUncollapsed) {
                Node v = TaxonomyData.getTree().getANode(t);
                while (v.getInDegree() > 0) {
                    Node w = v.getFirstInEdge().getSource();
                    int wt = (Integer) v.getInfo();
                    if (mainViewer.getCollapsedIds().contains(wt)) {
                        mainViewer.getCollapsedIds().remove(wt);
                        break;
                    }
                    for (Edge e = w.getFirstOutEdge(); e != null; e = w.getNextOutEdge(e)) {
                        Node u = e.getTarget();
                        if (u != v) {
                            mainViewer.getCollapsedIds().add((Integer) u.getInfo());
                        }
                    }
                    v = w;
                }
            }

            mainViewer.setDoReInduce(true);
            mainViewer.updateView(Director.ALL);
        }

        toSelect.clear();
        for (int t : toSelectTaxonIds) {
            toSelect.add(mainViewer.getTaxId2Node(t));
        }
        toDeselect.clear();
        for (int t : toDeSelectTaxonIds) {
            toDeselect.add(mainViewer.getTaxId2Node(t));
        }

        mainViewer.selectedNodes.addAll(toSelect);
        mainViewer.fireDoSelect(toSelect);
        Node v = toSelect.getLastElement();

        if (v != null) {
            final Point p = mainViewer.trans.w2d(mainViewer.getLocation(v));
            if (mainViewer.getFoundNode() == null)
                mainViewer.setFoundNode(v);
            mainViewer.scrollRectToVisible(new Rectangle(p.x - 60, p.y - 25, 120, 50));

        }
        mainViewer.selectedNodes.removeAll(toDeselect);
        mainViewer.fireDoDeselect(toDeselect);
        toSelect.clear();
        toDeselect.clear();

        mainViewer.repaint();

        toSelectTaxonIds.clear();
        toDeSelectTaxonIds.clear();

        mainViewer.repaint();
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
        final JCheckBox but = new JCheckBox();
        but.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchInCollapsed = but.isSelected();
                currentTaxonId = null;
                currentTaxonIterator = null;
                ProgramProperties.put("TaxonSearchInCollapsed", searchInCollapsed);
            }
        });
        but.setToolTipText("Search in collapsed nodes as well");
        but.setText("Uncollapse");
        but.setSelected(searchInCollapsed);
        return Collections.singletonList((AbstractButton) but);
    }
}
