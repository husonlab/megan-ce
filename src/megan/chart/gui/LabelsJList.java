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
package megan.chart.gui;

import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.find.JListSearcher;
import jloda.swing.util.ListTransferHandler;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.chart.ChartColorManager;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * labels JList
 * Daniel Huson, 6.2012
 */
public class LabelsJList extends JList<String> {
    final IDirectableViewer viewer;
    private final JListSearcher searcher;
    private boolean doClustering;
    private int tabIndex = -1;

    private boolean inSync = false;
    private final JPopupMenu popupMenu;
    private final Set<String> disabledLabels = new HashSet<>();
    private Map<String, String> label2ToolTips;
    private final SyncListener syncListener;
    public boolean inSelection = false; // use this to avoid selection bounce

    /**
     * constructor
     *
     * @param viewer
     * @param popupMenu
     */
    public LabelsJList(final IDirectableViewer viewer, final SyncListener syncListener, final JPopupMenu popupMenu) {
        super(new DefaultListModel<>());
        this.viewer = viewer;
        this.syncListener = syncListener;
        this.popupMenu = popupMenu;

        searcher = new JListSearcher(this);

        setDragEnabled(true);
        setTransferHandler(new ListTransferHandler());

        getModel().addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent event) {
            }

            public void intervalRemoved(ListDataEvent event) {
                if (!inSync) {
                    syncListener.syncList2Viewer(getEnabledLabels());
                    viewer.updateView(IDirector.ALL);

                }
            }

