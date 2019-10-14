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

package megan.viewer.gui;

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.util.ProgramProperties;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * tree cell Renderer
 * Daniel Huson, March 2016
 */
public class MyJTreeCellRender implements TreeCellRenderer {
    private final ClassificationViewer classificationViewer;
    private final Map<Integer, Set<Node>> id2NodesInInducedTree;

    private final JLabel label = new JLabel();
    private final LineBorder selectedBorder = (LineBorder) BorderFactory.createLineBorder(ProgramProperties.SELECTION_COLOR_DARKER);

    private Function<Integer,ImageIcon>  iconProducer = null;

    /**
     * constructor
     *
     * @param classificationViewer
     * @param id2NodesInInducedTree
     */
    public MyJTreeCellRender(ClassificationViewer classificationViewer, Map<Integer, Set<Node>> id2NodesInInducedTree) {
        this.classificationViewer = classificationViewer;
        this.id2NodesInInducedTree = id2NodesInInducedTree;
        label.setOpaque(true);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof ViewerJTree.MyJTreeNode) {
            final ViewerJTree.MyJTreeNode jNode = (ViewerJTree.MyJTreeNode) value;
            final Node v = jNode.getV(); // node in full tree tree
            final Integer classId = (Integer) v.getInfo();
            float count = 0;
            Set<Node> inducedNodes = id2NodesInInducedTree.get(classId);
            if (inducedNodes != null && inducedNodes.size() > 0) {
                final NodeData nodeData = (NodeData) inducedNodes.iterator().next().getData();
                if (nodeData != null)
                    count = nodeData.getCountSummarized();
            }

            final String name = (classificationViewer.getClassification().getName2IdMap().get((Integer) v.getInfo()));

            if (count > 0) {
                label.setText(String.format("<html>%s<font color=#a0a0a0> (%,.0f)</font>", name, count));
                label.setForeground(Color.BLACK);
            } else {
                label.setForeground(Color.LIGHT_GRAY);
                label.setText(name);
            }

            selected = classificationViewer.getSelectedNodeIds().contains(v.getInfo());

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
            if (iconProducer != null)
                label.setIcon(iconProducer.apply(classId));
        } else {
            label.setText(value.toString());
        }
        label.setPreferredSize(new Dimension(label.getPreferredSize().width + 100, label.getPreferredSize().height));
        label.setMinimumSize(label.getPreferredSize());
        label.validate();
        label.setToolTipText(label.getText());

        return label;
    }

    public void setIconProducer(Function<Integer,ImageIcon> iconProducer) {
        this.iconProducer = iconProducer;
    }
}
