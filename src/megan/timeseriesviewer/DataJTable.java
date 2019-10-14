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
import megan.chart.ChartColorManager;
import megan.core.Director;
import megan.core.Document;
import megan.core.SampleAttributeTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * the table allowing arrangement of series and time points
 */
public class DataJTable {
    private final TimeSeriesViewer viewer;
    private final Director dir;
    private final Document doc;
    private final ChartColorManager chartColorManager;
    private final JTable jTable;
    private final MyCellRenderer renderer;

    private final MyTableModel tableModel;

    private final Set<String> disabledSamples = new HashSet<>();
    private final Set<String> selectedSamples = new HashSet<>();

    private final static Color selectionBackgroundColor = new Color(255, 240, 240);
    private final static Color backgroundColor = new Color(230, 230, 230);

    private int columnPressed = -1;

    /**
     * Constructor
     *
     * @param viewer
     */
    public DataJTable(TimeSeriesViewer viewer) {
        this.viewer = viewer;
        this.dir = viewer.getDirector();
        this.doc = dir.getDocument();
        this.chartColorManager = doc.getChartColorManager();

        jTable = new JTable();
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        renderer = new MyCellRenderer();

        jTable.setColumnModel(new DefaultTableColumnModel() {
            public void addColumn(TableColumn tc) {
                tc.setMinWidth(150);
                super.addColumn(tc);
            }
        });

        jTable.addMouseListener(new MyMouseListener());
        jTable.getTableHeader().addMouseListener(new MyMouseListener());


        tableModel = new MyTableModel();
        setDefault();
        jTable.setModel(tableModel);

        final MySelectionListener mySelectionListener = new MySelectionListener();
        jTable.getSelectionModel().addListSelectionListener(mySelectionListener);
        jTable.getColumnModel().getSelectionModel().addListSelectionListener(mySelectionListener);
    }

