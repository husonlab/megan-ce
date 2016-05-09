/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.graphview.GraphView;
import jloda.gui.find.IObjectSearcher;
import megan.clusteranalysis.tree.Distances;
import megan.clusteranalysis.tree.Taxa;

/**
 * basic tab interface
 * Daniel Huson, 9.2015
 */
public interface ITab {
    /**
     * this tab has been selected
     */
    public void activate();

    /**
     * this tab has been deselected
     */
    public void deactivate();

    /**
     * compute the graphics
     *
     * @param taxa
     * @param distances
     */
    public void compute(Taxa taxa, Distances distances) throws Exception;

    /**
     * get the label
     *
     * @return
     */
    public String getLabel();

    /**
     * get the method name
     *
     * @return method of computation
     */
    public String getMethod();

    /**
     * update the view
     *
     * @param what
     */
    public void updateView(String what);

    /**
     * get the associated graphview
     *
     * @return graphview
     */
    public GraphView getGraphView();

    /**
     * zoom to fit
     */
    public void zoomToFit();

    /**
     * zoom to selection
     */
    public void zoomToSelection();

    /**
     * gets the searcher associated with this tab
     *
     * @return searcher
     */
    public IObjectSearcher getSearcher();
}
