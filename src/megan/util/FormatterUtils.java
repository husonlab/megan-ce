/*
 * FormatterUtils.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.util;

import jloda.graph.Edge;
import jloda.graph.EdgeSet;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.swing.director.IDirector;
import jloda.swing.format.Formatter;
import jloda.swing.format.IFormatterListener;
import jloda.swing.graphview.INodeEdgeFormatable;
import jloda.util.Pair;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;

import javax.swing.*;

/**
 * some utilities to manager formatting of trees
 * Daniel Huson, 3.2007
 */
public class FormatterUtils {

    /**
     * gets the formatter for the given viewer and director
     *
     * @return formatter
     */
    public static Formatter getFormatter(final INodeEdgeFormatable viewer, final IDirector dir) {
        if (Formatter.getInstance() == null) {
            final Formatter formatter = new Formatter(dir, viewer, false);
            formatter.getFrame().setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            Formatter.setInstance(formatter);

            formatter.addFormatterListener(new IFormatterListener() {
                public void nodeFormatChanged(NodeSet nodes) {
                    INodeEdgeFormatable viewer = formatter.getViewer();
                    if (viewer instanceof MainViewer) {
                        MainViewer mainViewer = (MainViewer) viewer;
                        for (Node v : nodes) {
                            mainViewer.getDirtyNodeIds().add((Integer) v.getInfo());
                        }
                        mainViewer.getDir().getDocument().setDirty(true);
                    } else if (viewer instanceof ClassificationViewer) {
                        ClassificationViewer classificationViewer = (ClassificationViewer) viewer;
                        for (Node v : nodes) {
                            classificationViewer.getDirtyNodeIds().add((Integer) v.getInfo());
                        }
                        classificationViewer.getDocument().setDirty(true);
                    }
                }

                public void edgeFormatChanged(EdgeSet edges) {
                    INodeEdgeFormatable viewer = formatter.getViewer();
                    if (viewer instanceof MainViewer) {
                        MainViewer mainViewer = (MainViewer) viewer;
                        for (Edge e : edges) {
                            mainViewer.getDirtyEdgeIds().add(new Pair<>((Integer) e.getSource().getInfo(), (Integer) e.getTarget().getInfo()));
                        }
                        //SyncSummaryAndTaxonomy.syncFormattingFromViewer2Summary(mainViewer, mainViewer.getDirector().getDocument().getMegan4Summary());
                        mainViewer.getDir().getDocument().setDirty(true);
                    } else if (viewer instanceof ClassificationViewer) {
                        ClassificationViewer classificationViewer = (ClassificationViewer) viewer;
                        for (Edge e : edges) {
                            classificationViewer.getDirtyEdgeIds().add(new Pair<>((Integer) e.getSource().getInfo(), (Integer) e.getTarget().getInfo()));
                        }
                        classificationViewer.getDocument().setDirty(true);
                    }
                }
            });
        } else
            Formatter.getInstance().setViewer(dir, viewer);
        return Formatter.getInstance();
    }
}
