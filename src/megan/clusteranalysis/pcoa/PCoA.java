/*
 * PCoA.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.clusteranalysis.pcoa;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
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
        progress.setMaximum(2L * rank);
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
		System.err.println(StringUtils.toString("%.8f", eigenValues, ", "));

        if (total > 0) {
			for (int j = 0; j < eigenValues.length; j++) {
				percentExplained[j] = 100.0 * eigenValues[j] / total;
			}
			System.err.println("Percent explained:");
			System.err.println(StringUtils.toString("%.1f%%", percentExplained, ", "));
		}

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
	 */
    public void computeLoadingVectorsBiPlot(final int numberOfSamples, final Map<String, float[]> class2counts) {
        loadingVectorsBiPlot.clear();

        final var numberOfClasses = (class2counts == null ? 0 : class2counts.size());

        final var matrixM = new double[numberOfSamples][numberOfClasses]; // sample X class matrix

        // setup classes
        final String[] classNames;
        if (class2counts != null) {
            classNames = class2counts.keySet().toArray(new String[numberOfClasses]);
            for (var classNumber = 0; classNumber < classNames.length; classNumber++) {
                final var name = classNames[classNumber];
                final var counts = class2counts.get(name);
                if (counts != null) {
                    for (int sampleNumber = 0; sampleNumber < counts.length; sampleNumber++) {
                        matrixM[sampleNumber][classNumber] += counts[sampleNumber];
                    }
                }
            }
        } else
            classNames = new String[0];

        // standardize points:

        final var standardizedPoints = Utilities.centerAndScale(points);
        final var S = Utilities.computeCovariance(matrixM, standardizedPoints, true);

        final var values = eigenValues.clone();
        Utilities.scalarMultiply(1.0 / (numberOfSamples - 1), values);
        Utilities.sqrt(values);
        Utilities.invertValues(values);
        var diagonal = Utilities.diag(values);
        var plotProjection = Utilities.multiply(S, diagonal);

        for (var row : plotProjection)
            Utilities.scalarMultiply(0.00001, row);


        var nameAndLoadingVector = (Pair<String, double[]>[])new Pair[numberOfClasses];
        for (var i = 0; i < numberOfClasses; i++) {
            nameAndLoadingVector[i] = (new Pair<>(classNames[i], plotProjection[i]));
        }

        // sort by decreasing length of vector
        Arrays.sort(nameAndLoadingVector, (a, b) -> {
            var aSquaredLength = Utilities.getSquaredLength(a.getSecond());
            var bSquaredLength = Utilities.getSquaredLength(b.getSecond());
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
            System.err.println(pair.getFirst() + ": "+Math.sqrt(Utilities.getSquaredLength(pair.getSecond())));

        }
        */
        loadingVectorsBiPlot.addAll(Arrays.asList(nameAndLoadingVector));
    }

    /**
     * computes the loading vectors as used in biplot
     *
	 */
    public void computeLoadingVectorsTriPlot(final int numberOfSamples, final Map<String, float[]> attribute2counts) {
        loadingVectorsTriPlot.clear();

        final var numberOfAttributes = (attribute2counts == null ? 0 : attribute2counts.size());

        final var matrixM = new double[numberOfSamples][numberOfAttributes]; // sample X class matrix

        // setup attributes
        final String[] attributeNames;
        if (attribute2counts != null) {
            attributeNames = attribute2counts.keySet().toArray(new String[numberOfAttributes]);
            for (var attributeNumber = 0; attributeNumber < attributeNames.length; attributeNumber++) {
                final var name = attributeNames[attributeNumber];
                final var counts = attribute2counts.get(name);
                if (counts != null) {
                    for (var sampleNumber = 0; sampleNumber < counts.length; sampleNumber++) {
                        matrixM[sampleNumber][attributeNumber] += counts[sampleNumber];
                    }
                }
            }
        } else
            attributeNames = new String[0];

        // standardize points:

        final var standardizedPoints = Utilities.centerAndScale(points);
        final var S = Utilities.computeCovariance(matrixM, standardizedPoints, true);

        final var values = eigenValues.clone();
        Utilities.scalarMultiply(1.0 / (numberOfSamples - 1), values);
        Utilities.sqrt(values);
        Utilities.invertValues(values);
        var diagonal = Utilities.diag(values);
        var projection = Utilities.multiply(S, diagonal);

        for (var row : projection)
            Utilities.scalarMultiply(0.00001, row);

        var nameAndLoadingVector = ( Pair<String, double[]>[] )new Pair[numberOfAttributes];
        for (var i = 0; i < numberOfAttributes; i++) {
            nameAndLoadingVector[i] = (new Pair<>(attributeNames[i], projection[i]));
        }

        // sort by decreasing length of vector
        Arrays.sort(nameAndLoadingVector, (a, b) -> {
            var aSquaredLength = Utilities.getSquaredLength(a.getSecond());
            var bSquaredLength = Utilities.getSquaredLength(b.getSecond());
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
            System.err.println(pair.getFirst() + ": "+Math.sqrt(Utilities.getSquaredLength(pair.getSecond())));

        }
        */

        loadingVectorsTriPlot.addAll(Arrays.asList(nameAndLoadingVector));
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
     * @return percent explained
     */
    public double getPercentExplained(int pc) {
        return percentExplained[pc];
    }

}
