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
package megan.clusteranalysis.gui;

import jloda.swing.find.IObjectSearcher;
import jloda.swing.find.TableSearcher;
import megan.clusteranalysis.tree.Distances;
import megan.clusteranalysis.tree.Taxa;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Set;

/**
 * tab that shows the data matrix
 * Daniel Huson, 5.2010
 */
public class MatrixTab extends JPanel {
    private final JScrollPane scrollPane;
    private final JPanel contentPanel;
    private JTable table;

    private final IObjectSearcher searcher;

    public MatrixTab(JFrame frame) {
        setLayout(new BorderLayout());
        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        setData(null, null);   // adds empty table to panel

        searcher = new TableSearcher(frame, "Matrix", getTable());

    }

    /**
     * set the data
     *
     * @param taxa
     * @param distances
     */
    public void setData(Taxa taxa, Distances distances) {
        contentPanel.removeAll();
        if (taxa != null && distances != null)
            table = new JTable(new MyTableModel(taxa, distances));
        else
            table = new JTable();

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setShowGrid(true);
        table.setDragEnabled(false);
        table.removeEditor();
        table.setCellSelectionEnabled(true);

        table.getTableHeader().setReorderingAllowed(false);
        contentPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
        contentPanel.add(table, BorderLayout.CENTER);
        scrollPane.revalidate();
    }

    public void selectByLabels(Set<String> toSelect) {
        System.err.println("Select From Previous: not implemented for MatrixTab");
    }

    static class MyTableModel extends AbstractTableModel {
        private final Taxa taxa;
        private final Distances distances;

        MyTableModel(Taxa taxa, Distances distances) {
            this.taxa = taxa;
            this.distances = distances;
        }

        public String getColumnName(int col) {
            if (col == 0)
                return "[Dataset]";
            else
                return "[" + (col) + "] " + taxa.getLabel(col);
        }

        public int getRowCount() {
            return distances.getNtax();
        }

        public int getColumnCount() {
            return distances.getNtax() + 1;
        }

        public Object getValueAt(int row, int col) {
            if (col == 0)
                return "[" + (row + 1) + "] " + taxa.getLabel(row + 1);
            else {
                return (float) distances.get(row + 1, col);

            }
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public void setValueAt(Object value, int row, int col) {
            if (col > 0) {
                distances.set(row + 1, col, Double.parseDouble(value.toString()));
                fireTableCellUpdated(row, col);
            }
        }
    }

    public JTable getTable() {
        return table;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public String getLabel() {
        return "Matrix";
    }

    public IObjectSearcher getSearcher() {
        return searcher;
    }
}