    public void setDefault() {
        int numberOfSamples = doc.getNumberOfSamples();
        int rows = (int) Math.sqrt(numberOfSamples);
        int cols = 0;
        while (rows * cols < numberOfSamples)
            cols++;

        setRowsAndCols(rows, cols);
        String[] sampleNames = doc.getSampleNamesAsArray();
        int count = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (row == 0)
                    setColumnName(col, "Time " + col);
                if (count < numberOfSamples)
                    putCell(row, col, sampleNames[count++]);
                else
                    putCell(row, col, null);
            }
        }
        updateView();
    }

    public void setRowsAndCols(int rows, int cols) {
        tableModel.resize(rows, cols);
    }

    public void putCell(int row, int col, String sample) {
        tableModel.put(row, col, sample);
    }

    public void setColumnName(int col, String name) {
        tableModel.setColumnName(col, name);
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
     * get the table
     *
     * @return table
     */
    public JTable getJTable() {
        return jTable;
    }

    private Font getFont() {
        return jTable.getFont();
    }

    public void selectRow(int row, boolean select) {
        if (row >= 0) {
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                final DataNode dataNode = (DataNode) tableModel.getValueAt(row, col);
                if (dataNode != null) {
                    dataNode.setSelected(select);
                }
            }
        }
    }

    private void selectColumn(int col, boolean select) {
        if (col >= 0) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                final DataNode dataNode = (DataNode) tableModel.getValueAt(row, col);
                if (dataNode != null) {
                    dataNode.setSelected(select);
                }
            }
        }
    }

    public void select(String name, boolean select) {
        for (int row = 0; row < getJTable().getRowCount(); row++) {
            for (int col = 0; col < getJTable().getColumnCount(); col++) {
                final DataNode dataNode = (DataNode) tableModel.getValueAt(row, col);
                if (dataNode != null && dataNode.getName() != null && dataNode.getName().endsWith(name)) {
                    dataNode.setSelected(select);
                }
            }
        }
    }

    public void clearSelection() {
        jTable.clearSelection();
        for (int row = 0; row < getJTable().getRowCount(); row++) {
            for (int col = 0; col < getJTable().getColumnCount(); col++) {
                ((DataNode) tableModel.getValueAt(row, col)).setSelected(false);
            }
        }
    }

    public void selectAll() {
        jTable.selectAll();
        for (int row = 0; row < getJTable().getRowCount(); row++) {
            for (int col = 0; col < getJTable().getColumnCount(); col++) {
                ((DataNode) tableModel.getValueAt(row, col)).setSelected(true);
            }
        }
    }

    public int getColumnPressed() {
        return columnPressed;
    }

    public void setColumnPressed(int columnPressed) {
        this.columnPressed = columnPressed;
    }


    private boolean isEnabled(int modelRow) {
        Object sampleName = jTable.getModel().getValueAt(modelRow, 0);
        return sampleName != null && !disabledSamples.contains(sampleName.toString());
    }

    public String getSampleName(int modelRow) {
        return jTable.getModel().getValueAt(modelRow, 0).toString();
    }

    public void setDisabledSamples(Collection<String> disabledSamples) {
        this.disabledSamples.clear();
        this.disabledSamples.addAll(disabledSamples);
    }

    public Set<String> getDisabledSamples() {
        return disabledSamples;
    }

    public void selectSamples(Collection<String> names, boolean select) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                final DataNode dataNode = (DataNode) tableModel.getValueAt(row, col);
                if (dataNode != null && dataNode.getName() != null && names.contains(dataNode.getName())) {
                    dataNode.setSelected(select);
                }
            }
        }
    }

    public Collection<String> getSelectedSamples() {
        ArrayList<String> selectedSamples = new ArrayList<>(tableModel.getRowCount() * tableModel.getColumnCount());
        int[] columnsInView = getColumnsInView();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col : columnsInView) {
                final DataNode dataNode = (DataNode) tableModel.getValueAt(row, col);
                if (dataNode != null && dataNode.isSelected()) {
                    selectedSamples.add(dataNode.getName());
                }
            }
        }
        return selectedSamples;
    }

    private int[] getColumnsInView() {
        int[] result = new int[jTable.getColumnCount()];

        // Use an enumeration
        Enumeration<TableColumn> e = jTable.getColumnModel().getColumns();
        for (int i = 0; e.hasMoreElements(); i++) {
            result[i] = e.nextElement().getModelIndex();
        }
        return result;
    }


    /**
     * table model
     */
    class MyTableModel extends AbstractTableModel {
        private DataNode[][] data;
        private String[] columnNames;

        MyTableModel() {
        }

        void resize(int rows, int cols) {
            data = new DataNode[rows][cols];
            columnNames = new String[cols];
        }

        void put(int row, int col, String name) {
            data[row][col] = new DataNode(name);
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        void setColumnName(int col, String name) {
            columnNames[col] = name;
        }

        public Object getValueAt(int row, int col) {
            if (row < data.length && data[row] != null && col < data[row].length)
                return data[row][col];
            else
                return null;
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
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
            data[row][col] = (DataNode) value;
            fireTableCellUpdated(row, col);
        }
    }

    static class DataNode {
        private final String name;
        private boolean selected;

        DataNode(String name) {
            this.name = name;
        }

        boolean isSelected() {
            return selected;
        }

        void setSelected(boolean selected) {
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
        private JLabel label;
        private Swatch swatch;
        private JPanel both;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (both == null) {
                both = new JPanel();
                both.setLayout(new BoxLayout(both, BoxLayout.X_AXIS));
                swatch = new Swatch();
                swatch.setMaximumSize(new Dimension(getFont().getSize(), getFont().getSize()));
                swatch.setBorder(BorderFactory.createLineBorder(Color.WHITE));
                label = new JLabel();
                both.add(swatch);
                both.add(Box.createHorizontalStrut(4));
                both.add(label);

                label.setPreferredSize(new Dimension(label.getPreferredSize().width + 100, label.getPreferredSize().height));
                label.setMinimumSize(label.getPreferredSize());
            }
            both.setEnabled(isEnabled(row));
            label.setForeground(both.isEnabled() ? Color.BLACK : Color.GRAY);

            if (value != null) {
                final DataNode dataNode = (DataNode) value;

                label.setText(doc.getSampleLabelGetter().getLabel(dataNode.getName()));
                if (dataNode.isSelected())
                    both.setBorder(BorderFactory.createLineBorder(Color.RED));
                else
                    both.setBorder(null);
                Color color = chartColorManager.getSampleColor(dataNode.getName());
                if (dataNode.getName() != null && dataNode.isSelected()) {
                    both.setBackground(selectionBackgroundColor);
                } else
                    both.setBackground(backgroundColor);

                swatch.setBackground(isEnabled(row) ? color : Color.LIGHT_GRAY);


                if (dataNode.getName() != null)
                    swatch.setShape((String) dir.getDocument().getSampleAttributeTable().get(dataNode.getName(), SampleAttributeTable.HiddenAttribute.Shape.toString()));
                else
                    swatch.setShape("None");

                final String description = (String) dir.getDocument().getSampleAttributeTable().get(dataNode.getName(), SampleAttributeTable.DescriptionAttribute);
                if (description != null && description.length() > 0)
                    both.setToolTipText(description);
                else
                    both.setToolTipText(dataNode.getName());
            }

            return both;
        }
    }

    private static class Swatch extends JPanel {
        private String shape;

        void setShape(String shape) {
            this.shape = shape;
        }

        public void paint(Graphics gc0) {
            Graphics2D gc = (Graphics2D) gc0;

            if (shape != null && shape.equalsIgnoreCase("Square")) {
                gc.setColor(getBackground());
                gc.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
                gc.setColor(Color.BLACK);
                gc.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
            } else if (shape != null && shape.equalsIgnoreCase("Triangle")) {
                Shape polygon = new Polygon(new int[]{1, getWidth() - 1, getWidth() / 2}, new int[]{getHeight() - 1, getHeight() - 1, 1}, 3);
                gc.setColor(getBackground());
                gc.fill(polygon);
                gc.setColor(Color.BLACK);
                gc.draw(polygon);
            } else if (shape != null && shape.equalsIgnoreCase("Diamond")) {
                Shape polygon = new Polygon(new int[]{1, (int) Math.round(getWidth() / 2.0), getWidth() - 1, (int) Math.round(getWidth() / 2.0)},
                        new int[]{(int) Math.round(getHeight() / 2.0), getHeight() - 1, (int) Math.round(getHeight() / 2.0), 1}, 4);
                gc.setColor(getBackground());
                gc.fill(polygon);
                gc.setColor(Color.BLACK);
                gc.draw(polygon);
            } else if (shape == null || !shape.equalsIgnoreCase("None")) {  // circle
                gc.setColor(getBackground());
                gc.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                gc.setColor(Color.BLACK);
                gc.drawOval(1, 1, getWidth() - 2, getHeight() - 2);
            }
        }
    }

    class MyMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            jTable.repaint();
        }

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            } else {
                if (e.getSource() instanceof JTableHeader) {
                    columnPressed = jTable.getTableHeader().columnAtPoint(e.getPoint());
                    if (columnPressed >= 0) {
                        if (!e.isShiftDown() && !e.isMetaDown())
                            clearSelection();
                        selectColumn(columnPressed, true);
                    }
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
            jTable.repaint();
        }
    }

    private void showPopupMenu(final MouseEvent e) {
        // show the header popup menu:
        if (e.getSource() instanceof JTableHeader) {
            columnPressed = jTable.getTableHeader().columnAtPoint(e.getPoint());
            (new PopupMenu(this, GUIConfiguration.getDataColumnHeaderPopupConfiguration(), dir.getCommandManager())).show(e.getComponent(), e.getX(), e.getY());
        } else if (e.getSource() instanceof JTable && e.getSource() == jTable) {
            JPopupMenu popupMenu = (new PopupMenu(this, GUIConfiguration.getDataPopupConfiguration(), dir.getCommandManager()));

            popupMenu.addSeparator();
            popupMenu.add(new AbstractAction("Select Row") {
                public void actionPerformed(ActionEvent ae) {
                    clearSelection();
                    selectRow(jTable.rowAtPoint(e.getPoint()), true);
                }
            });
            popupMenu.add(new AbstractAction("Select Column") {
                public void actionPerformed(ActionEvent ae) {
                    clearSelection();
                    selectColumn(jTable.columnAtPoint(e.getPoint()), true);
                }
            });

            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    class MySelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                // The mouse button has not yet been released
            } else {
                Set<String> toSelect = new HashSet<>();
                for (int row = 0; row < jTable.getRowCount(); row++) {
                    for (int col = 0; col < jTable.getColumnCount(); col++) {
                        Object sample = jTable.getValueAt(row, col);
                        if (sample != null) {
                            boolean selectedInDocument = doc.getSampleSelection().isSelected(sample.toString());
                            boolean selectedOnGrid = jTable.isRowSelected(row) && jTable.isColumnSelected(col);
                            if (selectedOnGrid || (selectedInDocument && (jTable.isRowSelected(row) || jTable.isColumnSelected(col))))
                                toSelect.add(sample.toString());
                        }
                    }
                }
                doc.getSampleSelection().clear();
                doc.getSampleSelection().setSelected(toSelect, true);
                viewer.updateView(IDirector.ENABLE_STATE);
            }
        }
    }
}
