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
package megan.classification.data;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.algorithms.LCAAddressing;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.viewer.TaxonomyData;

import java.io.*;
import java.util.*;

import static megan.main.MeganProperties.DISABLED_TAXA;

/**
 * the classification full tree
 * Daniel Huson, 4.2010, 4.2015
 */
public class ClassificationFullTree extends PhyloTree {
    private final Name2IdMap name2IdMap;
    private final Map<Integer, Set<Node>> id2Nodes = new HashMap<>(); // maps each id to all equivalent nodes
    private final Map<Integer, Node> id2Node = new HashMap<>(); //maps each id to a node

    private final Map<Integer, String> id2Address = new HashMap<>();
    private final Map<String, Integer> address2Id = new HashMap<>();

    private final NodeData emptyData = new NodeData(new float[0], new float[0]);

    /**
     * constructor
     *
     * @param name2IdMap
     */
    public ClassificationFullTree(String cName, Name2IdMap name2IdMap) {
        this.name2IdMap = name2IdMap;
        this.setName(cName);
        setAllowMultiLabeledNodes(true);
    }

    public void clear() {
        super.clear();
        id2Node.clear();
        id2Nodes.clear();

        id2Address.clear();
        address2Id.clear();
    }

    /**
     * load the tree from a file
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public void loadFromFile(String fileName) throws IOException {
        clear();

        System.err.print("Loading " + Basic.getFileNameWithoutPath(fileName) + ": ");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(ResourceManager.getFileAsStream(fileName)))) {
            read(r, true);
        }
        for (Node v = getFirstNode(); v != null; v = v.getNext()) {
            if (Basic.isInteger(getLabel(v))) {
                int id = Integer.parseInt(getLabel(v));
                setInfo(v, id);
                addId2Node(id, v);
                if (v.getInDegree() > 1)
                    System.err.println("Reticulate node: " + id);
            } else
                throw new IOException("Node has illegal label: " + getLabel(v));
        }
        if (id2Node.get(IdMapper.NOHITS_ID) == null) {
            Node v = newNode();
            addId2Node(IdMapper.NOHITS_ID, v);
            name2IdMap.put(IdMapper.NOHITS_LABEL, IdMapper.NOHITS_ID);
            name2IdMap.setRank(IdMapper.NOHITS_ID, 0);
            newEdge(getRoot(), v);
        }
        setInfo(getANode(IdMapper.NOHITS_ID), IdMapper.NOHITS_ID);


        if (id2Node.get(IdMapper.UNASSIGNED_ID) == null) {
            Node v = newNode();
            addId2Node(IdMapper.UNASSIGNED_ID, v);
            name2IdMap.put(IdMapper.UNASSIGNED_LABEL, IdMapper.UNASSIGNED_ID);
            name2IdMap.setRank(IdMapper.UNASSIGNED_ID, 0);
            newEdge(getRoot(), v);
        }
        setInfo(getANode(IdMapper.UNASSIGNED_ID), IdMapper.UNASSIGNED_ID);

        if (id2Node.get(IdMapper.LOW_COMPLEXITY_ID) == null) {
            Node v = newNode();
            addId2Node(IdMapper.LOW_COMPLEXITY_ID, v);
            name2IdMap.put(IdMapper.LOW_COMPLEXITY_LABEL, IdMapper.LOW_COMPLEXITY_ID);
            name2IdMap.setRank(IdMapper.LOW_COMPLEXITY_ID, 0);
            newEdge(getRoot(), v);
        }
        setInfo(getANode(IdMapper.LOW_COMPLEXITY_ID), IdMapper.LOW_COMPLEXITY_ID);

        if (id2Node.get(IdMapper.CONTAMINANTS_ID) == null) {
            Node v = newNode();
            addId2Node(IdMapper.CONTAMINANTS_ID, v);
            name2IdMap.put(IdMapper.CONTAMINANTS_LABEL, IdMapper.CONTAMINANTS_ID);
            name2IdMap.setRank(IdMapper.CONTAMINANTS_ID, 0);
            newEdge(getRoot(), v);
        }
        setInfo(getANode(IdMapper.CONTAMINANTS_ID), IdMapper.CONTAMINANTS_ID);

        if (getName().equals(Classification.Taxonomy)) {
            // fix Domains:
            int taxId = name2IdMap.get("Bacteria");
            if (taxId > 0)
                name2IdMap.setRank(taxId, 127);
            taxId = name2IdMap.get("Archaea");
            if (taxId > 0)
                name2IdMap.setRank(taxId, 127);
            taxId = name2IdMap.get("Eukaryota");
            if (taxId > 0)
                name2IdMap.setRank(taxId, 127);


            // disable taxa
            for (int t : ProgramProperties.get(DISABLED_TAXA, new int[0])) {
                TaxonomyData.getDisabledTaxa().add(t);
            }
        }

        LCAAddressing.computeAddresses(this, id2Address, address2Id);
        System.err.println(String.format("%,9d", getNumberOfNodes()));
    }


    /**
     * add all ids that many be missing from the tree to the tree
     */
    private void addMissingToTree(String unclassifiedNodeLabel, int unclassifiedNodeId, String labelFormat, final int maxId) {
        // add additional nodes for ids that are not found in the tree:
        Node unclassified = null;

        for (int id = 1; id <= maxId; id++) {
            if (!containsId(id)) {

                if (unclassified == null) {
                    unclassified = newNode();
                    Edge before = null;
                    for (Edge e = getRoot().getFirstOutEdge(); e != null; e = getRoot().getNextOutEdge(e)) {
                        Node w = e.getTarget();
                        Integer wid = (Integer) w.getInfo();
                        if (wid < 0) {
                            before = e;
                            break;
                        }
                    }
                    if (before != null) // insert unclassified edge before the no hits etc nodes
                        newEdge(getRoot(), before, unclassified, null, Edge.BEFORE, Edge.AFTER, null);
                    else
                        newEdge(getRoot(), unclassified);
                    unclassified.setInfo(unclassifiedNodeId);
                    addId2Node(unclassifiedNodeId, unclassified);
                    name2IdMap.put(unclassifiedNodeLabel, unclassifiedNodeId);
                }

                Node v = newNode(id);
                newEdge(unclassified, v);
                name2IdMap.put(String.format(labelFormat, id), id);
                addId2Node(id, v);
            }
        }
    }

