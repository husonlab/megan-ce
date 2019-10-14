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

import jloda.swing.util.ListTransferHandler;
import jloda.swing.util.PopupMenu;
import megan.chart.ChartColorManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.LinkedList;

/**
 * side by list for attributes
 * Created by huson on 9/18/16.
 */
public class AttributesList extends LabelsJList {
    private final ChartSelection chartSelection;

    /**
     * constructor
     *
     * @param viewer
     */
    public AttributesList(final ChartViewer viewer) {
        super(viewer, createSyncListenerAttributesList(viewer), createPopupMenu(viewer));
        this.chartSelection = viewer.getChartSelection();

        setName("Attributes");

        addListSelectionListener(listSelectionEvent -> {
            if (!inSelection) {
                inSelection = true;
                try {
                    chartSelection.clearSelectionAttributes();
                    chartSelection.setSelectedAttribute(getSelectedLabels(), true);
                } finally {
                    inSelection = false;
                }
            }
        });

        setDragEnabled(true);
        setTransferHandler(new ListTransferHandler());
        chartSelection.addAttributesSelectionListener(chartSelection -> {
            if (!inSelection) {
                inSelection = true;
                try {
                    DefaultListModel model = (DefaultListModel) getModel();
                    for (int i = 0; i < model.getSize(); i++) {
                        String name = getModel().getElementAt(i);
                        if (chartSelection.isSelectedAttribute(name))
                            addSelectionInterval(i, i + 1);
                        else
                            removeSelectionInterval(i, i + 1);
                    }
                } finally {
                    inSelection = false;
                }
            }
        });
    }

    /**
     * call this when tab containing list is activated
     */
    public void activate() {
        getViewer().getSearchManager().setSearcher(getSearcher());
        getViewer().getSearchManager().getFindDialogAsToolBar().clearMessage();
        if (!inSelection) {
            inSelection = true;
            try {
                chartSelection.clearSelectionAttributes();
                chartSelection.setSelectedAttribute(getSelectedLabels(), true);
                this.repaint(); // todo: or viewer.repaint() ??
            } finally {
                inSelection = false;
            }
        }
    }

    /**
     * call this when tab containing list is deactivated
     */
    public void deactivate() {
        if (!inSelection) {
            inSelection = true;
            try {
                chartSelection.clearSelectionAttributes();
                this.repaint(); // todo: or viewer.repaint() ??
            } finally {
                inSelection = false;
            }
        }
    }

    private ChartViewer getViewer() {
        return (ChartViewer) viewer;
    }

    public ChartColorManager.ColorGetter getColorGetter() {
        return getViewer().getDir().getDocument().getChartColorManager().getAttributeColorGetter();
    }

    private static SyncListener createSyncListenerAttributesList(final ChartViewer viewer) {
        return enabledNames -> {
            if (viewer.getChartDrawer().canAttributes()) {
                viewer.getChartDrawer().forceUpdate();
            }
        };
    }

    private static PopupMenu createPopupMenu(ChartViewer viewer) {
        return new PopupMenu(null, GUIConfiguration.getAttributesListPopupConfiguration(), viewer.getCommandManager());
    }
}
