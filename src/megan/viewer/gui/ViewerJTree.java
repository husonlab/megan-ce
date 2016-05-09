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

package megan.viewer.gui;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.gui.IPopupMenuModifier;
import jloda.util.ProgramProperties;
import megan.viewer.ClassificationViewer;
import megan.viewer.GUIConfiguration;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * tree viewer for classification
 * Created by huson on 2/3/16.
 */
public class ViewerJTree extends JTree {
    private final ClassificationViewer classificationViewer;
    private final Map<Integer, MyJTreeNode> id2node = new HashMap<>();
    private IPopupMenuModifier popupMenuModifier;

    private final JPopupMenu popupMenu;

    boolean inSelection = false;  // use this to prevent bouncing when selecting from viewer

    /**
     * constructor
     *
     * @param classificationViewer
     */
    public ViewerJTree(ClassificationViewer classificationViewer) {
        this.classificationViewer = classificationViewer;
        setCellRenderer(new MyJTreeCellRender(classificationViewer));

        addTreeSelectionListener(new MyJTreeSelectionListener(this, classificationViewer));

        final MyJTreeListener treeListener = new MyJTreeListener(this, classificationViewer, id2node);
        addTreeWillExpandListener(treeListener);
        addTreeExpansionListener(treeListener);
        addMouseListener(new MyMouseListener());

        popupMenu = new jloda.gui.PopupMenu(GUIConfiguration.getJTreePopupConfiguration(), classificationViewer.getCommandManager(), false);
    }

    /**
     * rescan the jtree
     */
    public void update() {
        if (classificationViewer.getTree().getNumberOfNodes() > 1) {
            removeAll();
            id2node.clear();
            Node root = classificationViewer.getClassification().getFullTree().getRoot();
            int id = (Integer) root.getInfo();
            final MyJTreeNode node = new MyJTreeNode(root);
            final DefaultTreeModel model = (DefaultTreeModel) getModel();
            model.setRoot(node);
            id2node.put(id, node);
            setRootVisible(true);
            setShowsRootHandles(true);
            addChildren(node);
        }
    }

    /**
     * add all children of a given node
     *
     * @param node
     */
    public void addChildren(MyJTreeNode node) {
        final Node v = node.getV();
        final DefaultTreeModel model = (DefaultTreeModel) getModel();

        if (v.getOutDegree() > 0 && node.getChildCount() == 0) {
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                final Node w = e.getTarget();
                final MyJTreeNode wNode = new MyJTreeNode(w);
                node.add(wNode);
                id2node.put((Integer) w.getInfo(), wNode);
                model.nodeStructureChanged(wNode);
            }
        }
        model.nodeStructureChanged(node);
    }

    public boolean isInSelection() {
        return inSelection;
    }

    /**
     * select a node by id
     *
     * @param id
     */
    public void setSelected(int id, boolean select) {
        if (!inSelection) {
            inSelection = true;
            DefaultMutableTreeNode n = id2node.get(id);
            if (n != null) {
                TreePath path = new TreePath(n);
                if (select)
                    setSelectionPath(path);
                else
                    removeSelectionPath(path);
                makeVisible(path);
                repaint();
            }
            inSelection = false;
        }
    }

    /**
     * select nodes by their ids
     *
     * @param ids
     */
    public void setSelected(Collection<Integer> ids, boolean select) {
        if (!inSelection) {
            inSelection = true;
            LinkedList<TreePath> paths = new LinkedList<>();

            for (Integer id : ids) {
                DefaultMutableTreeNode n = id2node.get(id);
                if (n != null) {
                    TreePath path = new TreePath(n);
                    paths.add(path);
                    if (paths.size() == 1)
                        makeVisible(path);
                }
            }
            if (select)
                setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
            else
                removeSelectionPaths(paths.toArray(new TreePath[paths.size()]));
            repaint();
            inSelection = false;
        }
    }

    public void setPopupMenuModifier(IPopupMenuModifier popupMenuModifier) {
        this.popupMenuModifier = popupMenuModifier;
    }

    class MyMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) showPopupMenu(e);
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) showPopupMenu(e);
        }
    }

    private void showPopupMenu(MouseEvent e) {
        if (popupMenuModifier != null) {
            popupMenuModifier.apply(popupMenu, classificationViewer.getCommandManager());
            popupMenuModifier = null;
        }
        popupMenu.show(ViewerJTree.this, e.getX(), e.getY());
    }

    /**
     * tree node
     */
    public class MyJTreeNode extends DefaultMutableTreeNode {
        private final Node v;

        MyJTreeNode(Node v) {
            this.v = v;
        }

        public Node getV() {
            return v;
        }

        public String toString() {
            return "[" + v.getInfo() + "]";
        }

        @Override
        public boolean isLeaf() {
            return v.getOutDegree() == 0;
        }
    }
}

/**
 * tree cell Renderer
 */