    /**
     * save to file
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public void saveToFile(String fileName) throws IOException {
        System.err.println("Saving tree to file: " + fileName);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName))) {
            write(w, false);
            w.write(";\n");
        }
        System.err.println("done (" + getNumberOfNodes() + " nodes)");
    }

    /**
     * extract the induced tree
     *
     * @param id2data
     * @param collapsedIds
     * @param targetTree
     */
    public void extractInducedTree(Map<Integer, NodeData> id2data, Set<Integer> collapsedIds, PhyloTree targetTree, Map<Integer, Set<Node>> targetId2Nodes) {
        final Map<Integer, Float> id2count = new HashMap<>();
        for (Integer id : id2data.keySet()) {
            id2count.put(id, id2data.get(id).getCountAssigned());
        }

        final NodeSet keep = new NodeSet(this);
        labelToKeepRec(getRoot(), id2count.keySet(), keep);

        targetTree.clear();

        final Node rootCpy = targetTree.newNode();
        Map<Node, Node> node2cpy = new HashMap<>(); // used in context of reticulate nodes, currently not supported
        targetTree.setRoot(rootCpy);
        rootCpy.setInfo(getRoot().getInfo());
        node2cpy.put(getRoot(), rootCpy);
        final int rootId = (Integer) getRoot().getInfo();
        final NodeData rootData = id2data.get(rootId);
        rootCpy.setData(rootData != null ? rootData : emptyData);

        induceRec(getRoot(), rootCpy, targetTree, keep, collapsedIds, id2data, node2cpy);

        targetId2Nodes.clear();
        for (Node v = targetTree.getFirstNode(); v != null; v = v.getNext()) {
            int id = (Integer) v.getInfo();
            Set<Node> nodes = targetId2Nodes.computeIfAbsent(id, k -> new HashSet<>());
            nodes.add(v);
            if (v.getInDegree() > 1)
                System.err.println("Reticulate node: " + id + " (currently not supported)");
        }
        System.err.println(String.format("Induced tree has %,d of %,d nodes", +keep.size(), getNumberOfNodes()));

        if (collapsedIds.size() > 0) { // ensure that set of collapsed ids is only as large as necessary for given data
            final Set<Integer> notNeeded = new HashSet<>();
            for (Node v = targetTree.getFirstNode(); v != null; v = targetTree.getNextNode(v)) {
                Integer id = (Integer) v.getInfo();
                if (!collapsedIds.contains(id))
                    notNeeded.add(id);
            }
            if (notNeeded.size() > 0) {
                collapsedIds.removeAll(notNeeded);
            }
        }
    }

