/*
 * CircularSplitWeights.java Copyright (C) 2024 Daniel H. Huson
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
package megan.clusteranalysis.nnet;


import megan.clusteranalysis.tree.Distances;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Given a circular ordering and a distance matrix,
 * computes the unconstrained or constrained least square weighted splits
 * <p/>
 * For all vectors, the canonical ordering of pairs is (0,1),(0,2),...,(0,n-1),(1,2),(1,3),...,(1,n-1), ...,(n-1,n)
 * <p/>
 * (i,j) ->  (2n - i -3)i/2 + j-1  .
 * <p/>
 * Increase i -> increase index by n-i-2
 * Decrease i -> decrease index by n-i-1.
 * <p/>
 * <p/>
 * x[i][j] is the split {i+1,i+2,...,j} | -------
 */
class CircularSplitWeights {
    /* Epsilon constant for the conjugate gradient algorithm */
    private static final double CG_EPSILON = 0.0001;

    static public SplitSystem compute(int[] ordering, Distances dist, boolean constrained, double cutoff) {
        final int ntax = dist.getNtax();
        final int npairs = (ntax * (ntax - 1)) / 2;

        //Handle n=1,2 separately.
        if (ntax == 1)
            return new SplitSystem();
        if (ntax == 2) {
            SplitSystem smallSplits = new SplitSystem();
            float d_ij = (float) dist.get(ordering[1], ordering[2]);
            if (d_ij > 0.0) {
                BitSet A = new BitSet();
                A.set(ordering[1]);
                Split split = new Split();
                split.set(A, ntax, d_ij);
                smallSplits.addSplit(split);
            }
            return smallSplits;
        }

        /* Re-order taxa so that the ordering is 0,1,2,...,n-1 */
        final double[] d = setupD(dist, ordering);
        final double[] x = new double[npairs];

        if (!constrained)
            CircularSplitWeights.runUnconstrainedLS(ntax, d, x);
        else // do constrained optimization
        {
            final double[] W = setupW(dist.getNtax());
            /* Find the constrained optimal values for x */
            runActiveConjugate(ntax, d, W, x);
        }

        /* Construct the splits with the appropriate weights */
        SplitSystem splits = new SplitSystem();
        int index = 0;
        for (int i = 0; i < ntax; i++) {
            BitSet A = new BitSet();
            for (int j = i + 1; j < ntax; j++) {
                A.set(ordering[j + 1]);
                if (x[index] > cutoff) {
                    Split split = new Split();
                    split.set(A, ntax, (float) (x[index]));
                    splits.addSplit(split);
                }
                index++;
            }
        }
        return splits;
    }

    /**
     * setup working distance so that ordering is trivial.
     * Note the the code assumes that taxa are labeled 0..ntax-1 and
     * we do the transition here. It is undone when extracting the splits
     *
     * @param dist     Distances block
     * @param ordering circular ordering
     * @return double[] distances stored as a vector
     */
    static private double[] setupD(Distances dist, int[] ordering) {
        final int ntax = dist.getNtax();
        final int npairs = ((ntax - 1) * ntax) / 2;

        double[] d = new double[npairs];
        int index = 0;
        for (int i = 0; i < ntax; i++)
            for (int j = i + 1; j < ntax; j++)
                d[index++] = dist.get(ordering[i + 1], ordering[j + 1]);
        return d;
    }

    static private double[] setupW(int ntax) {
        final int npairs = ((ntax - 1) * ntax) / 2;
        double[] v = new double[npairs];

        int index = 0;
        for (int i = 0; i < ntax; i++)
            for (int j = i + 1; j < ntax; j++) {
                    v[index] = 1.0;
                index++;
            }
        return v;
    }


