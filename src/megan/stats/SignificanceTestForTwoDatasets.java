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

import cern.jet.stat.Probability;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Signifiance test for two datasets
 * <p/>
 * Suparna Mitra and Wei Wu, 2008
 */

public class SignificanceTestForTwoDatasets {
    /*
     * class attributes
     */
    private final Vector<Vector<Double>> dataset = new Vector<>();
    private double significance = 0.05;

    //x1,n1,x2,n2 used for Q.A
    private double x1 = 0;
    private double n1 = 0;
    private double x2 = 0;
    private double n2 = 0;

    /**
     * default constructor
     */
    public SignificanceTestForTwoDatasets() {

    }

    /**
     * construct instance to perform proportion test
     *
     * @param x1
     * @param n1
     * @param x2
     * @param n2
     */
    private SignificanceTestForTwoDatasets(double x1, double n1, double x2, double n2) {
        this.x1 = x1;
        this.n1 = n1;
        this.x2 = x2;
        this.n2 = n2;
    }


    /**
     * calculate chi-square and p value for two proprotions
     *
     * @param x1: value of the particular node in first data Data1
     * @param n1: Total read in higher node(level) including Data1
     * @param x2: value of the particular node in second data Data2
     * @param n2: Total read in higher node(level) including Data2
     * @return res: res[0]: chi square value; res[1]: p value
     */

    static public double[] runProportionTest(double x1, double n1, double x2, double n2) {
        SignificanceTestForTwoDatasets test = new SignificanceTestForTwoDatasets(x1, n1, x2, n2);

        double[] res = new double[2];

        res[0] = test.getChi_SquareValueWithContinuityCorrectionTwoTailed();
        res[1] = test.getPValueForProportionTest();

        return res;
    }

    /*
     * calculate chi square and p value for two dataset
     * @param valuesD1        counts for first dataset in all child nodes
     * @param valuesD2        counts for second dataset in all child nodes
     * @return res: res[0]: chi square value; res[1]: p value
     *         !Attention: if (valuesD1.length != valuesD2.length) res=[NaN,NaN]
     */

    static public double[] runLowerBranchesTest(double[] valuesD1, double[] valuesD2) {
        SignificanceTestForTwoDatasets test = new SignificanceTestForTwoDatasets();

        double[] res = new double[2];
        Vector<Double> dataA = new Vector<>();
        Vector<Double> dataB = new Vector<>();
        for (double d0 : valuesD1) {
            dataA.add(d0);
        }
        for (double d1 : valuesD2) {
            dataB.add(d1);
        }

        test.clearDataset();//somit letze aufgeladene Data die neue Berechnung nicht beeinflusst
        test.getDataset().add(dataA);
        test.getDataset().add(dataB);
        // System.out.println(test.getDataset().size()+" "+test.getDataset().get(0).size()+" "+test.getDataset().get(1).size());

        if (valuesD1.length == valuesD2.length) {
            res[0] = test.getChi_SquareValue();
            res[1] = test.getPValueForChi_Squrare();
            return res;
        } else {
            res[0] = Double.NaN;
            res[1] = Double.NaN;
            return res;
        }
    }


    private Vector<Vector<Double>> getDataset() {
        return dataset;
    }

    /**
     * **************************************
     * Q.A
     * calculate Chi Square value of 2x2 array for propotion test
     */
    public void setX1(double d) {
        x1 = d;
    }

    public double getX1() {
        return x1;
    }

    public void setX2(double d) {
        x2 = d;
    }

    public double getX2() {
        return x2;
    }

    public void setN1(double d) {
        n1 = d;
    }

    public double getN1() {
        return n1;
    }

    public void setN2(double d) {
        n2 = d;
    }

    public double getN2() {
        return n2;
    }

    //special calculation for proportion test

    private double getChi_SquareValueWithContinuityCorrectionTwoTailed() {
        double result;

        double p1 = x1 / n1;
        double p2 = x2 / n2;
        double p = (p1 * n1 + p2 * n2) / (n1 + n2);

        if (p == 1)
            return 0;

        result = (n1 * n2) * (Math.abs(p1 - p2) - (n1 + n2) / (2 * n1 * n2)) * (Math.abs(p1 - p2) - (n1 + n2) / (2 * n1 * n2)) / ((n1 + n2) * p * (1 - p));
        return result;
    }

    /**
     * *****************************************
     * Q.B
     * Branches comparison
     */

    /*
     * get, set significance
     */
    public double getSignificance() {
        return significance;
    }

    public void setSignificance(double d) {
        significance = d;
    }

    /*
     * read all values of input files into the attribute Vector<Vetor<Integer>> of this class
     */

