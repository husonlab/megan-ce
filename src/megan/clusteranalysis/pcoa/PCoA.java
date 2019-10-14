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
package megan.clusteranalysis.pcoa;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgressListener;
import megan.clusteranalysis.tree.Distances;
import megan.clusteranalysis.tree.Taxa;

import java.util.*;

/**
 * does PCoA calculation
 * Daniel Huson, 9.2012
 */
public class PCoA {
    private final Taxa samples;
    private final Matrix matrixD; // distances between samples
    private final int rank;
    private int numberOfPositiveEigenValues;
    private double[] eigenValues;
    private double[] percentExplained;
    private final Map<String, double[]> sampleName2Point = new HashMap<>();
    private final double[][] points;
    private boolean done = false;

    private final List<Pair<String, double[]>> loadingVectorsBiPlot = new LinkedList<>();
    private final List<Pair<String, double[]>> loadingVectorsTriPlot = new LinkedList<>();

    /**
     * constructor
     *
     * @param samples
     * @param distances
     */
    public PCoA(Taxa samples, Distances distances) {
        this.samples = samples;
        rank = samples.size();
        matrixD = new Matrix(rank, rank);
        for (int i = 0; i < rank; i++) {
            for (int j = 0; j < rank; j++) {
                if (i == j)
                    matrixD.set(i, j, 0);
                else {
                    double d = distances.get(i + 1, j + 1);
                    matrixD.set(i, j, d);
                }
            }
        }
        points = new double[rank][];
    }

    /**
     * calculate the MDS analysis
     */
    public void calculateClassicMDS(ProgressListener progress) throws CanceledException {
        progress.setSubtask("Eigenvalue decomposition");

        progress.setProgress(-1);
        progress.setMaximum(-1);

        loadingVectorsBiPlot.clear();
        loadingVectorsTriPlot.clear();

        // PrintWriter pw = new PrintWriter(System.err);
        // System.err.println("distanceMatrix:");
        //distanceMatrix.print(pw, rank, rank);
        //pw.flush();

        final Matrix centered = Utilities.computeDoubleCenteringOfSquaredMatrix(matrixD);

        //System.err.println("centered:");
        //centered.print(pw, rank, rank);
        //pw.flush();

        final EigenvalueDecomposition eigenValueDecomposition = centered.eig();
        final Matrix eigenVectors = eigenValueDecomposition.getV();
        //System.err.println("eigenVectors:");
        //eigenVectors.print(pw, rank, rank);
        //pw.flush();

        numberOfPositiveEigenValues = 0;
        final Matrix positiveEigenValues = eigenValueDecomposition.getD();
        for (int i = 0; i < rank; i++) {
            if (positiveEigenValues.get(i, i) > 0.000000001)
                numberOfPositiveEigenValues++;
            else
                positiveEigenValues.set(i, i, 0);
        }
        //System.err.println("positiveEigenValues:");
        //positiveEigenValues.print(pw, rank, rank);
        //pw.flush();


        // multiple eigenvectors by sqrt of eigenvalues
        progress.setSubtask("Calculating PCoA");
        progress.setProgress(0);
        progress.setMaximum(2 * rank);
        final Matrix scaledEigenVectors = (Matrix) eigenVectors.clone();
        for (int i = 0; i < rank; i++) {
            for (int j = 0; j < rank; j++) {
                double v = scaledEigenVectors.get(i, j);
                v = v * Math.sqrt(positiveEigenValues.get(j, j));
                scaledEigenVectors.set(i, j, v);
            }
            progress.incrementProgress();
        }
        //System.err.println("scaledEigenVectors:");
        //scaledEigenVectors.print(pw, rank, rank);
        //pw.flush();

        System.err.println("numberOfPositiveEigenValues: " + numberOfPositiveEigenValues);

        // sort indices by eigenValues
        int[] indices = Utilities.sortValues(positiveEigenValues);
        /*
        System.err.println("indices: " + Basic.toString(indices));
        for(int i=0;i<indices.length;i++) {
            System.err.println("positiveEigenValues.get(indices["+i+"],indices["+i+"])="+positiveEigenValues.get(indices[i], indices[i]));
        }
        */

        eigenValues = new double[numberOfPositiveEigenValues];
        percentExplained = new double[numberOfPositiveEigenValues];

        double total = 0;
        for (int j = 0; j < numberOfPositiveEigenValues; j++) {
            total += eigenValues[j] = positiveEigenValues.get(indices[j], indices[j]);
        }

        System.err.println("Positive eigenvalues:");
        System.err.println(Basic.toString("%.8f", eigenValues, ", "));

        if (total > 0) {
            for (int j = 0; j < eigenValues.length; j++) {
                percentExplained[j] = 100.0 * eigenValues[j] / total;
            }
        }

        System.err.println("Percent explained:");
        System.err.println(Basic.toString("%.1f%%", percentExplained, ", "));

        for (int i = 0; i < rank; i++) {
            String name = samples.getLabel(i + 1);
            double[] vector = new double[numberOfPositiveEigenValues];
            sampleName2Point.put(name, vector);
            for (int j = 0; j < numberOfPositiveEigenValues; j++) {
                vector[j] = scaledEigenVectors.get(i, indices[j]);
            }
            points[i] = vector;
            progress.incrementProgress();
        }
        done = true;
    }

