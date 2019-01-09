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

package megan.algorithms;

/**
 * create an LCA assignment algorithm for taxonomy
 * Daniel Huson, 3.2016
 */
public class AssignmentUsingLCAForTaxonomyCreator implements IAssignmentAlgorithmCreator {
    private final String cName;
    private final boolean usePercentIdentityFilter;
    private final float percentToCover;

    /**
     * constructor
     *
     * @param cName
     */
    public AssignmentUsingLCAForTaxonomyCreator(String cName, boolean usePercentIdentityFilter, float percentToCover) {
        this.cName = cName;
        this.usePercentIdentityFilter = usePercentIdentityFilter;
        this.percentToCover = percentToCover;
        if (percentToCover == 100)
            System.err.println(String.format("Using 'Naive LCA' algorithm for binning: %s", cName));
        else
            System.err.println(String.format("Using 'Naive LCA' algorithm (%.1f %%) for binning: %s", percentToCover, cName));
    }

    /**
     * creates an assignment algorithm
     *
     * @return assignment algorithm
     */
    @Override
    public IAssignmentAlgorithm createAssignmentAlgorithm() {
        return new AssignmentUsingLCAForTaxonomy(cName, usePercentIdentityFilter, percentToCover);
    }
}
