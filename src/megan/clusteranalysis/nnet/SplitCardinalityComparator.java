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
package megan.clusteranalysis.nnet;

import java.util.Comparator;

/**
 * compares two splits first by their cardinality, then by their id.
 */
public class SplitCardinalityComparator implements Comparator {
    private int splitID;
    private int cardinality;

    public SplitCardinalityComparator() {
    }

    public SplitCardinalityComparator(int splitID, int cardinality) {
        this.splitID = splitID;
        this.cardinality = cardinality;
    }


    public int compare(Object obj1, Object obj2) throws ClassCastException {
        SplitCardinalityComparator scc1 = (SplitCardinalityComparator) obj1;
        SplitCardinalityComparator scc2 = (SplitCardinalityComparator) obj2;

        if (scc1.cardinality < scc2.cardinality)
            return -1;
        else if (scc1.cardinality > scc2.cardinality)
            return 1;
        if (scc1.splitID < scc2.splitID)
            return -1;
        else if (scc1.splitID > scc2.splitID)
            return 1;

        return 0;
    }

    public int getSplitID() {
        return splitID;
    }

    public void setSplitID(int splitID) {
        this.splitID = splitID;
    }

    public int getCardinality() {
        return cardinality;
    }

    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }
}
