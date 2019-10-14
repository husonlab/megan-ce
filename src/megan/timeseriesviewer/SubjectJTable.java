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

package megan.timeseriesviewer;

import jloda.swing.director.IDirector;
import jloda.swing.util.PopupMenu;
import megan.core.Director;
import megan.core.Document;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * the table allowing arrangement of series and time points
 */
public class SubjectJTable {
    private final TimeSeriesViewer viewer;
    private final Director dir;
    private final Document doc;
    private final JTable jTable;

    private final MyTableModel tableModel;
    private final MyCellRenderer renderer;

    private final static Color selectionBackgroundColor = new Color(255, 240, 240);

    /**
     * Constructor
     *
     * @param viewer
     */
    public SubjectJTable(TimeSeriesViewer viewer) {
        this.viewer = viewer;
        this.dir = viewer.getDirector();
        this.doc = dir.getDocument();
        jTable = new JTable();
        renderer = new MyCellRenderer();

        tableModel = new MyTableModel();
        setDefault();
        jTable.setModel(tableModel);

        jTable.createDefaultColumnsFromModel();
        jTable.setColumnSelectionAllowed(false);
        jTable.setRowSelectionAllowed(true);
        jTable.setCellSelectionEnabled(true);
        jTable.setGridColor(Color.LIGHT_GRAY);
        jTable.setShowGrid(true);
        jTable.addMouseListener(new MyMouseListener());

        jTable.getTableHeader().setReorderingAllowed(false);
        jTable.getTableHeader().addMouseListener(new MyMouseListener());

        // setRowSorter(new TableRowSorter<DefaultTableModel>());

        MySelectionListener mySelectionListener = new MySelectionListener();
        jTable.getSelectionModel().addListSelectionListener(mySelectionListener);
        jTable.getColumnModel().getSelectionModel().addListSelectionListener(mySelectionListener);
    }

    public void setDefault() {
        int rows = (int) Math.sqrt(doc.getNumberOfSamples());

        tableModel.resize(rows);
        tableModel.setColumnName(0, "Subject");
        for (int row = 0; row < rows; row++) {
            tableModel.put(row, "" + row);
        }
    }

    public void setRows(int rows) {
        tableModel.resize(rows);
        tableModel.setColumnName(0, "Subject");
        for (int row = 0; row < rows; row++) {
            tableModel.put(row, "" + row);
        }
    }

    /**
     * get the table
     *
     * @return table
     */
    public JTable getJTable() {
        return jTable;
    }

    public Font getFont() {
        return jTable.getFont();
    }

    public void updateView() {
        jTable.createDefaultColumnsFromModel();

        for (int i = 0; i < jTable.getColumnCount(); i++) {
            TableColumn col = jTable.getColumnModel().getColumn(i);
            col.setCellRenderer(renderer);
        }
        jTable.revalidate();
    }


    /**
     * table model
     */
    class MyTableModel extends AbstractTableModel {
        private String columnName = "Subject";
        private DataNode[][] data;

        MyTableModel() {
        }

        void resize(int rows) {
            data = new DataNode[rows][1];
        }

        void put(int row, String name) {
            data[row][0] = new DataNode(name);
        }

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return (col == 0 ? columnName : "??");
        }

        void setColumnName(int col, String name) {
            if (col == 0)
                columnName = name;
        }

        public Object getValueAt(int row, int col) {
            return (col == 0 ? data[row][col] : null);
        }

        public Class getColumnClass(int c) {
            return (c == 0 ? getValueAt(0, c).getClass() : null);
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            return col >= 2;
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                data[row][col] = (DataNode) value;
                fireTableCellUpdated(row, col);
            }
        }
    }

    static class DataNode {
        private final String name;
        private boolean selected;

        DataNode(String name) {
            this.name = name;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String toString() {
            return name;
        }

        String getName() {
            return name;
        }
    }

    class MyCellRenderer implements TableCellRenderer {
        private JPanel panel;
        private JLabel label;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (panel == null) {
                panel = new JPanel();
                panel.setLayout(new BorderLayout());
                label = new JLabel();
                panel.setPreferredSize(new Dimension(label.getPreferredSize().width + 100, label.getPreferredSize().height));
                panel.setMinimumSize(label.getPreferredSize());
                panel.add(label, BorderLayout.CENTER);

            }
            label.setEnabled(isEnabled(row));
            label.setForeground(label.isEnabled() ? Color.BLACK : Color.GRAY);

            DataNode dataNode = (DataNode) value;
            label.setText(dataNode.getName());
            if (isSelected) {
                //label.setBorder(BorderFactory.createLineBorder(Color.RED));
                panel.setBackground(selectionBackgroundColor);
            } else {
                label.setBorder(null);
                panel.setBackground(label.getBackground());
            }


            //label.setBackground(isEnabled(row) ? selectionBackgroundColor : Color.LIGHT_GRAY);
            return panel;
        }

        boolean isEnabled(int modelRow) {
            //String sampleName = tableModel.getValueAt(modelRow, 0).toString();
            return true;
        }
    }

    class MySelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                // The mouse button has not yet been released
            } else {
                //dir.updateView(IDirector.ENABLE_STATE);
                //jTable.repaint();
                viewer.getDataJTable().clearSelection();
                for (int row = 0; row < jTable.getRowCount(); row++) {
                    viewer.getDataJTable().selectRow(row, jTable.isRowSelected(row));
                }
                viewer.getDataJTable().getJTable().repaint();
                viewer.updateView(IDirector.ENABLE_STATE);
            }
        }
    }

    class MyMouseListener extends MouseAdapter {
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

    private void showPopupMenu(final MouseEvent e) {
        // show the header popup menu:
        if (e.getSource() instanceof JTableHeader) {
            (new PopupMenu(this, GUIConfiguration.getSubjectColumnHeaderPopupConfiguration(), dir.getCommandManager())).show(e.getComponent(), e.getX(), e.getY());
        } else if (e.getSource() instanceof JTable && e.getSource() == jTable) {
            (new PopupMenu(this, GUIConfiguration.getSubjectPopupConfiguration(), dir.getCommandManager())).show(e.getComponent(), e.getX(), e.getY());
        }
    }

}