    public int getNumberOfPositiveEigenValues() {
        return numberOfPositiveEigenValues;
    }

    /**
     * given i-th, j-th and k-th coordinates for given name
     *
     * @param i
     * @param j
     * @param k
     * @param sampleName
     * @return (i, j, k)
     */
    public double[] getProjection(int i, int j, int k, String sampleName) {
        double[] vector = sampleName2Point.get(sampleName);
        return new double[]{i < numberOfPositiveEigenValues ? vector[i] : 0, j < numberOfPositiveEigenValues ? vector[j] : 0, k < numberOfPositiveEigenValues ? vector[k] : 0};
    }

    /**
     * get rank
     *
     * @return rank
     */
    public int getRank() {
        return rank;
    }


    public boolean isDone() {
        return done;
    }

    public double[] getEigenValues() {
        return eigenValues;
    }

    /**
     * computes the loading vectors as used in biplot
     *
     * @param numberOfSamples
     * @param class2counts
     */
    public void computeLoadingVectorsBiPlot(final int numberOfSamples, final Map<String, float[]> class2counts) {
        loadingVectorsBiPlot.clear();

        final int numberOfClasses = (class2counts == null ? 0 : class2counts.size());

        final double[][] matrixM = new double[numberOfSamples][numberOfClasses]; // sample X class matrix

        // setup classes
        final String[] classNames;
        if (class2counts != null) {
            classNames = class2counts.keySet().toArray(new String[numberOfClasses]);
            for (int classNumber = 0; classNumber < classNames.length; classNumber++) {
                final String name = classNames[classNumber];
                final float[] counts = class2counts.get(name);
                if (counts != null) {
                    for (int sampleNumber = 0; sampleNumber < counts.length; sampleNumber++) {
                        matrixM[sampleNumber][classNumber] += counts[sampleNumber];
                    }
                }
            }
        } else
            classNames = new String[0];

        // standardize points:

        final double[][] standardizedPoints = Utilities.centerAndScale(points);
        final double[][] S = Utilities.computeCovariance(matrixM, standardizedPoints, true);

        final double[] values = eigenValues.clone();
        Utilities.scalarMultiply(1.0 / (numberOfSamples - 1), values);
        Utilities.sqrt(values);
        Utilities.invertValues(values);
        double[][] diagonal = Utilities.diag(values);
        double[][] biplotProjection = Utilities.multiply(S, diagonal);

        for (double[] row : biplotProjection)
            Utilities.scalarMultiply(0.00001, row);


        Pair<String, double[]>[] nameAndLoadingVectorBiPlot = new Pair[numberOfClasses];
        for (int i = 0; i < numberOfClasses; i++) {
            nameAndLoadingVectorBiPlot[i] = (new Pair<>(classNames[i], biplotProjection[i]));
        }

        // sort by decreasing length of vector
        Arrays.sort(nameAndLoadingVectorBiPlot, (a, b) -> {
            double aSquaredLength = Utilities.getSquaredLength(a.getSecond());
            double bSquaredLength = Utilities.getSquaredLength(b.getSecond());
            if (aSquaredLength > bSquaredLength)
                return -1;
            else if (aSquaredLength < bSquaredLength)
                return 1;
            else
                return a.getFirst().compareTo(b.getFirst());
        });

        /*
        for (int i = 0; i < nameAndLoadingVector.length; i++) {
            Pair<String, double[]> pair = nameAndLoadingVector[i];
            System.err.println(pair.get1() + ": "+Math.sqrt(Utilities.getSquaredLength(pair.getSecond())));

        }
        */
        loadingVectorsBiPlot.addAll(Arrays.asList(nameAndLoadingVectorBiPlot));
    }

