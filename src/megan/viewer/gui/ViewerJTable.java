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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.swing.window.IPopupMenuModifier;
import jloda.swing.util.PopupMenu;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.viewer.ClassificationViewer;
import megan.viewer.GUIConfiguration;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * jtable representation of the data at leaves of tree
 * Daniel Huson, 10.2010
 */
public class ViewerJTable extends JTable {
    private final ClassificationViewer classificationViewer;
    private final DefaultTableModel model;
    private final MyCellRender cellRenderer;
    private final Map<Integer, Integer> id2row = new HashMap<>();
    private final JPopupMenu popupMenu;
    private IPopupMenuModifier popupMenuModifier;

    private boolean inSelection = false;  // use this to prevent bouncing when selecting from viewer

    /**
     * constructor
     *
     * @param classificationViewer
     */
    public ViewerJTable(ClassificationViewer classificationViewer) {
        this.classificationViewer = classificationViewer;

        model = new DefaultTableModel() {
            public Class getColumnClass(int col) {
                if (col >= 0 && col < getColumnCount()) {
                    if (col == 0)
                        return Pair.class;
                    else
                        return Integer.class;
                } else
                    return Object.class;
            }
        };

        setModel(model);
        addMouseListener(new MyMouseListener());
        getSelectionModel().addListSelectionListener(new MyListSelectionListener());
        cellRenderer = new MyCellRender();

        getTableHeader().setReorderingAllowed(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setShowGrid(false);
        setRowSorter(new TableRowSorter<TableModel>(model));

        popupMenu = new PopupMenu(this, GUIConfiguration.getJTablePopupConfiguration(), classificationViewer.getCommandManager());


        // ToolTipManager.sharedInstance().unregisterComponent(this);

        // rescan();
    }

    /**
     * rescan the heatmap
     */
    public void update() {
        clear();
        // setup column names
        final String[] columnNames = new String[classificationViewer.getNumberOfDatasets() + 1];
        columnNames[0] = classificationViewer.getClassName();
        for (int i = 1; i <= classificationViewer.getNumberOfDatasets(); i++) {
            columnNames[i] = "Reads [" + i + "]";
        }
        model.setColumnIdentifiers(columnNames);
        // setup cellRenderer:
        for (int i = 0; i < getColumnCount(); i++) {
            TableColumn col = getColumnModel().getColumn(i);
            col.setCellRenderer(cellRenderer);
        }

        if (classificationViewer.getTree().getRoot() != null) {
            buildHeatMapRec(classificationViewer.getTree().getRoot(), new HashSet<>());
        }

        float[] maxCounts = new float[classificationViewer.getNumberOfDatasets()];

        for (Node v = classificationViewer.getTree().getFirstNode(); v != null; v = v.getNext()) {
            if (v.getOutDegree() == 0) {
                NodeData data = classificationViewer.getNodeData(v);
                if (data != null) {
                    float[] summarized = data.getSummarized();
                    int top = Math.min(summarized.length, maxCounts.length);
                    for (int i = 0; i < top; i++) {
                        maxCounts[i] = Math.max(maxCounts[i], summarized[i]);
                    }
                }
            }
        }
        cellRenderer.setMaxCounts(maxCounts);
    }

    /**
     * recursively build the table
     *
     * @param v
     * @return total count so far
     */
    private void buildHeatMapRec(Node v, HashSet<Integer> seen) {
        if (v.getOutDegree() == 0) {
            NodeData data = classificationViewer.getNodeData(v);
            if (data != null) {
                float[] summarized = data.getSummarized();
                Comparable[] rowData = new Comparable[summarized.length + 1];
                int id = (Integer) v.getInfo();
                if (!seen.contains(id)) {
                    seen.add(id);
                    String name = classificationViewer.getClassification().getName2IdMap().get(id);
                    rowData[0] = new Pair<>(name, id) {
                        public String toString() {
                            return getFirst();
                        }
                    };
                    for (int i = 0; i < summarized.length; i++) {
                        rowData[i + 1] = summarized[i];
                    }
                    id2row.put(id, model.getRowCount());
                    model.addRow(rowData);
                }
            }
        } else {
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                buildHeatMapRec(w, seen);
            }
        }
    }

    /**
     * erase the table
     */
    private void clear() {
        id2row.clear();
        clearSelection();
        while (model.getRowCount() > 0)
            model.removeRow(model.getRowCount() - 1);
        model.getDataVector().clear();
    }

    /**
     * don't allow editing of anything
     *
     * @param row
     * @param column
     * @return false
     */
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public boolean isInSelection() {
        return inSelection;
    }