    /**
     * Compute the branch lengths for unconstrained least squares using
     * the formula of Chepoi and Fichet (this takes O(N^2) time only!).
     *
     * @param n the number of taxa
     * @param d the distance matrix
     * @param x the split weights
     */
    static private void runUnconstrainedLS(int n, double[] d, double[] x) {
        int index = 0;

        for (int i = 0; i <= n - 3; i++) {
            //index = (i,i+1)
            //x[i,i+1] = (d[i][i+1] + d[i+1][i+2] - d[i,i+2])/2
            x[index] = (d[index] + d[index + (n - i - 2) + 1] - d[index + 1]) / 2.0;
            index++;
            for (int j = i + 2; j <= n - 2; j++) {
                //x[i][j] = ( d[i,j] + d[i+1,j+1] - d[i,j+1] - d[i+1][j])
                x[index] = (d[index] + d[index + (n - i - 2) + 1] - d[index + 1] - d[index + (n - i - 2)]) / 2.0;
                index++;
            }
            //index = (i,n-1)

            if (i == 0) //(0,n-1)
                x[index] = (d[0] + d[n - 2] - d[2 * n - 4]) / 2.0; //(d[0,1] + d[0,n-1] - d[1,n-1])/2
            else
                //x[i][n-1] == (d[i,n-1] + d[i+1,0] - d[i,0] - d[i+1,n-1])
                x[index] = (d[index] + d[i] - d[i - 1] - d[index + (n - i - 2)]) / 2.0;
            index++;
        }
        //index = (n-2,n-1)
        x[index] = (d[index] + d[n - 2] - d[n - 3]) / 2.0;
    }

    /**
     * Returns the array indices for the smallest propKept proportion of negative values in x.
     * In the case of ties, priority is given to the earliest entries.
     * Size of resulting array will be propKept * (number of negative entries) rounded up.
     *
     * @param x        returns an array
     * @param propKept the
     * @return int[] array of indices
     */
    static private int[] worstIndices(double[] x, double propKept) {
        if (propKept == 0)
            return null;

        int n = x.length;

        int numNeg = 0;
        for (double aX1 : x)
            if (aX1 < 0.0)
                numNeg++;

        if (numNeg == 0)
            return null;

        double[] xcopy = new double[numNeg];
        int j = 0;
        for (double aX : x)
            if (aX < 0.0)
                xcopy[j++] = aX;

        Arrays.sort(xcopy);

        int nkept = (int) Math.ceil(propKept * numNeg);
        double cutoff = xcopy[nkept - 1];

        int[] result = new int[nkept];
        int front = 0, back = nkept - 1;

        for (int i = 0; i < n; i++) {
            if (x[i] < cutoff)
                result[front++] = i;
            else if (x[i] == cutoff) {
                if (back >= front)
                    result[back--] = i;
            }
        }
        return result;
    }

