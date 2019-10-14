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
import megan.chart.data.IChartData;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.LinkedList;

/**
 * side list for series
 * Created by huson on 9/18/16.
 */
public class SeriesList extends LabelsJList {
    private final ChartSelection chartSelection;

    /**
     * constructor
     *
     * @param viewer
     */
    public SeriesList(final ChartViewer viewer) {
        super(viewer, createSyncListenerSeriesList(viewer), createPopupMenu(viewer));
        this.chartSelection = viewer.getChartSelection();
        setName("Series");

        addListSelectionListener(listSelectionEvent -> {
            if (!inSelection) {
                inSelection = true;
                try {
                    chartSelection.clearSelectionSeries();
                    chartSelection.setSelectedSeries(getSelectedLabels(), true);
                } finally {
                    inSelection = false;
                }
            }
        });

        setDragEnabled(true);
        setTransferHandler(new ListTransferHandler());
        chartSelection.addSeriesSelectionListener(chartSelection -> {
            if (!inSelection) {
                inSelection = true;
                try {
                    DefaultListModel model = (DefaultListModel) getModel();
                    for (int i = 0; i < model.getSize(); i++) {
                        String name = getModel().getElementAt(i);
                        if (chartSelection.isSelectedSeries(name))
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
                chartSelection.clearSelectionSeries();
                chartSelection.setSelectedSeries(getSelectedLabels(), true);
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
                chartSelection.clearSelectionSeries();
                this.repaint(); // todo: or viewer.repaint() ??
            } finally {
                inSelection = false;
            }
        }
    }

    public ChartColorManager.ColorGetter getColorGetter() {
        return getViewer().getDir().getDocument().getChartColorManager().getSeriesColorGetter();
    }

    private ChartViewer getViewer() {
        return (ChartViewer) viewer;
    }

    private static SyncListener createSyncListenerSeriesList(final ChartViewer viewer) {
        return enabledNames -> {
            if (viewer.getChartData() instanceof IChartData) {
                viewer.getChartData().setEnabledSeries(enabledNames);
            }
        };
    }

    private static PopupMenu createPopupMenu(ChartViewer viewer) {
        return new PopupMenu(null, GUIConfiguration.getSeriesListPopupConfiguration(), viewer.getCommandManager());
    }
}
