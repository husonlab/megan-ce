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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import megan.core.Document;
import megan.viewer.TaxonomyData;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * applies or unapplies the signficance test
 * Daniel Huson, 6.2008
 */
class
ApplySignificanceTest {
    public static final String NO_CORRECTION = "none";
    public static String BONFERRONI = "bonferroni";
    public static String HOLM_BONFERRONI = "holm_bonferroni";
    public static final String CONTRASTS = "contrasts";

    private static final int NO_CORRECTION_ID = 1;
    private static final int BONFERRONI_ID = 2;
    private static final int HOLM_BONFERRONI_ID = 3;
    public static final int CONTRASTS_ID = 4;

    /**
     * apply or unapply the significance test for two compared results
     *
     * @param doc
     * @param show
     */
    public static void apply(Document doc, boolean show) {

        System.err.println("Computing differences...");
        PhyloTree tree = doc.getDir().getMainViewer().getTree();

        if (show) {
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                NodeData vd = (NodeData) v.getData();

                // calculate left support:
                if (v.getInDegree() == 1 /*&& v.getFirstInEdge().getSource().getOutDegree() > 1*/) {
                    Node w = v.getFirstInEdge().getSource();

                    float value1 = vd.getSummarized()[0];
                    float value2 = vd.getSummarized()[1];
                    NodeData wd = (NodeData) w.getData();

                    float up1 = wd.getSummarized()[0];
                    float up2 = wd.getSummarized()[1];

                    double[] result = SignificanceTestForTwoDatasets.runProportionTest(value1, up1, value2, up2);
                    // System.err.println("value1: "+value1+" up1: "+up1+" value2: "+value2+" up2: "+up2+" result: "+result[0]+", "+result[1]);
                    double pvalue = result[1];
                    vd.setUpPValue(pvalue > 1 || pvalue < 0 ? -1 : pvalue);
                } else
                    vd.setUpPValue(-1);

                if (v.getOutDegree() > 1) {
                    // calculate right support:
                    {
                        boolean addAssigned = (vd.getAssigned()[0] > 0 || vd.getAssigned()[1] > 0);
                        int size = v.getOutDegree();
                        if (addAssigned)
                            size += 1;

                        double[] down1 = new double[size];
                        double[] down2 = new double[size];

                        int count = 0;
                        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e), count++) {
                            Node w = e.getTarget();
                            NodeData wd = (NodeData) w.getData();
                            down1[count] = wd.getSummarized()[0];
                            down2[count] = wd.getSummarized()[1];
                        }
                        if (addAssigned) {
                            down1[count] = vd.getAssigned()[0];
                            down2[count] = vd.getAssigned()[1];
                        }
                        double[] result = SignificanceTestForTwoDatasets.runLowerBranchesTest(down1, down2);
                        double pvalue = result[1];

                        vd.setDownPValue(pvalue > 1 || pvalue < 0 ? -1 : pvalue);
                    }
                } else
                    vd.setDownPValue(-1);
                Integer taxId = (Integer) v.getInfo();
                System.out.println(TaxonomyData.getName2IdMap().get(taxId) + ": UPv= " + vd.getUpPValue() + " DPv= " + vd.getDownPValue());
            }
            switch (doc.getSignificanceTestCorrection()) {
                case BONFERRONI_ID:
                    System.err.println("Applying Bonferroni correction");
                    applyBonferroniCorrection(tree);
                    break;
                case HOLM_BONFERRONI_ID:
                    System.err.println("Applying Holm-Bonferroni correction");
                    applyHolmBonferroniCorrection(tree);
                    break;
                case NO_CORRECTION_ID:
                default:
                    break;
            }

        } else  // hide
        {
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                NodeData vd = (NodeData) v.getData();
                vd.setUpPValue(-1);
                vd.setDownPValue(-1);
            }
        }
    }

    /**
     * apply the Bonferroni correction
     *
     * @param tree
     */
    private static void applyBonferroniCorrection(PhyloTree tree) {
        int numberOfUpCases = 0;
        int numberOfDownCases = 0;

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            NodeData vd = (NodeData) v.getData();
            if (!Double.isNaN(vd.getDownPValue()) && vd.getDownPValue() != -1 && v.getOutDegree() > 1)
                numberOfDownCases++;
            if (!Double.isNaN(vd.getUpPValue()) && vd.getUpPValue() != -1 && v.getInDegree() > 0)
                numberOfUpCases++;
        }
        if (numberOfDownCases == 0) numberOfDownCases = 1;
        if (numberOfUpCases == 0) numberOfUpCases = 1;

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            NodeData vd = (NodeData) v.getData();
            if (!Double.isNaN(vd.getUpPValue()) && vd.getUpPValue() != -1 && v.getInDegree() > 0)
                vd.setUpPValue(Math.min(1, vd.getUpPValue() * numberOfUpCases));
            if (!Double.isNaN(vd.getDownPValue()) && vd.getDownPValue() != -1 && v.getOutDegree() > 1)
                vd.setDownPValue(Math.min(1, vd.getDownPValue() * numberOfDownCases));
        }
    }

    /**
     * apply the Holm-Bonferroni correction
     *
     * @param tree
     */
    private static void applyHolmBonferroniCorrection(PhyloTree tree) {
        List<Pair<Double, Integer>> upPairs = new LinkedList<>();
        List<Pair<Double, Integer>> downPairs = new LinkedList<>();

        int countUp = 0;
        int countDown = 0;
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            NodeData vd = (NodeData) v.getData();

            if (!Double.isNaN(vd.getDownPValue()) && vd.getDownPValue() != -1 && v.getOutDegree() > 1) {
                downPairs.add(new Pair<>(vd.getDownPValue(), countDown++));
            }
            if (!Double.isNaN(vd.getUpPValue()) && vd.getUpPValue() != -1 && v.getInDegree() > 0) {
                upPairs.add(new Pair<>(vd.getUpPValue(), countUp++));
            }
        }

        Pair[] upArray = upPairs.toArray(new Pair[0]);
        Pair[] downArray = downPairs.toArray(new Pair[0]);

        Arrays.sort(upArray, new CompareFirst());
        for (int i = 0; i < upArray.length; i++) {
            Pair pair = upArray[i];

            double uncorrected = pair.getFirstDouble();
            double bonferroni = Math.min(1, uncorrected * countUp);
            double bholm = Math.min(1, pair.getFirstDouble() * (i + 1));
            pair.setFirst(bholm);
        }
        Arrays.sort(upArray, new CompareSecond());

        Arrays.sort(downArray, new CompareFirst());
        for (int i = 0; i < downArray.length; i++) {
            Pair pair = downArray[i];

            double uncorrected = pair.getFirstDouble();
            double bonferroni = Math.min(1, uncorrected * countUp);
            double bholm = Math.min(1, pair.getFirstDouble() * (i + 1));
            pair.setFirst(bholm);
        }
        Arrays.sort(downArray, new CompareSecond());

        countUp = 0;
        countDown = 0;
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            NodeData vd = (NodeData) v.getData();

            if (!Double.isNaN(vd.getDownPValue()) && vd.getDownPValue() != -1 && v.getOutDegree() > 1) {
                if (downArray[countDown].getSecondInt() != countDown)
                    System.err.println("Holm-Bonferroni failed, illegal ordering");
                vd.setDownPValue(downArray[countDown].getFirstDouble());
                countDown++;
            }
            if (!Double.isNaN(vd.getUpPValue()) && vd.getUpPValue() != -1 && v.getInDegree() > 0) {
                if (upArray[countUp].getSecondInt() != countUp)
                    System.err.println("Holm-Bonferroni failed, illegal ordering");
                vd.setUpPValue(upArray[countUp].getFirstDouble());
                countUp++;
            }
        }
    }
}

class CompareFirst implements Comparator<Pair> {
    public int compare(Pair a, Pair b) {
        if (a.getFirstDouble() > b.getFirstDouble())
            return -1;
        if (a.getFirstDouble() < b.getFirstDouble())
            return 1;
        return Integer.compare(a.getSecondInt(), b.getSecondInt());
    }
}

class CompareSecond implements Comparator<Pair> {
    public int compare(Pair a, Pair b) {
        return Integer.compare(a.getSecondInt(), b.getSecondInt());
    }

}
