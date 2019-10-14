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

package megan.assembly;

import jloda.graph.DirectedCycleDetector;
import jloda.graph.Edge;
import jloda.graph.Graph;

import java.util.Collection;

/**
 * breaks all directed cycles be removing edges
 * Created by huson on 8/22/16.
 */
class DirectedCycleBreaker {
    public static int apply(Graph G) {
        DirectedCycleDetector dector = new DirectedCycleDetector(G);
        int count = 0;
        while (dector.apply()) {
            final Collection<Edge> cycle = dector.cycle();
            Edge best = null;
            for (Edge e : cycle) {
                if (best == null || (int) e.getInfo() < (int) best.getInfo() ||
                        ((int) e.getInfo()) == (int) best.getInfo() &&
                                e.getSource().getOutDegree() + e.getTarget().getInDegree() <
                                        best.getSource().getOutDegree() + best.getTarget().getInDegree())
                    best = e;
            }
            if (best == null)
                throw new RuntimeException("Internal error: empty cycle???");
            G.deleteEdge(best);
            count++;
        }
        return count;
    }
}