    /**
     * Uses an active set method with the conjugate gradient algorithm to find x that minimises
     * <p/>
     * (Ax - d)W(Ax-d)
     * <p/>
     * Here, A is the design matrix for the set of cyclic splits with ordering 0,1,2,...,n-1
     * d is the distance vector, with pairs in order (0,1),(0,2),...,(0,n-1),(1,2),(1,3),...,(1,n-1), ...,(n-1,n)
     * W is a vector of variances for d, with pairs in same order as d.
     * x is a vector of split weights, with pairs in same order as d. The split (i,j), for i<j, is {i,i+1,...,j-1}| rest
     *
     * @param ntax The number of taxa
     * @param d    the distance matrix
     * @param W    the weight matrix
     * @param x    the split weights
     */
    static private void runActiveConjugate(int ntax, double[] d, double[] W, double[] x) {
        int npairs = d.length;
        if (W.length != npairs || x.length != npairs)
            throw new IllegalArgumentException("Vectors d,W,x have different dimensions");

        CircularSplitWeights.runUnconstrainedLS(ntax, d, x);
        boolean all_positive = true;
        for (int k = 0; k < npairs; k++)
            if (x[k] < 0.0) {
                all_positive = false;
                break;
            }

        if (all_positive)
            return;

        final boolean[] active = new boolean[npairs];

        double[] y = new double[npairs];
        double[] AtWd = new double[npairs];
        for (int k = 0; k < npairs; k++)
            y[k] = W[k] * d[k];
        CircularSplitWeights.calculateAtx(ntax, y, AtWd);

        final double[] r = new double[npairs];
        final double[] w = new double[npairs];
        final double[] p = new double[npairs];

        final double[] old_x = new double[npairs];
        Arrays.fill(old_x, 1.0);

        boolean first_pass = true;

        while (true) {
            while (true) {
                if(first_pass)
                    first_pass=false;
                else
                    CircularSplitWeights.circularConjugateGrads(ntax, npairs, r, w, p, y, W, AtWd, active, x);

                final int[] entriesToContract = worstIndices(x, 0.6);
                if (entriesToContract != null) {
                    for (int index : entriesToContract) {
                        x[index] = 0.0;
                        active[index] = true;
                    }
                    CircularSplitWeights.circularConjugateGrads(ntax, npairs, r, w, p, y, W, AtWd, active, x);
                }
                int min_i = -1;
                double min_xi = -1.0;
                for (int i = 0; i < npairs; i++) {
                    if (x[i] < 0.0) {
                        double xi = (old_x[i]) / (old_x[i] - x[i]);
                        if ((min_i == -1) || (xi < min_xi)) {
                            min_i = i;
                            min_xi = xi;
                        }
                    }
                }

                if (min_i == -1)
                    break;
                else {
                    for (int i = 0; i < npairs; i++) {
                        if (!active[i])
                            old_x[i] += min_xi * (x[i] - old_x[i]);
                    }
                    active[min_i] = true;
                    x[min_i] = 0.0;
                }
            }

            calculateAb(ntax, x, y);
            for (int i = 0; i < npairs; i++)
                y[i] *= W[i];
            calculateAtx(ntax, y, r); /* r = AtWAx */

            int min_i = -1;
            double min_grad = 1.0;
            for (int i = 0; i < npairs; i++) {
                r[i] -= AtWd[i];
                r[i] *= 2.0;
                if (active[i]) {
                    double grad_ij = r[i];
                    if ((min_i == -1) || (grad_ij < min_grad)) {
                        min_i = i;

                        min_grad = grad_ij;
                    }
                }
            }

            if ((min_i == -1) || (min_grad > -0.0001))
                return;
            else
                active[min_i] = false;
        }
    }

    /* Compute the row sum in d. */

    static private double rowSum(int n, double[] d, int k) {
        double r = 0;
        int index = 0;

        if (k > 0) {
            index = k - 1;
            for (int i = 0; i < k; i++) {
                r += d[index];
                index += (n - i - 2);
            }
            index++;
        }
        for (int j = k + 1; j < n; j++)
            r += d[index++];

        return r;
    }


    /**
     * Computes p = A^Td, where A is the topological matrix for the
     * splits with circular ordering 0,1,2,....,ntax-1
     * *
     *
     * @param n number of taxa
     * @param d distance matrix
     * @param p the result
     */
    static private void calculateAtx(int n, double[] d, double[] p) {
        int index = 0;
        for (int i = 0; i < n - 1; i++) {
            p[index] = rowSum(n, d, i + 1);
            index += (n - i - 1);
        }

        index = 1;
        for (int i = 0; i < n - 2; i++) {
             p[index] = p[index - 1] + p[index + (n - i - 2)] - 2 * d[index + (n - i - 2)];
            index += (n - i - 2) + 1;
        }

        for (int k = 3; k <= n - 1; k++) {
            index = k - 1;
            for (int i = 0; i < n - k; i++) {
                p[index] = p[index - 1] + p[index + n - i - 2] - p[index + n - i - 3] - 2.0 * d[index + n - i - 2];
                index += (n - i - 2) + 1;
            }
        }
    }