    /**
     * label all nodes in tree that we must keep in induced tree
     *
     * @param v
     * @param ids
     * @param keep
     * @return true, if node v is to be kept
     */
    private boolean labelToKeepRec(Node v, Set<Integer> ids, NodeSet keep) {
        boolean hasBelow = false;

        int id = (Integer) v.getInfo();
        if (ids.size() == 0 || ids.contains(id))
            hasBelow = true;

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (labelToKeepRec(w, ids, keep))
                hasBelow = true;
        }
        if (hasBelow)
            keep.add(v);
        return hasBelow;
    }

    /**
     * induce the tree
     *
     * @param v
     * @param vCpy
     * @param keep
     * @param stopIds   don't induce below these ids
     * @param node2copy
     */
    private void induceRec(Node v, Node vCpy, PhyloTree treeCpy, NodeSet keep, Set<Integer> stopIds, Map<Integer, NodeData> id2data, Map<Node, Node> node2copy) {
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (keep.contains(w)) {
                int id = (Integer) w.getInfo();
                Node wCpy = null;
                if (node2copy != null)
                    wCpy = node2copy.get(w);
                if (wCpy == null) {
                    wCpy = treeCpy.newNode();
                    if (node2copy != null)
                        node2copy.put(w, wCpy);
                    wCpy.setInfo(id);
                }
                NodeData nodeData = id2data.get(id);
                wCpy.setData(Objects.requireNonNullElseGet(nodeData, () -> new NodeData(new float[0], new float[0])));

                treeCpy.newEdge(vCpy, wCpy);

                if (wCpy.getInDegree() > 1) {
                    for (Edge f = wCpy.getFirstInEdge(); f != null; f = wCpy.getNextInEdge(f))
                        treeCpy.setSpecial(f, true);
                }
                if (!stopIds.contains(w.getInfo()))
                    induceRec(w, wCpy, treeCpy, keep, stopIds, id2data, node2copy);
            }
        }
    }

    /**
     * modify the given set of collapsedIds so that the toUncollapse ids are uncollapsed
     *
     * @param toUncollapse
     * @param collapsedIds
     */
    public void uncollapse(Set<Integer> toUncollapse, Set<Integer> collapsedIds) {
        Set<Integer> newCollapsed = new HashSet<>();

        for (Node v = getFirstNode(); v != null; v = v.getNext()) {
            int vId = (Integer) v.getInfo();
            if (toUncollapse.contains(vId) && collapsedIds.contains(vId)) {
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    Node w = e.getTarget();
                    newCollapsed.add((Integer) w.getInfo());
                }
            }
        }
        collapsedIds.removeAll(toUncollapse);
        collapsedIds.addAll(newCollapsed);
    }

    /**
     * computes the id2data map
     *
     * @param id2counts
     */
    public void computeId2Data(int numberOfDatasets, Map<Integer, float[]> id2counts, Map<Integer, NodeData> id2data) {
        id2data.clear();
        if (id2counts != null) {
            if (ClassificationManager.isTaxonomy(getName()))
                computeTaxonomyId2DataRec(numberOfDatasets, getRoot(), id2counts, id2data);
            else
                computeId2DataRec(numberOfDatasets, getRoot(), id2counts, new HashMap<>(), id2data);
        }
    }

    /**
     * recursively computes the classification-id 2 assigned and classification-id 2 summarized maps.
     * Use this when classification contains same leaves more than once
     *
     * @param numberOfDataSets
     * @param v
     * @param id2counts
     * @param id2idsBelow
     * @return set of classification-ids on or below node v
     */
    private Set<Integer> computeId2DataRec(int numberOfDataSets, Node v, Map<Integer, float[]> id2counts, Map<Integer, Set<Integer>> id2idsBelow, Map<Integer, NodeData> id2data) {
        final int id = (Integer) v.getInfo();

        final Set<Integer> idsBelow = new HashSet<>();
        id2idsBelow.put(id, idsBelow);

        idsBelow.add(id);

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            final Set<Integer> allBelow = computeId2DataRec(numberOfDataSets, w, id2counts, id2idsBelow, id2data);
            idsBelow.addAll(allBelow);
        }

        float[] assigned = new float[numberOfDataSets];
        float[] summarized = new float[numberOfDataSets];
        long total = 0;

        final float[] counts = id2counts.get(id);
        if (counts != null) {
            int top = Math.min(assigned.length, counts.length);
            for (int i = 0; i < top; i++) {
                assigned[i] = counts[i];
                total += counts[i];
            }
        }

        for (Integer below : id2idsBelow.get(id)) {
            float[] countBelow = id2counts.get(below);
            if (countBelow != null) {
                int top = Math.min(summarized.length, countBelow.length);
                for (int i = 0; i < top; i++) {
                    summarized[i] += countBelow[i];
                    total += countBelow[i];
                }
            }
        }
        if (total > 0)
            id2data.put(id, new NodeData(assigned, summarized));
        return idsBelow;
    }

    public Set<Integer> getIds() {
        return id2Node.keySet();
    }

    /**
     * recursively computes the taxon-id 2 assigned and taxon-id 2 summarized maps
     *
     * @param numberOfDataSets
     * @param v
     * @param id2counts
     * @return set of f-ids on or below node v
     */
    private Set<Integer> computeTaxonomyId2DataRec(int numberOfDataSets, Node v, Map<Integer, float[]> id2counts, Map<Integer, NodeData> id2data) {
        int taxonomyId = (Integer) v.getInfo();

        // first process all children
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            computeTaxonomyId2DataRec(numberOfDataSets, w, id2counts, id2data);
        }

        // set up assigned:
        float[] assigned = new float[numberOfDataSets];
        float[] summarized = new float[numberOfDataSets];
        long total = 0;

        float[] counts = id2counts.get(taxonomyId);
        if (counts != null) {
            int top = Math.min(assigned.length, counts.length);
            for (int i = 0; i < top; i++) {
                assigned[i] = counts[i];
                summarized[i] = counts[i];
                total += counts[i];
            }
        }

        // setup summarized:

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            int wId = (Integer) w.getInfo();
            NodeData dataW = id2data.get(wId);
            if (dataW != null) {
                float[] below = dataW.getSummarized();
                int top = Math.min(summarized.length, below.length);
                for (int i = 0; i < top; i++) {
                    summarized[i] += below[i];
                    total += below[i];
                }
            }
        }

        if (total > 0)
            id2data.put(taxonomyId, new NodeData(assigned, summarized));
        return null;
    }


    /**
     * get all descendants of a id (including the id itself
     *
     * @param id
     * @return all descendant ids
     */
    public Set<Integer> getAllDescendants(int id) {
        final Set<Integer> set = new HashSet<>();
        set.add(id);
        return getAllDescendants(set);
    }

    /**
     * does the node contain this id
     *
     * @param id
     * @return true, if id is contained
     */
    private boolean containsId(int id) {
        return id2Node.containsKey(id);
    }

    /**
     * get all descendants of a set of f ids (including the ids themselves
     *
     * @param ids
     * @return ids and all descendants
     */
    public Set<Integer> getAllDescendants(Set<Integer> ids) {
        final Set<Integer> set = new HashSet<>(ids);
        getAllDescendantsRec(getRoot(), false, set);
        return set;
    }

    /**
     * recursively add all descendants to a set
     *
     * @param v
     * @param add
     * @param set
     */
    private void getAllDescendantsRec(Node v, boolean add, Set<Integer> set) {
        if (!add && set.contains(v.getInfo()))
            add = true;
        else if (add)
            set.add((Integer) v.getInfo());
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            getAllDescendantsRec(e.getTarget(), add, set);
        }
    }

    /**
     * get all parents for a given id. There can be more than one parent because an id can appear on more than one node
     *
     * @param classId
     * @return all ids of all parents
     */
    public Set<Integer> getAllParents(int classId) {
        final Set<Integer> set = new HashSet<>();
        for (Node v : getNodes(classId)) {
            if (v.getInDegree() > 0) {
                final Node w = v.getFirstInEdge().getSource();
                set.add((Integer) w.getInfo());
            }
        }
        return set;
    }

    /**
     * returns the child of classV that is above classW
     *
     * @param classV
     * @param classW
     * @return child of classV that is above classW, or 0, if not found
     */
    public int getChildAbove(int classV, int classW) {
        if (classV == classW)
            return classV;

        final Node v = getANode(classV);
        if (v != null) {
            for (Node w : getNodes(classW)) {
                while (w.getInDegree() > 0) {
                    final Node u = w.getFirstInEdge().getSource();
                    if (u == v)
                        return (int) w.getInfo();
                    else
                        w = u;
                }
            }
        }
        return 0;
    }


    /**
     * gets all nodes associated with a given f id
     *
     * @param id
     * @return nodes
     */
    public Set<Node> getNodes(int id) {
        Set<Node> set = id2Nodes.get(id);
        if (set == null) {
            set = new HashSet<>();
            id2Nodes.put(id, set);
            Node v = id2Node.get(id); // there is only one node associated with this id, make a set and save it for repeated use
            if (v != null) {
                set.add(v);
            }
        }
        return set;
    }

    /**
     * get a node associated with this id
     *
     * @param id
     * @return a node
     */
    public Node getANode(int id) {
        return id2Node.get(id);
    }

    public void clearId2Node(int id) {
        id2Nodes.remove(id);
        id2Node.remove(id);
    }

    public void addId2Node(int id, Node v) {
        if (id2Node.get(id) == null) {
            id2Node.put(id, v);
        } else if (id2Nodes.get(id) == null) {
            final Set<Node> set = new HashSet<>();
            set.add(id2Node.get(id));
            set.add(v);
            id2Nodes.put(id, set);
        } else
            id2Nodes.get(id).add(v);
    }

    /**
     * gets the LCA of a set of ids
     *
     * @param ids
     * @return LCA
     */
    public Integer getLCA(Set<Integer> ids) {
        final Set<String> addresses = new HashSet<>();
        for (Integer id : ids) {
            String address = id2Address.get(id);
            if (address != null)
                addresses.add(address);
        }
        String prefix = LCAAddressing.getCommonPrefix(addresses, true);
        return address2Id.get(prefix);
    }

    /**
     * is the class below a descendant of the class above?
     *
     * @param idAbove
     * @param idBelow
     * @return true, if idAbove an ancestor of idBelow
     */
    public boolean isDescendant(Integer idAbove, Integer idBelow) {
        String addressAbove = id2Address.get(idAbove);
        String addressBelow = id2Address.get(idBelow);
        if (addressAbove != null && addressBelow != null)
            return id2Address.get(idBelow).startsWith(id2Address.get(idAbove));
        else {
            Set<Node> nodesAbove = id2Nodes.get(idAbove);
            Set<Node> nodesBelow = id2Nodes.get(idBelow);
            if (nodesAbove != null && nodesBelow != null) {
                for (Node w : nodesBelow) {
                    while (true) {
                        if (nodesAbove.contains(w))
                            return true;
                        if (w.getInDegree() == 0)
                            break;
                        w = w.getFirstInEdge().getSource();
                    }
                }
            }
            return false;
        }
    }

    /**
     * gets the address for an id
     *
     * @param id
     * @return address
     */
    public String getAddress(int id) {
        return id2Address.get(id);
    }

    /**
     * gets the id for an address
     *
     * @param address
     * @return id
     */
    public int getAddress2Id(String address) {
        Integer id = address2Id.get(address);
        return Objects.requireNonNullElse(id, 0);
    }

    /**
     * get all nodes at the given level (i.e. distance from root)
     *
     * @param level
     * @return set of nodes
     */
    public Set<Integer> getAllAtLevel(int level) {
        final Set<Integer> result = new HashSet<>();
        getAllAtLevelRec(getRoot(), 0, level, result);
        return result;
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param current
     * @param level
     * @param result
     * @return number added
     */
    private void getAllAtLevelRec(Node v, int current, int level, Set<Integer> result) {
        if (current == level) {
            result.add((Integer) v.getInfo());
        } else {
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                getAllAtLevelRec(e.getTarget(), current + 1, level, result);
            }
        }
    }

    /**
     * gets all nodes of given rank
     *
     * @param rank
     * @param closure if true, compute closure
     * @return result
     */
    public Set<Integer> getNodeIdsAtGivenRank(Integer rank, boolean closure) {
        Set<Integer> result = new HashSet<>();
        getNodesAtGivenRankRec(getRoot(), rank, result, closure);
        return result;
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param rank
     * @param result
     * @param closure
     * @return true, if node found below
     */
    private boolean getNodesAtGivenRankRec(final Node v, final Integer rank, final Set<Integer> result, final boolean closure) {
        if (name2IdMap.getRank((Integer) v.getInfo()) == rank) {
            result.add((Integer) v.getInfo());
            return true;
        }

        // are  some, but not all, of the children not involved? If so, add those to the result (to collapse them)
        int noneBelowCount = 0;
        final Node[] noneBelow = new Node[v.getOutDegree()];

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            if (!getNodesAtGivenRankRec(e.getTarget(), rank, result, closure))
                noneBelow[noneBelowCount++] = e.getTarget();
        }

        if (closure && noneBelowCount > 0 && noneBelowCount < noneBelow.length) {
            for (int i = 0; i < noneBelowCount; i++)
                result.add((Integer) noneBelow[i].getInfo());
        }

        return noneBelowCount < noneBelow.length;
    }

}