            public void contentsChanged(ListDataEvent event) {
            }
        });

        addListSelectionListener(event -> {
            if (!inSync && !inSelection)
                viewer.updateView("enable_state");
        });

        setCellRenderer(new MyCellRenderer());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                int index = locationToIndex(me.getPoint());
                if (getModel().getSize() > 0 && getCellBounds(index, index) != null
                        && !getCellBounds(index, index).contains(me.getPoint())) {
                    clearSelection();
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {
                /*
                if (getSelectedIndices().length == 0) {
                    JList list = (JList) me.getComponent();
                    int index = list.locationToIndex(me.getPoint());
                    setSelectedIndex(index);
                }
                */
                if (me.isPopupTrigger())
                    popup(me);
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                if (me.isPopupTrigger())
                    popup(me);
            }

            void popup(MouseEvent me) {
                if (LabelsJList.this.popupMenu != null) {
                    LabelsJList.this.popupMenu.show(LabelsJList.this, me.getX(), me.getY());
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!viewer.isLocked()) {
                    int clicks = event.getClickCount();
                    if (clicks == 3) {
                        boolean changed = false;
                        int index = getSelectedIndex();
                        String label = ((DefaultListModel) getModel()).get(index).toString();
                        String prefix5 = label.length() > 5 ? label.substring(0, 5) : label;
                        for (int i = 0; i < ((DefaultListModel) getModel()).size(); i++) {
                            String item = ((DefaultListModel) getModel()).getElementAt(i).toString();
                            if (!item.startsWith(prefix5)) {
                                disabledLabels.add(item);
                                changed = true;
                            } else {
                                if (disabledLabels.contains(item)) {
                                    disabledLabels.remove(item);
                                    changed = true;
                                }
                            }
                        }
                        if (changed) {
                            if (!inSync) {
                                syncListener.syncList2Viewer(getEnabledLabels());
                                viewer.updateView(IDirector.ALL);
                            }
                        }
                    } else if (clicks == 2) {
                        boolean changed = false;
                        int[] indices = getSelectedIndices();
                        boolean hasOneActive = false;
                        boolean hasOneInactive = false;
                        for (int element : indices) {
                            String label = ((DefaultListModel) getModel()).getElementAt(element).toString();
                            if (disabledLabels.contains(label))
                                hasOneInactive = true;
                            else
                                hasOneActive = true;
                            if (hasOneActive && hasOneInactive)
                                break;
                        }
                        boolean enable = true;
                        if (hasOneActive && !hasOneInactive)
                            enable = false;
                        for (int element : indices) {
                            String label = ((DefaultListModel) getModel()).getElementAt(element).toString();
                            if (enable && disabledLabels.contains(label)) {
                                disabledLabels.remove(label);
                                changed = true;
                            } else if (!enable && !disabledLabels.contains(label)) {
                                disabledLabels.add(label);
                                changed = true;
                            }
                        }
                        if (changed) {
                            if (!inSync) {
                                if (!viewer.isLocked()) {
                                    viewer.lockUserInput();
                                    syncListener.syncList2Viewer(getEnabledLabels());
                                    viewer.updateView(IDirector.ALL);
                                    viewer.unlockUserInput();
                                }
                            }
                        }
                    }
                }
            }
        });
        setDoClustering(ProgramProperties.get(getName() + "DoClustering", doClustering));
    }

    public void ensureSelectedIsVisible() {
        if (getSelectedIndices().length > 0) {
            ensureIndexIsVisible(getSelectedIndex());
        }

    }

    /**
     * gets the tab index
     *
     * @return tab index
     */
    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int index) {
        this.tabIndex = index;
    }

    /**
     * get searcher
     *
     * @return
     */
    public JListSearcher getSearcher() {
        return searcher;
    }

    /**
     * get all labels held by this list
     *
     * @return all labels
     */
    public LinkedList<String> getAllLabels() {
        final LinkedList<String> result = new LinkedList<>();
        for (int i = 0; i < getModel().getSize(); i++) {
            String label = getModel().getElementAt(i);
            result.add(label);
        }
        return result;
    }

    public LinkedList<String> getSelectedLabels() {
        final LinkedList<String> result = new LinkedList<>();
        final DefaultListModel model = (DefaultListModel) getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (isSelectedIndex(i)) {
                result.add(model.getElementAt(i).toString());
            }
        }
        return result;
    }

    public void selectTop(int top) {
        final DefaultListModel model = (DefaultListModel) getModel();
        top = Math.min(top, model.getSize());
        clearSelection();
        if (top > 0)
            setSelectionInterval(0, top - 1);
    }

    /**
     * gets the list of enabled labels
     *
     * @return enabled labels
     */
    public LinkedList<String> getEnabledLabels() {
        final LinkedList<String> result = new LinkedList<>();
        for (int i = 0; i < getModel().getSize(); i++) {
            String label = getModel().getElementAt(i);
            if (!disabledLabels.contains(label))
                result.add(label);
        }
        return result;
    }

    /**
     * get all the currently disabled labels
     *
     * @return disabled labels
     */
    public LinkedList<String> getDisabledLabels() {
        final LinkedList<String> result = new LinkedList<>(disabledLabels);
        return result;
    }

    /**
     * set the disabled labels
     *
     * @param labels
     */
    public void setDisabledLabels(Collection<String> labels) {
        disabledLabels.clear();
        disabledLabels.addAll(labels);
    }

    /**
     * disable the named labels
     *
     * @param labels
     */
    public void disableLabels(Collection<String> labels) {
        disabledLabels.addAll(labels);
    }


    /**
     * enable the named labels
     *
     * @param labels
     */
    public void enableLabels(Collection<String> labels) {
        disabledLabels.removeAll(labels);
    }

    /**
     * sync to the given list of labels
     *
     * @param labels
     */
    public void sync(final Collection<String> labels, final Map<String, String> label2toolTip, final boolean clearOldOrder) {
        if (!inSelection && !inSync) {
            inSync = true;
            this.label2ToolTips = label2toolTip;
            try {
                Runnable runnable = () -> {
                    disabledLabels.clear();
                    if (clearOldOrder)
                        ((DefaultListModel) getModel()).removeAllElements();

                    final Set<String> labelsSet = new HashSet<>();
                    labelsSet.addAll(labels);

                    final Set<String> toDelete = new HashSet<>();
                    for (String label : disabledLabels) {
                        if (!labelsSet.contains(label))
                            toDelete.add(label);
                    }
                    disabledLabels.removeAll(toDelete);

                    final List<String> toKeep = new LinkedList<>();
                    for (int i = 0; i < getModel().getSize(); i++) {
                        String label = getModel().getElementAt(i);
                        if (labelsSet.contains(label))
                            toKeep.add(label);
                    }
                    ((DefaultListModel<String>) getModel()).removeAllElements();
                    for (String label : toKeep) {
                        ((DefaultListModel<String>) getModel()).addElement(label);
                    }
                    final Set<String> seen = new HashSet<>();
                    seen.addAll(toKeep);
                    for (String label : labels) {
                        if (!seen.contains(label))
                            ((DefaultListModel<String>) getModel()).addElement(label);
                    }
                    validate();
                };
                if (SwingUtilities.isEventDispatchThread())
                    runnable.run();
                else
                    SwingUtilities.invokeAndWait(runnable);
            } catch (Exception e) {
                Basic.caught(e);
            } finally {
                inSync = false;
            }
        }
    }

    /**
     * syncs the selection in this list to the viewer
     */
    public void fireSyncToViewer() {
        syncListener.syncList2Viewer(getEnabledLabels());
    }

    public String getToolTipText(MouseEvent evt) {
        if (label2ToolTips == null)
            return null;
        // Get item index
        int index = locationToIndex(evt.getPoint());

        // Get item
        String label = getModel().getElementAt(index);
        String toolTip = label2ToolTips.get(label);
        if (toolTip != null)
            return toolTip;
        else
            return label;
    }

    public Map<String, String> getLabel2ToolTips() {
        return label2ToolTips;
    }

    /**
     * cell renderer
     */
    class MyCellRenderer extends JPanel implements ListCellRenderer<String> {
        private final JPanel box = new JPanel();
        private final JLabel label = new JLabel();

        MyCellRenderer() {
            final Dimension dim = new Dimension(10, 10);
            if (getColorGetter() != null) {
                box.setMinimumSize(dim);
                box.setMaximumSize(dim);
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                add(box);
                add(Box.createHorizontalStrut(2));
            }
            // label.setFont(new Font("Arial", Font.PLAIN,12));
            add(label);

            setOpaque(true);
            setForeground(Color.black);
            setBackground(Color.WHITE);
        }

        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            box.setEnabled(enabled);
            label.setEnabled(enabled);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            label.setText(value);
            setEnabled(!disabledLabels.contains(value) && LabelsJList.this.isEnabled());
            if (isSelected) {
                setBorder(BorderFactory.createLineBorder(ProgramProperties.SELECTION_COLOR_DARKER));
                setBackground(ProgramProperties.SELECTION_COLOR);
            } else {
                setBorder(BorderFactory.createLineBorder(Color.WHITE));
                setBackground(Color.WHITE);
            }
            if (getColorGetter() != null) {
                Color color;
                if (isEnabled() && LabelsJList.this.isEnabled()) {
                    color = getColorGetter().get(value);
                    box.setBackground(color);
                    if (color.equals(Color.WHITE))
                        box.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                    box.setBorder(BorderFactory.createLineBorder(color.darker()));
                } else {
                    box.setBackground(Color.WHITE);
                    box.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                }
                box.repaint();
            }
            return this;
        }
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    ChartColorManager.ColorGetter getColorGetter() {
        return null;
    }

    public boolean isDoClustering() {
        return doClustering;
    }

    public void setDoClustering(boolean doClustering) {
        this.doClustering = doClustering;
        ProgramProperties.put(getName() + "DoClustering", doClustering);
    }
}