    /**
     * select nodes by their ids
     *
     * @param ids
     */
    public void setSelected(Collection<Integer> ids, boolean select) {
        if (!inSelection) {
            inSelection = true;

            int first = -1;
            for (Integer id : ids) {
                Integer row = id2row.get(id);
                if (row != null) {
                    row = convertRowIndexToView(row);
                    if (first == -1)
                        first = row;
                    if (select)
                        addRowSelectionInterval(row, row);
                    else
                        removeRowSelectionInterval(row, row);
                }
            }
            if (first != -1) {
                final int firstf = first;
                final Runnable runnable = () -> scrollRectToVisible(new Rectangle(getCellRect(firstf, 0, true)));
                if (SwingUtilities.isEventDispatchThread())
                    runnable.run();
                else
                    SwingUtilities.invokeLater(runnable);
            }
        }
        inSelection = false;
    }

    class MyMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                try {
                    classificationViewer.getDir().executeImmediately("zoom full;zoom selected;", classificationViewer.getCommandManager());
                } catch (Exception e1) {
                    Basic.caught(e1);
                }
            }
        }

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }
    }

    private void showPopupMenu(MouseEvent e) {
        if (popupMenuModifier != null) {
            popupMenuModifier.apply(popupMenu, classificationViewer.getCommandManager());
            popupMenuModifier = null;
        }
        popupMenu.show(ViewerJTable.this, e.getX(), e.getY());
    }

    /**
     * selection listener
     */
    class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!inSelection) {
                inSelection = true;
                if (!classificationViewer.isLocked()) {
                    if (!e.getValueIsAdjusting()) {
                        boolean originallyEmptySelectionInViewer = classificationViewer.getSelectedNodes().isEmpty();
                        classificationViewer.selectAllNodes(false);
                        classificationViewer.selectAllEdges(false);
                        // selection event
                        if (getSelectedRowCount() > 0) {
                            int[] selectedRowIndices = getSelectedRows();
                            // first deselect all nodes and edges

                            for (int row : selectedRowIndices) {
                                row = convertRowIndexToModel(row);
                                int col = convertColumnIndexToModel(0);
                                if (row > model.getDataVector().size() - 1) {
                                    return;
                                }
                                if (getModel().getValueAt(row, 0) != null) {
                                    int id = ((Pair<String, Integer>) getModel().getValueAt(row, col)).getSecond();
                                    if (classificationViewer.getNodes(id) != null) {
                                        for (Node v : classificationViewer.getNodes(id)) {
                                            classificationViewer.setSelected(v, true);
                                            classificationViewer.repaint();
                                        }
                                    }
                                }
                            }
                            if (originallyEmptySelectionInViewer != classificationViewer.getSelectedNodes().isEmpty())
                                classificationViewer.getCommandManager().updateEnableState();
                        }
                    }
                }
                inSelection = false;
            }
        }
    }

    public IPopupMenuModifier getPopupMenuModifier() {
        return popupMenuModifier;
    }

    public void setPopupMenuModifier(IPopupMenuModifier popupMenuModifier) {
        this.popupMenuModifier = popupMenuModifier;
    }
}

class MyCellRender implements TableCellRenderer {
    private GreenGradient[] greenGradients;

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        // in case of reorder columns:
        col = table.convertColumnIndexToModel(col);
        if (col == 0) {
            Pair<String, Integer> pair = (Pair<String, Integer>) value;
            if (pair == null)
                return new JLabel("");
            JLabel label = new JLabel(pair.getFirst());
            if (isSelected)
                label.setBorder(BorderFactory.createLineBorder(ProgramProperties.SELECTION_COLOR_DARKER));
            else
                label.setBorder(null);

            label.setPreferredSize(new Dimension(label.getPreferredSize().width + 100, label.getPreferredSize().height));
            label.setMinimumSize(label.getPreferredSize());

            return label;

        } else {
            int number;

            try {
                number = Math.round(Float.parseFloat(String.valueOf(value)));
            } catch (NumberFormatException nfe) {
                return new JLabel("?");
            }

            JLabel label = new JLabel(String.valueOf(number));
            label.setFont(new Font("SansSerif", Font.PLAIN, 11));
            try {
                if (greenGradients != null && greenGradients[col - 1] != null) {
                    Color color = greenGradients[col - 1].getLogColor(number);
                    label.setBackground(color);
                    if (color.getRed() + color.getGreen() + color.getBlue() < 250)
                        label.setForeground(Color.WHITE);
                }
            } catch (IllegalArgumentException iae) {
                // -1 ?
            }
            label.setOpaque(true);
            if (isSelected)
                label.setBorder(BorderFactory.createLineBorder(ProgramProperties.SELECTION_COLOR));
            else if (hasFocus)
                label.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            else
                label.setBorder(null);
            label.setToolTipText(label.getText());
            return label;
        }
    }

    public void setMaxCounts(float[] maxCounts) {
        greenGradients = new GreenGradient[maxCounts.length];
        for (int i = 0; i < maxCounts.length; i++)
            greenGradients[i] = new GreenGradient(Math.round(maxCounts[i]));
    }

}





