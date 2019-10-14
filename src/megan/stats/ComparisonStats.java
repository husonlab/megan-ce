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
package megan.stats;

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.phylo.PhyloTree;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.CanceledException;
import megan.classification.IdMapper;
import megan.core.Director;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * statistical comparison of two datasets
 * Daniel Huson, 3.2007
 */
class ComparisonStats {
    private static final List<String> methods;

    static {
        methods = new LinkedList<>();
        methods.add(ResamplingMethodItem.NAME);
    }

    /**
     * return a string describing all known methods
     *
     * @return methods
     */
    public static String getKnownMethods() {
        return Basic.toString(methods, " ");
    }

    /**
     * launch the computation of a comparison
     *
     * @param dir1
     * @param dir2
     * @param methodName
     * @param options
     * @throws IOException
     */
    public static void launchComparison(Director dir0, Director dir1, Director dir2, String methodName,
                                        String options) throws IOException, CanceledException {

        IMethodItem item = null;
        if (methodName.equals(ResamplingMethodItem.NAME)) {
            item = new ResamplingMethodItem();
        } else
            NotificationsInSwing.showError("Unknown statistical method: " + methodName);
        if (item != null) {
            item.parseOptionString(options);

            Map<Integer, Float> input1 = computeInputMapFromLeaves(dir1, item.getOptionUseInternal(), item.getOptionUseUnassigned());
            System.err.println("Input map for " + dir1.getTitle() + ": " + input1.keySet().size());
            Map<Integer, Float> input2 = computeInputMapFromLeaves(dir2, item.getOptionUseInternal(), item.getOptionUseUnassigned());
            System.err.println("Input map for " + dir2.getTitle() + ": " + input2.keySet().size());

            item.setInput(input1, input2);

            item.apply(dir0.getDocument().getProgressListener());

            Map<Integer, Double> result = item.getOutput();
            ResamplingMethodItem.displayResult(result, dir1, dir2);
        }
    }


    /**
     * compute the input map from all nodes of the input taxonomy
     *
     * @param dir
     * @return input map
     */
    private static Map<Integer, Float> computeInputMapFromLeaves(Director dir, boolean useInternal, boolean useUnassigned) {
        Map<Integer, Float> map = new TreeMap<>();

        PhyloTree tree = dir.getMainViewer().getTree();
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (useInternal || v.getDegree() == 1) // is a leaf
            {
                Integer taxId = (Integer) v.getInfo();
                if (taxId != null && (useUnassigned ||
                        !(taxId.equals(IdMapper.NOHITS_ID)
                                || taxId.equals(IdMapper.UNASSIGNED_ID) || taxId.equals(IdMapper.LOW_COMPLEXITY_ID)))) {
                    float count = ((NodeData) v.getData()).getCountSummarized();
                    map.put(taxId, count);
                }
            }
        }
        return map;
    }
}
