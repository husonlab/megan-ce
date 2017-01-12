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

/**
 * create a best hit assignment algorithm
 * Daniel Huson, 3.2016
 */
public class AssignmentUsingBestHitCreator implements IAssignmentAlgorithmCreator {
    private final AssignmentUsingBestHit assignmentUsingBestHit;

    /**
     * constructor
     *
     * @param cName
     */
    public AssignmentUsingBestHitCreator(String cName, String fileName) {
        assignmentUsingBestHit = new AssignmentUsingBestHit(cName, fileName); // only need one instance
    }

    /**
     * creates an assignment algorithm
     *
     * @return assignment algorithm
     */
    @Override
    public IAssignmentAlgorithm createAssignmentAlgorithm() {
        return assignmentUsingBestHit;
    }
}
