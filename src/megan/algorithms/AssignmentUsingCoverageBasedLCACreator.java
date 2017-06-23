/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.core.Document;

/**
 * create a coverage-base LCA assignment algorithm
 * Daniel Huson, 4.2017
 */
public class AssignmentUsingCoverageBasedLCACreator implements IAssignmentAlgorithmCreator {
    private final Document document;
    private final float topPercent;

    /**
     * constructor
     *
     * @param document
     */
    public AssignmentUsingCoverageBasedLCACreator(Document document, float topPercent) {
        this.document = document;
        this.topPercent = topPercent;
        System.err.println("Using coverage-based LCA algorithm for binning: " + Classification.Taxonomy);
    }

    /**
     * creates an assignment algorithm
     *
     * @return assignment algorithm
     */
    @Override
    public IAssignmentAlgorithm createAssignmentAlgorithm() {
        if (ProgramProperties.get("use-segment-lca", false)) {
            System.err.println("Using SEGMENT algorithm");
            return new AssignmentUsingSegmentLCA(document);
        } else if (ProgramProperties.get("use-new-coverage-lca", false)) {
            System.err.println("Using NEW algorithm");
            return new AssignmentUsingCoverageBasedLCANext(document);
        } else {
            System.err.println("Using OLD algorithm");
            return new AssignmentUsingCoverageBasedLCA(document, topPercent);
        }
    }
}