    /**
     * computes the loading vectors as used in biplot
     *
     * @param numberOfSamples
     */
    public void computeLoadingVectorsTriPlot(final int numberOfSamples, final Map<String, float[]> attribute2counts) {
        loadingVectorsTriPlot.clear();

        final int numberOfAttributes = (attribute2counts == null ? 0 : attribute2counts.size());

        final double[][] matrixM = new double[numberOfSamples][numberOfAttributes]; // sample X class matrix

        // setup attributes
        final String[] attributeNames;
        if (attribute2counts != null) {
            attributeNames = attribute2counts.keySet().toArray(new String[numberOfAttributes]);
            for (int attributeNumber = 0; attributeNumber < attributeNames.length; attributeNumber++) {
                final String name = attributeNames[attributeNumber];
                final float[] counts = attribute2counts.get(name);
                if (counts != null) {
                    for (int sampleNumber = 0; sampleNumber < counts.length; sampleNumber++) {
                        matrixM[sampleNumber][attributeNumber] += counts[sampleNumber];
                    }
                }
            }
        } else
            attributeNames = new String[0];

        // standardize points:

        final double[][] standardizedPoints = Utilities.centerAndScale(points);
        final double[][] S = Utilities.computeCovariance(matrixM, standardizedPoints, true);

        final double[] values = eigenValues.clone();
        Utilities.scalarMultiply(1.0 / (numberOfSamples - 1), values);
        Utilities.sqrt(values);
        Utilities.invertValues(values);
        double[][] diagonal = Utilities.diag(values);
        double[][] biplotProjection = Utilities.multiply(S, diagonal);

        for (double[] row : biplotProjection)
            Utilities.scalarMultiply(0.00001, row);

        Pair<String, double[]>[] nameAndLoadingVectorBiPlot = new Pair[numberOfAttributes];
        for (int i = 0; i < numberOfAttributes; i++) {
            nameAndLoadingVectorBiPlot[i] = (new Pair<>(attributeNames[i], biplotProjection[i]));
        }

        // sort by decreasing length of vector
        Arrays.sort(nameAndLoadingVectorBiPlot, (a, b) -> {
            double aSquaredLength = Utilities.getSquaredLength(a.getSecond());
            double bSquaredLength = Utilities.getSquaredLength(b.getSecond());
            if (aSquaredLength > bSquaredLength)
                return -1;
            else if (aSquaredLength < bSquaredLength)
                return 1;
            else
                return a.getFirst().compareTo(b.getFirst());
        });

        /*
        for (int i = 0; i < nameAndLoadingVector.length; i++) {
            Pair<String, double[]> pair = nameAndLoadingVector[i];
            System.err.println(pair.get1() + ": "+Math.sqrt(Utilities.getSquaredLength(pair.getSecond())));

        }
        */

        loadingVectorsTriPlot.addAll(Arrays.asList(nameAndLoadingVectorBiPlot));
    }

    public List<Pair<String, double[]>> getLoadingVectorsBiPlot() {
        return loadingVectorsBiPlot;
    }

    public List<Pair<String, double[]>> getLoadingVectorsTriPlot() {
        return loadingVectorsTriPlot;
    }

    /**
     * get percent explained for the given PC
     *
     * @param pc
     * @return percent explained
     */
    public double getPercentExplained(int pc) {
        return percentExplained[pc];
    }

}