    public Vector<Vector<Double>> addSubfiles(LinkedList<String> filelist) throws IOException {
        String aLine;
        Vector<double[]> output = new Vector<>();
        LinkedList<String> genename = new LinkedList<>();

        //count wird eingesetz als Zaehler der input Files
        int count = 0;
        for (String s : filelist) {
            BufferedReader r = new BufferedReader(new FileReader(s));
            aLine = r.readLine();

            aLine = r.readLine();
            while (aLine != null) {

                if ((aLine.length() > 0) && !aLine.startsWith("#")) {
                    StringTokenizer token = new StringTokenizer(aLine);
                    String name = token.nextToken();
                    if (genename.isEmpty()) {
                        genename.addLast(name);
                        double[] value = new double[filelist.size()];
                        value[count] = Long.parseLong(token.nextToken());
                        output.add(value);
                    } else {
                        //warum "+=" statt "="? Da in manchen Files kommmen einige Genenamen mehrfach vor, betrachte ich diese gleichen Genenamen als einen Gen
                        if (genename.contains(name))
                            output.get(genename.indexOf(name))[count] += Integer.parseInt(token.nextToken());
                        else {
                            double[] value1 = new double[filelist.size()];
                            value1[count] = Long.parseLong(token.nextToken());
                            genename.addLast(name);
                            output.add(value1);
                        }
                    }
                }
                aLine = r.readLine();
            }
            count++;
        }

        /*
         * converte Vetor<long[]> to Vetor<Vecotr<Long>>
         * so kann the result be in calss datafield "dataset" saved
         */
        Vector<Vector<Double>> result = new Vector<>();
        for (double[] it : output) {
            Vector<Double> tmp = new Vector<>();
            for (double it1 : it) {
                tmp.add(it1);
            }
            result.add(tmp);
        }
        return result;
    }

    /*
     * add dataset into class atrribute "dataset"
     */

    public void addLastDataset(Vector<Double> data) {
        dataset.add(data);
    }

    /*
     * erase content in "dataset"
     */

    private void clearDataset() {
        dataset.clear();
    }

    public void addAllDatasets(Vector<Vector<Double>> data) {
        dataset.addAll(data);
    }

    private Vector<Double> getRowtotal() {
        double tmpsumme = 0d;
        Vector<Double> rowtotal = new Vector<>();

        for (Vector<Double> aDataset : dataset) {
            for (Double aDouble : aDataset) {
                tmpsumme += aDouble;
            }
            rowtotal.add(tmpsumme);
            tmpsumme = 0d;
        }

        return rowtotal;
    }

    private Vector<Double> getColumentotal() {
        Vector<Double> columentotal = new Vector<>();

        for (int j = 0; j < dataset.get(0).size(); j++) {
            double tmp = 0d;

            for (Vector<Double> aDataset : dataset) {
                tmp += aDataset.get(j);
            }
            columentotal.add(tmp);
        }

        return columentotal;
    }

    private int getD_FValue() {
        return (dataset.size() - 1) * (dataset.get(0).size() - 1);
    }

    /*
     * die folgenden zwei Methoden geben erneut array zurueck, nicht einheitlich zu obigen Methoden.
     * diese Umsetzung entstand aus nicht sorgfaeltigem Design
     */

    private double[][] getE_table() {
        double total = 0.0d;
        for (int i = 0; i < getRowtotal().size(); i++) {
            total += getRowtotal().get(i);
        }

        Vector<Double> row = getRowtotal();
        Vector<Double> col = getColumentotal();
        double[][] e_table = new double[dataset.size()][dataset.get(0).size()];
        for (int i = 0; i < row.size(); i++) {
            for (int j = 0; j < col.size(); j++) {
                e_table[i][j] = row.get(i) * col.get(j) / total;
            }
        }

        return e_table;
    }

    private double[][] getArrayOfdataset() {
        double[][] o_table = new double[dataset.size()][dataset.get(0).size()];

        for (int i = 0; i < dataset.size(); i++) {
            for (int j = 0; j < dataset.get(0).size(); j++) {
                o_table[i][j] = dataset.get(i).get(j);
            }
        }

        return o_table;
    }

    /*
     * calculate Chi Square value of bigger array
     */

    private double getChi_SquareValue() {
        double result = 0.0d;
        double[][] o_table = getArrayOfdataset();
        double[][] e_table = getE_table();

        for (int i = 0; i < dataset.size(); i++) {
            for (int j = 0; j < dataset.get(0).size(); j++) {
                result += (o_table[i][j] - e_table[i][j]) * (o_table[i][j] - e_table[i][j]) / e_table[i][j];
            }
        }

        return result;
    }

    /*
     * calculate p value for QA and QB with the class from colt.jar Copyright 1999 CERN - European Organization for Nuclear Research
     */

    private double getPValueForChi_Squrare() {
        return Probability.chiSquareComplemented(getD_FValue(), getChi_SquareValue());
    }

    private double getPValueForProportionTest() {
        return Probability.chiSquareComplemented(1.0, getChi_SquareValueWithContinuityCorrectionTwoTailed());
    }

    /*
     * Frage nach Akzeptibalitaet
     */

    public boolean isAcceptableForQA() {
        return getPValueForProportionTest() >= significance;
    }

    public boolean isAcceptableForQB() {
        return getPValueForChi_Squrare() >= significance;
    }

}
