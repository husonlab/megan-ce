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

import jloda.swing.util.GraphViewPopupListener;
import jloda.util.ProgramProperties;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.tree.Distances;
import megan.clusteranalysis.tree.NJ;
import megan.clusteranalysis.tree.Taxa;

/**
 * Tab that displays a NJ tree
 * Daniel Huson, 9.2015
 */
public class NJTab extends TreeTabBase implements ITab {
    /**
     * constructor
     *
     * @param clusterViewer
     */
    public NJTab(final ClusterViewer clusterViewer) {
        super(clusterViewer);

        getGraphView().setPopupListener(new GraphViewPopupListener(getGraphView(),
                megan.clusteranalysis.GUIConfiguration.getNodePopupConfiguration(),
                megan.clusteranalysis.GUIConfiguration.getEdgePopupConfiguration(),
                megan.clusteranalysis.GUIConfiguration.getPanelPopupConfiguration(), clusterViewer.getCommandManager()));
    }

    public String getLabel() {
        return "NJ Tree";
    }

    public String getMethod() {
        return "Neighbor-joining";
    }


    /**
     * sync
     *
     * @param taxa
     * @param distances
     * @throws Exception
     */
    public void compute(Taxa taxa, Distances distances) throws Exception {
        if (getGraphView().getGraph().getNumberOfNodes() == 0) {
            System.err.println("Computing " + getLabel());
            getGraphView().setAutoLayoutLabels(true);
            NJ.apply(taxa, distances, getGraphView());
            getGraphView().setFixedNodeSize(true);
            getGraphView().setFont(ProgramProperties.get(ProgramProperties.DEFAULT_FONT, clusterViewer.getFont()));
            clusterViewer.addFormatting(getGraphView());
        }
    }
}