    /**
     * Computes d = Ab, where A is the topological matrix for the
     * splits with circular ordering 0,1,2,....,ntax-1
     *
     * @param n number of taxa
     * @param b split weights
     * @param d pairwise distances from split weights
     */
    static private void calculateAb(int n, double[] b, double[] d) {

        {
            int dindex = 0;
            for (int i = 0; i < n - 1; i++) {
                double d_ij = 0.0;
                int index = i - 1;
                for (int k = 0; k < i; k++) {
                    d_ij += b[index];
                    index += (n - k - 2);
                }
                index++;
                for (int k = i + 1; k < n ; k++)
                    d_ij += b[index++];

                d[dindex] = d_ij;
                dindex += (n - i - 2) + 1;
            }
        }

        {
            int index = 1;
            for (int i = 0; i <= n - 3; i++) {
                d[index] = d[index - 1] + d[index + (n - i - 2)] - 2 * b[index - 1];
                index += 1 + (n - i - 2);
            }
        }


        for (int k = 3; k <n ; k++) {
            int index = k - 1;
            for (int i = 0; i < n - k; i++) {
                d[index] = d[index - 1] + d[index + (n - i - 2)] - d[index + (n - i - 2) - 1] - 2.0 * b[index - 1];
                index += 1 + (n - i - 2);
            }
        }
    }


    /**
     * Computes sum of squares of the lower triangle of the matrix x
     *
     * @param x the matrix
     * @return sum of squares of the lower triangle
     */
    static private double norm(double[] x) {
        double norm = 0.0;
        for (double v : x) {
            norm += v * v;
        }
        return norm;
    }


    /**
     * Conjugate gradient algorithm solving A^tWA x = b (where b = AtWd)
     * such that all x[i][j] for which active[i][j] = true are set to zero.
     * We assume that x[i][j] is zero for all active i,j, and use the given
     * values for x as our starting vector.
     *
     * @param ntax   the number of taxa
     * @param npairs dimension of b and x
     * @param r      stratch matrix
     * @param w      stratch matrix
     * @param p      stratch matrix
     * @param y      stratch matrix
     * @param W      the W matrix
     * @param b      the b matrix
     * @param active the active constraints
     * @param x      the x matrix
     */
    static private void circularConjugateGrads(int ntax, int npairs,
                                               double[] r, double[] w, double[] p, double[] y,
                                               double[] W, double[] b,
                                               boolean[] active, double[] x) {
        int kmax = ntax * (ntax - 1) / 2;

        calculateAb(ntax, x, y);

        for (int k = 0; k < npairs; k++)
            y[k] = W[k] * y[k];
        calculateAtx(ntax, y, r);

        for (int k = 0; k < npairs; k++)
            if (!active[k])
                r[k] = b[k] - r[k];
            else
                r[k] = 0.0;

        double rho = norm(r);
        double rho_old = 0;

        double e_0 = CG_EPSILON * Math.sqrt(norm(b));
        int k = 0;

        while ((rho > e_0 * e_0) && (k < kmax)) {

            k = k + 1;
            if (k == 1) {
                System.arraycopy(r, 0, p, 0, npairs);
            } else {
                double beta = rho / rho_old;
                for (int i = 0; i < npairs; i++)
                    p[i] = r[i] + beta * p[i];

            }

            calculateAb(ntax, p, y);
            for (int i = 0; i < npairs; i++)
                y[i] *= W[i];

            calculateAtx(ntax, y, w);
            for (int i = 0; i < npairs; i++)
                if (active[i])
                    w[i] = 0.0;

            double alpha = 0.0;
            for (int i = 0; i < npairs; i++)
                alpha += p[i] * w[i];
            alpha = rho / alpha;

            for (int i = 0; i < npairs; i++) {
                x[i] += alpha * p[i];
                r[i] -= alpha * w[i];
            }
            rho_old = rho;
            rho = norm(r);
        }
    }
}