class MyJTreeCellRender implements TreeCellRenderer {
    private final ClassificationViewer classificationViewer;
    private final JLabel label = new JLabel();
    private final LineBorder selectedBorder = (LineBorder) BorderFactory.createLineBorder(ProgramProperties.SELECTION_COLOR_DARKER);

    public MyJTreeCellRender(ClassificationViewer classificationViewer) {
        this.classificationViewer = classificationViewer;
        label.setOpaque(true);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof ViewerJTree.MyJTreeNode) {
            final ViewerJTree.MyJTreeNode jNode = (ViewerJTree.MyJTreeNode) value;
            final Node v = jNode.getV(); // node in full tree
            final Integer classId = (Integer) v.getInfo();
            int count = 0;
            final Node w = classificationViewer.getANode(classId); // a node in the tree
            if (w != null) {
                final NodeData nodeData = classificationViewer.getNodeData(w);
                if (nodeData != null)
                    count = nodeData.getCountSummarized();
            }
            final String name = (classificationViewer.getClassification().getName2IdMap().get((Integer) v.getInfo()));

            if (count > 0) {
                label.setText(String.format("<html>%s<font color=#a0a0a0> (%,d)</font>", name, count));
                label.setForeground(Color.BLACK);
            } else {
                label.setForeground(Color.LIGHT_GRAY);
                label.setText(name);
            }

            selected = classificationViewer.getSelectedNodeIds().contains((Integer) v.getInfo());

            if (selected) {
                label.setBackground(ProgramProperties.SELECTION_COLOR);
                label.setBorder(selectedBorder);
            } else if (hasFocus) {
                label.setBackground(Color.WHITE);
                label.setBorder(selectedBorder);
            } else {
                label.setBackground(Color.WHITE);
                label.setBorder(null);
            }
        } else {
            label.setText(value.toString());
        }
        label.setPreferredSize(new Dimension(label.getPreferredSize().width + 100, label.getPreferredSize().height));
        label.setMinimumSize(label.getPreferredSize());
        label.validate();
        label.setToolTipText(label.getText());
        return label;
    }
}

class MyJTreeListener implements TreeWillExpandListener, TreeExpansionListener {
    private final ViewerJTree jTree;
    private final ClassificationViewer classificationViewer;
    private final Map<Integer, ViewerJTree.MyJTreeNode> id2node;

    /**
     * constructor
     *
     * @param classificationViewer
     */
    MyJTreeListener(ViewerJTree jTree, ClassificationViewer classificationViewer, Map<Integer, ViewerJTree.MyJTreeNode> id2node) {
        this.jTree = jTree;
        this.classificationViewer = classificationViewer;
        this.id2node = id2node;
    }

    /**
     * Invoked whenever a node in the tree is about to be collapsed.
     */
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    }

    /**
     * Called whenever an item in the tree has been collapsed.
     */
    public void treeCollapsed(TreeExpansionEvent event) {
    }

    /**
     * Called whenever an item in the tree has been expanded.
     */
    public void treeExpanded(TreeExpansionEvent event) {
    }

    /**
     * Invoked whenever a node in the tree is about to be expanded.
     */
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        if (classificationViewer.isLocked()) {
            throw new ExpandVetoException(event);
        }
        jTree.addChildren((ViewerJTree.MyJTreeNode) event.getPath().getLastPathComponent());
    }
}

class MyJTreeSelectionListener implements TreeSelectionListener {
    private final ClassificationViewer ClassificationViewer;
    private final ViewerJTree jtree;

    public MyJTreeSelectionListener(ViewerJTree jtree, ClassificationViewer ClassificationViewer) {
        this.jtree = jtree;
        this.ClassificationViewer = ClassificationViewer;
    }

    /**
     * Called whenever the value of the selection changes.
     *
     * @param e the event that characterizes the replace.
     */
    public void valueChanged(TreeSelectionEvent e) {
        if (!jtree.inSelection) {
            jtree.inSelection = true;
            Set<Integer> ids2Select = new HashSet<>();
            Set<Integer> ids2Deselect = new HashSet<>();

            for (TreePath path : e.getPaths()) {
                final ViewerJTree.MyJTreeNode node = (ViewerJTree.MyJTreeNode) path.getLastPathComponent();
                if (e.isAddedPath(path))
                    ids2Select.add((Integer) node.getV().getInfo());
                else
                    ids2Deselect.add((Integer) node.getV().getInfo());
            }
            if (ids2Select.size() > 0 || ids2Deselect.size() > 0) {
                if (ids2Deselect.size() > 0)
                    ClassificationViewer.setSelectedIds(ids2Deselect, false);
                if (ids2Select.size() > 0) {
                    ClassificationViewer.setSelectedIds(ids2Select, true);
                    Node v = ClassificationViewer.getANode(ids2Select.iterator().next());
                    if (v != null)
                        ClassificationViewer.scrollToNode(v);
                }
                ClassificationViewer.repaint();
            }
            jtree.inSelection = false;
        }
    }
}