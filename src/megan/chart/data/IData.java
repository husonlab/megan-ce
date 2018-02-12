/*
 *  Copyright (C) 2018 Daniel H. Huson
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
package megan.chart.data;

import jloda.phylo.PhyloTree;
import megan.chart.gui.ChartSelection;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * most general interface for chart data
 * Daniel Huson, 6.2012
 */
public interface IData {
    void clear();

    ChartSelection getChartSelection();

    void setDataSetName(String label);

    String getDataSetName();

    void setSeriesLabel(String label);

    String getSeriesLabel();

    void setClassesLabel(String label);

    String getClassesLabel();

    void setCountsLabel(String label);

    String getCountsLabel();

    void setEnabledSeries(Collection<String> series);

    int getNumberOfSeries();

    Collection<String> getSeriesNames();

    void read(Reader r) throws IOException;

    void write(Writer w) throws IOException;

    Map<String, String> getSamplesTooltips();

    Map<String, String> getClassesTooltips();

    PhyloTree getTree();

    void setTree(PhyloTree tree);

}
