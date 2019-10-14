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

import jloda.swing.util.ArgsOptions;
import jloda.util.CanceledException;
import jloda.util.ProgressCmdLine;
import jloda.util.ProgressListener;
import jloda.util.UsageException;

import java.io.*;
import java.util.*;

/**
 * compares two datasets and ranks entries by the significance of their difference
 */
public class ResamplingMethod {
    private int resamplingSize = 10000;
    private int repeatitions = 20000;
    private double p_left = 5;
    private double p_right = 95;
    private String inputFileName1;
    private String inputFileName2;
    private boolean useSecond = false;
    private String outputFileName = "out.txt";
    private final Random random;
    private boolean optionWarningOnBoundary = true;
    private ProgressListener progressListener = new ProgressCmdLine();
    /*
     * neu attributes for implements of the future interface
     */
    private Map<Integer, Float> input1;
    private Map<Integer, Float> input2;
    private final HashMap<Integer, float[]> unionOfInputs = new HashMap<>();
    private final HashMap<Integer, Double> resultOfScalevalue = new HashMap<>();

    /**
     * constructor
     */
    public ResamplingMethod() {
        random = new Random();
    }

    /**
     * constructor
     *
     * @param seed random generator seed
     */
    public ResamplingMethod(long seed) {
        random = new Random(seed);
    }

    /*
     * create one new Map that save all keys(genenames) from m1 und m2
     * (note:these 2 datasets must not include only the same gene, because the returned result is their union ),
     * the with keys associated values will be saved in form of int[].
     * @param m1
     * @param m2
     */

    public void setInput(Map<Integer, Float> input1, Map<Integer, Float> input2) {
        this.input1 = input1;

        this.input2 = input2;

        //insert element in the order of keys in m1
        for (Integer key1 : this.input1.keySet()) {
            float[] value1 = new float[2];
            //int[0] saves value from m1, int[1] saves value from m2
            value1[0] = this.input1.get(key1);
            //wenn m2 has the same key, the associated value with this key in m2 should be saved right now in int[];
            //otherweise we see the associated value in m2 as zero
            if (this.input2.containsKey(key1)) {
                value1[1] = this.input2.get(key1);
            }
            unionOfInputs.put(key1, value1);
        }
        //insert left elements from m2 that are not included in m1;
        //their associated values in m1 should be seen as zero
        for (Integer key2 : this.input2.keySet()) {
            if (!unionOfInputs.containsKey(key2)) {
                float[] value2 = new float[2];
                value2[1] = this.input2.get(key2);
                unionOfInputs.put(key2, value2);
            }
        }
    }

    /**
     * configure a comparison
     *
     * @param resamplingSize
     * @param repeatitions
     * @param p_left
     * @param useSecond
     * @param optionWarningOnBoundary
     * @param seed
     */
    public void configure(int resamplingSize, int repeatitions, int p_left, boolean useSecond, boolean optionWarningOnBoundary, int seed) {
        this.resamplingSize = resamplingSize;
        this.repeatitions = repeatitions;
        this.p_left = p_left;
        this.useSecond = useSecond;
        this.optionWarningOnBoundary = optionWarningOnBoundary;
        if (seed != 0) random.setSeed(seed);
    }

    /* options: */

    public int getOptionResamplingSize() {
        return resamplingSize;
    }

    public void setOptionResamplingSize(int resamplingSize) {
        this.resamplingSize = resamplingSize;
    }

    public int getOptionRepeatitions() {
        return repeatitions;
    }

    public void setOptionRepeatitions(int repeats) {
        this.repeatitions = repeats;
    }


    public double getOptionLeftPercentile() {
        return p_left;
    }

    public void setOptionLeftPercentile(double leftPercentile) {
        this.p_left = leftPercentile;
    }

    private double getOptionRightPercentile() {
        return p_right;
    }

    public boolean getOptionUseSecond() {
        return useSecond;
    }

    public void setOptionUseSecond(boolean useSecond) {
        this.useSecond = useSecond;
    }

    public boolean getOptionWarnOnBoundary() {
        return optionWarningOnBoundary;
    }

    public void setOptionWarnOnBoundary(boolean optionWarningOnBoundary) {
        this.optionWarningOnBoundary = optionWarningOnBoundary;
    }


    /**
     * determine whether statistical test is applicable
     *
     * @return true, if applicable
     */
    public boolean isApplicable() {
        if (resamplingSize < 1) {
            System.err.println("Illegal resamplingsize: " + resamplingSize);
            return false;
        }
        if (repeatitions < 1) {
            System.err.println("Illegal repeatitions: " + repeatitions);
            return false;
        }
        if ((p_left < 0) || (p_left > 100)) {
            System.err.println("Illegal percentile: " + p_left);
            return false;
        }
        if (input1 != null && input2 != null && (input1.isEmpty() || input2.isEmpty())) {
            System.err.println("Illegal empty dataset");
            return false;
        }
        float sum = 0;
        if (input1 != null && useSecond) {
            for (float i : input1.values()) {
                sum = sum + i;
            }
            if (sum == 0) {
                System.err.println("Illegal: the 1st data set contains all zeros, resampling not possible!");
                return false;
            }
            if (Objects.requireNonNull(input2).containsValue(0)) {
                System.err.println("Illegal: the data set as percentile set contains zero value!");
                return false;
            } else return true;
        } else if (input2 != null) {
            for (float j : input2.values()) {
                sum = sum + j;
            }
            if (sum == 0) {
                System.err.println("Illegal: the 2nd data set contains all zeros, resampling not possible!");
                return false;
            }
            if (Objects.requireNonNull(input1).containsValue(0)) {
                System.err.println("Illegal: the data set as percentile set contains zero value!");
                return false;
            }
        }
        return true;
    }

    /**
     * apply the statistical test
     */
    public void apply(ProgressListener progressListener) throws CanceledException {
        this.progressListener = progressListener;
        progressListener.setTasks("Resampling statistical test", "setup");
        System.err.println("Starting random sampling comparison test...");

        //display some information
        System.err.println("Resampling size: " + getOptionResamplingSize());
        System.err.println("Repeatitions: " + getOptionRepeatitions());
        System.err.println("Left " + getOptionLeftPercentile() + ", right " + getOptionRightPercentile());
        //System.err.println("Choose percentiles from: the " + (useSecond?"second":"first")+" input Dataset");
        //System.err.println("########################");

        /*
         * convert datatype of the input data in array
         */
        Set<Integer> genelist = unionOfInputs.keySet();
        float[] sample1 = new float[unionOfInputs.size()];
        float[] sample2 = new float[unionOfInputs.size()];
        int count1 = 0;
        for (Integer s1 : genelist) {
            //System.err.println("value of "+s1+" from Map m1 would be read: "+unionOfDataquelle.get(s1)[0]);
            sample1[count1++] = unionOfInputs.get(s1)[0];
        }
        int count2 = 0;
        for (Integer s2 : genelist) {
            //System.err.println("value of "+s2+" from Map m2 would be read: "+unionOfDataquelle.get(s2)[1]);
            sample2[count2++] = unionOfInputs.get(s2)[1];
        }

        /*
         * now goto the old process
         */
        float[] median = computeMedians(sample1, sample2, resamplingSize, repeatitions);

        float[][] p05_95 = computePercentileLimits(useSecond ? sample2 : sample1, resamplingSize, repeatitions, p_left, p_right);
        System.err.println("########################");
        //check array of p05_95 and median
        for (int i = 0; i < median.length; i++) {
            System.err.println("m_" + i + " " + median[i] + "  " + "p05_" + i + " " + p05_95[i][0] + "  " + "p95_" + i + " " + p05_95[i][1] + "  " + "p50_" + i + " " + p05_95[i][2]);
        }

        Result[] differ = compareMedianToPercentilslimits(median, p05_95);

        /*
         * convert result in Map
         */
        int count3 = 0;
        for (Integer s3 : genelist) {
            System.err.println("scale value of " + s3 + " pushed into the result");
            resultOfScalevalue.put(s3, differ[count3++].getScale());
        }
    }


    /**
     * get the output, amap of strings to values
     *
     * @return output
     */
    public Map<Integer, Double> getOutput() {
        return resultOfScalevalue;
    }

    /**
     * run the program
     *
     * @param args
     * @throws Exception
     */
    static public void main(String[] args) throws Exception {

        ResamplingMethod compare = new ResamplingMethod();

        if (args.length > 0)
            compare.setParametersFromCommandLine(args);
        else
            compare.setParametersFromConsole();
        compare.run();
    }

    /**
     * run the algorithm
     *
     * @throws Exception
     */
    private void run() throws Exception {

        float[] sample1 = readInput(inputFileName1);
        float[] sample2 = readInput(inputFileName2);

        if (sample1.length != sample2.length)
            throw new Exception("Samples have different lengths: " + sample1.length + " vs " + sample2.length);

        float[] median = computeMedians(sample1, sample2, resamplingSize, repeatitions);

        float[][] p05_95 = computePercentileLimits(useSecond ? sample2 : sample1, resamplingSize, repeatitions, p_left, p_right);
        System.err.println("########################");
        //check array of p05_95 and median
        for (int i = 0; i < median.length; i++) {
            System.err.println("m_" + i + " " + median[i] + "  " + "p05_" + i + " " + p05_95[i][0] + "  " + "p95_" + i + " " + p05_95[i][1] + "  " + "p50_" + i + " " + p05_95[i][2]);
        }

        Result[] differ = compareMedianToPercentilslimits(median, p05_95);

        //display some information
        System.err.println("Choose medians from: " + inputFileName1 + ", " + inputFileName2);
        System.err.println("Choose percentiles from: " + (useSecond ? "second" : "first") + " input file");
        System.err.println("Repeats: " + getOptionRepeatitions() + ", sample size: " + getOptionResamplingSize() + "");
        System.err.println("Result will be stored in " + outputFileName);
        System.err.println("Left " + getOptionLeftPercentile() + ", right " + getOptionRightPercentile());
        System.err.println("########################");

        //copy the result and sort this copy
        Result[] output = new Result[differ.length];
        System.arraycopy(differ, 0, output, 0, differ.length);
        for (int i = 0; i < output.length; i++)
            output[i].setGenNum(++i);
        Arrays.sort(output, Result.getScaleComparator());
        //quickSortInScale(output, 0, output.length - 1);
        reversePrint(output);

        createOutputFilesReverse(output, outputFileName, false);
    }

    /**
     * generate a random sample
     *
     * @param sample
     * @param size
     * @return random sample
     */
    private float[] getRandomSample(float[] sample, int size) {
        float[] commulativeSum = new float[sample.length];
        float[] result = new float[sample.length];
        int sum = 0;

        for (int i = 0; i < sample.length; i++) {
            sum += sample[i];
            commulativeSum[i] = sum;
        }

        /*
         * added on 14.11.07
         * if no values in the dataset, also all values are 0, return int[] mit 0;
         */
        if (sum == 0) return result;
        //

        for (int j = 0; j < size; j++) {
            //trying another position for new Random()
            //Random rnd = new Random();
            //trying different seeds
            //rnd.setSeed(System.nanoTime());
            int r = random.nextInt(sum) + 1; //1<=r<=sum
            //check generation of random numbers
            //System.err.println("radomnum "+j+" "+r);
            //
            int bucket = Arrays.binarySearch(commulativeSum, r);
            /*
             * ?which positon for negativ Index
             */
            if (bucket < 0) bucket = -bucket - 1;
            //			check count of bucket
            //System.err.println("Gene"+bucket+" is selected");
            //
            result[bucket]++;
        }
        //check number of every bucket
        /*
           for (int i = 0; i < result.length; i++){
               System.err.println(result[i]);
           }
        */

        //check summe of all numbers
        /*
           int checksume = 0;
           for (int i = 0; i < result.length; i++){
               checksume += result[i];
           }
           System.err.println(checksume);
        */
        return result;

    }

    /**
     * get the array of absolute differences between N1 and N2
     *
     * @param N1
     * @param N2
     * @return absolute differences
     */
    private float[] getAbsoluteDifference(float[] N1, float[] N2) {

        assert (N1.length == N2.length);

        float[] result = new float[N1.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Math.abs(N1[i] - N2[i]);
        }

        return result;
    }

    /**
     * extract a row from a two dimensional matrix
     *
     * @param twoDimArray
     * @param r
     * @return r-th row
     */
    private float[] extractRow(float[][] twoDimArray, int r) throws CanceledException {

        assert ((r >= 0) && (r < twoDimArray[0].length));

        float[] row = new float[twoDimArray.length];
        progressListener.setMaximum(row.length);

        for (int i = 0; i < row.length; i++) {
            row[i] = twoDimArray[i][r];
            progressListener.setProgress(i);
        }

        return row;
    }

    /**
     * get the median of a row of numbers
     *
     * @param row
     * @return median
     */
    private float getMedian(float[] row) {
        /*
        int[] tmp = new int[row.length];

        System.arraycopy(row, 0, tmp, 0, row.length);

        Arrays.sort(tmp);
        return tmp[(tmp.length-1) / 2];
        // don't rewrite to use average with number of values is even because median must be one of the values.
        */

        //10.30.2007 try to output average value
        float sum = 0;
        for (float p : row) {
            sum += p;
        }
        return sum / row.length;
        //
    }

    /**
     * computes the medians for the two samples
     *
     * @param sample1
     * @param sample2
     * @param resamplingSize
     * @param repeats
     * @return medians
     */
    private float[] computeMedians(float[] sample1, float[] sample2, int resamplingSize, int repeats) throws CanceledException {

        assert (sample1.length == sample2.length);

        int length = sample1.length;

        final float[][] diff = new float[repeats][length];

        progressListener.setSubtask("random sampling");
        progressListener.setMaximum(repeats);
        for (int i = 0; i < repeats; i++) {
            float[] N1 = getRandomSample(sample1, resamplingSize);
            /*
            //check N1 for median
            BufferedWriter RandomsampleN1ofMedian = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("RandomsampleN1ofMedian_rep"+(i+1)+".txt", false)));
            for (int res1 : N1) {
            	RandomsampleN1ofMedian.write(res1 + "\n");
            }
            RandomsampleN1ofMedian.close();
            //
            */
            float[] N2 = getRandomSample(sample2, resamplingSize);
            /*
            //check N2 for median
            BufferedWriter RandomsampleN2ofMedian = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("RandomsampleN2ofMedian_rep"+(i+1)+".txt", false)));
            for (int res2 : N2) {
            	RandomsampleN2ofMedian.write(res2 + "\n");
            }
            RandomsampleN2ofMedian.close();
            //
            */
            diff[i] = getAbsoluteDifference(N1, N2);
            /*
            //check difference of N1, N2 for median
            BufferedWriter DiffofN1N2ofMedian = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("DiffofN1N2ofMedian_rep"+(i+1)+".txt", false)));
            for (int res3 : diff[i]) {
            	DiffofN1N2ofMedian.write(res3 + "\n");
            }
            DiffofN1N2ofMedian.close();
            //
            */
            progressListener.setProgress(i);
        }

        progressListener.setSubtask("computing medians");
        progressListener.setMaximum(-1);
        progressListener.setProgress(-1);

        float[] median = new float[length];
        for (int j = 0; j < length; j++) {
            float[] row = extractRow(diff, j);
            /*
            //check unsorted  median of Gene
            BufferedWriter UnsortedMedianofGene = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("UnsortedMedianofGene"+(j+1)+".txt", false)));
            for (int h = 0; h < row.length; h++) {
            	UnsortedMedianofGene.write(row[h]+"\n");
            }
            UnsortedMedianofGene.close();
            //
            */

            /*
            //check sorted median of Gene
            int[] tmp = new int[row.length];

            System.arraycopy(row, 0, tmp, 0, row.length);

            Arrays.sort(tmp);
            BufferedWriter SortedMedianofGene = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("SortedMedianofGene"+(j+1)+".txt", false)));
            for (int h = 0; h < tmp.length; h++) {
                SortedMedianofGene.write(tmp[h]+"\n");
            }
            SortedMedianofGene.write("median is "+tmp[(tmp.length-1) / 2]);
            SortedMedianofGene.close();
            //
            */
            median[j] = getMedian(row);
            progressListener.setProgress(j);

        }
        return median;

    }
    /*
     * end of Part A
     */

    /**
     * compute the boundaries for the percentile interval
     *
     * @param diff_i
     * @param leftValue
     * @param rightValue
     * @return leftlimit, rightlimit, middle value
     */
    private float[] getPercentileInterval(float[] diff_i, double leftValue, double rightValue) {

        float[] sorted = new float[diff_i.length];
        System.arraycopy(diff_i, 0, sorted, 0, diff_i.length);
        Arrays.sort(sorted);

        assert (diff_i.length * leftValue / 100.0 > 0);
        assert (diff_i.length * rightValue / 100.0 <= 100);

        int leftPer = (int) (diff_i.length * leftValue / 100.0);
        if (leftPer < 1)
            leftPer = 1;
        //wenn repeatstime is too small, rightPer can be 0 or -1
        int rightPer = (int) (diff_i.length * rightValue / 100.0) - 1;
        if (rightPer < 1)
            rightPer = 1;

        assert leftPer > 0 && leftPer <= diff_i.length && rightPer <= diff_i.length && leftPer <= rightPer;

        /*
         * result[0]:leftlimit; result[1]:rightlimit; result[2]:middelvalue
         */
        float[] result = new float[3];

        result[0] = sorted[leftPer - 1];
        result[1] = sorted[rightPer - 1];

        result[2] = sorted[(diff_i.length - 1) / 2];

        return result;
    }

    /**
     * here we compare a sample with it self and compute the left-value and right-value percentile limits
     *
     * @param sample
     * @param resamplingSize
     * @param repeats
     * @param leftValue
     * @param rightValue
     * @return left and right value for each class
     */
    private float[][] computePercentileLimits(float[] sample, int resamplingSize, int repeats, double leftValue, double rightValue) throws CanceledException {

        int length = sample.length;

        float[][] diff = new float[repeats][length];
        for (int i = 0; i < repeats; i++) {
            float[] N1 = getRandomSample(sample, resamplingSize);
            /*
            //check N1 for percentile
            BufferedWriter RandomsampleN1ofPer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("RandomsampleN1ofPer_rep"+(i+1)+".txt", false)));
            for (int res1 : N1) {
            	RandomsampleN1ofPer.write(res1 + "\n");
            }
            RandomsampleN1ofPer.close();
            //
            */
            float[] N2 = getRandomSample(sample, resamplingSize);
            /*
            //check N2 for percentile
            BufferedWriter RandomsampleN2ofPer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("RandomsampleN2ofPer_rep"+(i+1)+".txt", false)));
            for (int res2 : N2) {
            	RandomsampleN2ofPer.write(res2 + "\n");
            }
            RandomsampleN2ofPer.close();
            //
            */
            diff[i] = getAbsoluteDifference(N1, N2);
            /*
            //check difference of N1, N2 for percentile
            BufferedWriter DiffofN1N2ofPer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("DiffofN1N2ofPer_rep"+(i+1)+".txt", false)));
            for (int res3 : diff[i]) {
            	DiffofN1N2ofPer.write(res3 + "\n");
            }
            DiffofN1N2ofPer.close();
            //
            */
        }

        float[][] p5_95 = new float[length][3];
        for (int i = 0; i < length; i++) {
            float[] diff_i = extractRow(diff, i);
            /*
           //check unsorted  percentile of Gene
            BufferedWriter UnsortedPerofGene = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("UnsortedPerofGene"+(i+1)+".txt", false)));
            for (int h = 0; h < diff_i.length; h++) {
                UnsortedPerofGene.write(diff_i[h]+"\n");
            }
            UnsortedPerofGene.close();
            //
            */

            /*
            //check sorted percentile of Gene
            int[] sorted = new int[diff_i.length];
            System.arraycopy(diff_i, 0, sorted, 0, diff_i.length);
            Arrays.sort(sorted);

            assert(diff_i.length * leftValue / 100.0>0);
            assert(diff_i.length * rightValue / 100.0<=100);

            int leftPer = (int) (diff_i.length * leftValue / 100.0);
            if (leftPer < 1)
                leftPer = 1;
            int rightPer = (int) (diff_i.length * rightValue / 100.0) - 1;
            assert ((leftPer >0) && (leftPer <= diff_i.length) && (rightPer > 0) && (rightPer <= diff_i.length) && (leftPer <= rightPer));
            BufferedWriter SortedPerofGene = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("SortedPerofGene"+(i+1)+".txt", false)));
            for (int h = 0; h < sorted.length; h++) {
                SortedPerofGene.write(sorted[h]+ "\n");
            }
            SortedPerofGene.write("p5: "+sorted[leftPer]+"         p95: "+sorted[rightPer]+ "\n");
            SortedPerofGene.close();
            //
            */
            p5_95[i] = getPercentileInterval(diff_i, leftValue, rightValue);

        }

        return p5_95;
    }
    /*
     * end of PartB
     */

    /**
     * compare the median values with the p5_95 intervals
     *
     * @param median
     * @param p5_95
     * @return for each bucket, how its median value compares to the 9-95 interval
     */
    private Result[] compareMedianToPercentilslimits(float[] median, float[][] p5_95) throws CanceledException {
        assert (median.length == p5_95.length);

        int n = median.length;
        Result[] result = new Result[n];
        //initialize of result
        for (int j = 0; j < n; j++) {
            result[j] = new Result();
        }

        progressListener.setSubtask("Comparing medians to percentile limits");
        progressListener.setMaximum(n);
        progressListener.setProgress(0);


        for (int i = 0; i < n; i++) {
            float p05 = p5_95[i][0];
            float p95 = p5_95[i][1];
            float p50 = p5_95[i][2];
            float lengthOfInterval = p95 - p05;
            double scale;
            if (lengthOfInterval == 0) {
                scale = median[i] - p50;
                result[i].setGenNum(i + 1);
                result[i].setScale(scale);
                result[i].setRemark("Can't compare (zero in both samples)");
            }
            //rewritten on 23.08.07 replace int to double
            else {
                scale = (median[i] - (double) (p50)) / (double) (p95 - p05);
                if (optionWarningOnBoundary) {
                    if (Math.abs(scale) < 0.90) {
                        result[i].setGenNum(i + 1);
                        result[i].setScale(scale);
                        result[i].setRemark("Similar");
                    } else if (Math.abs(scale) > 1.10) {
                        result[i].setGenNum(i + 1);
                        result[i].setScale(scale);
                        result[i].setRemark("Not similar");
                    } else {
                        result[i].setGenNum(i + 1);
                        result[i].setScale(scale);
                        result[i].setRemark("Can't be classified");
                    }
                } else {
                    if (Math.abs(scale) < 1) {
                        result[i].setGenNum(i + 1);
                        result[i].setScale(scale);
                        result[i].setRemark("Similar");
                    } else {
                        result[i].setGenNum(i + 1);
                        result[i].setScale(scale);
                        result[i].setRemark("Not similar");
                    }
                }
            }
            progressListener.setProgress(i);
        }
        return result;
    }
    /*
     * end of PartC
     */

    /*
     * 20.08.2007 sort , operation direckt on the parameter "re"
     * (Note: solved on 23.08.07)one problem is that, the indexes with the same absolute values can not be ordert from positve to negative
     */

    static private void quickSortInScale(Result[] re, int left_index, int right_index) {
        if (left_index > right_index) return;

        if (right_index == (left_index + 1)) {
            if (Math.abs(re[left_index].getScale()) > Math.abs(re[right_index].getScale())) {
                Result tmp;
                tmp = re[right_index];
                re[right_index] = re[left_index];
                re[left_index] = tmp;
            } else if (Math.abs(re[left_index].getScale()) == Math.abs(re[right_index].getScale())) {
                if (re[left_index].getScale() < re[right_index].getScale()) {
                    Result tmp1;
                    tmp1 = re[right_index];
                    re[right_index] = re[left_index];
                    re[left_index] = tmp1;
                }
            }
        } else if (left_index == right_index) {
        } else {
            int pivot = left_index + ((right_index - left_index) / 2);
            Result[] copy = new Result[right_index - left_index + 1];
            //initialize of copy as container
            for (int j = 0; j <= (right_index - left_index); j++) {
                copy[j] = new Result();
            }
            int count = 0;
            for (int i = left_index; i <= right_index; i++) {
                //note: not include pivot self!
                if ((Math.abs(re[i].getScale()) < Math.abs(re[pivot].getScale())) && (i != pivot))
                    copy[count++] = re[i];
                    //the smaller signed values that have the same absolute value as pivot, go left
                else if ((Math.abs(re[i].getScale()) == Math.abs(re[pivot].getScale())) && (i != pivot)) {
                    if (re[i].getScale() < re[pivot].getScale()) copy[count++] = re[i];
                }
            }
            copy[count++] = re[pivot];
            int newIndexOfPivot = left_index + (count - 1); //save the new index of Pivotelement ; rewritten on 23.08.07
            for (int j = left_index; j <= right_index; j++) {
                if (Math.abs(re[j].getScale()) > Math.abs(re[pivot].getScale())) copy[count++] = re[j];
                    //the larger or same signed values that have the same absolute value as pivot, go right
                else if ((Math.abs(re[j].getScale()) == Math.abs(re[pivot].getScale())) && (j != pivot)) {
                    if (re[j].getScale() >= re[pivot].getScale()) copy[count++] = re[j];
                }
            }
            int count1 = 0;
            //rewrite the input array with copy
            for (int k = left_index; k <= right_index; k++) {
                re[k] = copy[count1++];
            }
            quickSortInScale(re, left_index, newIndexOfPivot - 1);
            quickSortInScale(re, newIndexOfPivot + 1, right_index);
        }
    }

    /**
     * sets parameters from the command line
     *
     * @throws Exception
     */
    private void setParametersFromCommandLine(String[] args) throws Exception {
        ArgsOptions options = new ArgsOptions(args, this, "SubsystemCompare - compare two samples and rank differences by significance");


        inputFileName1 = options.getOptionMandatory("-i1", "input1", "Input file 1", inputFileName1);
        inputFileName2 = options.getOptionMandatory("-i2", "input2", "Input file 2", inputFileName2);

        outputFileName = options.getOption("-o", "output", "output file", outputFileName);


        repeatitions = options.getOption("-r", "repeats", "Number of repeats", repeatitions);
        resamplingSize = options.getOption("-s", "size", "Sampling size", resamplingSize);

        p_left = options.getOption("-p", "left", "left limit of percentile interval", p_left);
        if (p_left <= 0 || p_left >= 100)
            throw new UsageException("-p " + p_left + " must be >0 and <100");
        p_right = 100 - p_left;

        useSecond = options.getOption("-po", "second", "Use the second input for percentiles", false);

        optionWarningOnBoundary = options.getOption("+wb", "noBoundary", "Don't make a call of values on the boundary", true);

        int seed = options.getOption("-seed", "randSeed", "use as seed for random number generate, if !=0", 0);
        if (seed != 0)
            random.setSeed(seed);
        options.done();
    }

    /**
     * sets parameters from the console
     *
     * @throws Exception
     */
    private void setParametersFromConsole() throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("please enter number of selecttimes(integer value): ");
        resamplingSize = Integer.parseInt(input.readLine());
        System.err.println("please enter number of repeattimes(integer value): ");
        repeatitions = Integer.parseInt(input.readLine());
        System.err.println("please enter the left limit of percentile interval(double value): ");
        p_left = Double.parseDouble(input.readLine());

        p_right = 100 - p_left;

        System.err.println("please enter the path of sample1: ");
        inputFileName1 = input.readLine();
        System.err.println("please enter the path of sample2: ");
        inputFileName2 = input.readLine();

        System.err.println("please choose one sample for getting Percentiles: ");
        System.err.println("1. " + inputFileName1);
        System.err.println("2. " + inputFileName2);
        System.err.println("choice(1/2):");

        int p = Integer.parseInt(input.readLine());
        useSecond = (p == 2);

        System.err.println("please enter a file name for output file data: ");
        outputFileName = input.readLine();
    }


    /*
     * Input
     * it has been realised, that *.txt data that contains only a split of int values would be read correctly.
     * in plan.... read *.txt that contain more splits
     */

    private float[] readInput(String fileName) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(new File(fileName)));
            String aLine;
            LinkedList<Float> input = new LinkedList<>();
            //r.readLine(); //ignore the 1. line
            while ((aLine = r.readLine()) != null) {
                if (aLine.length() > 0 && !aLine.startsWith("#"))
                    input.addLast(Float.valueOf(aLine));
            }
            float[] result = new float[input.size()];
            int count = 0;
            for (Float anInput : input) {
                result[count++] = anInput;
            }
            r.close();//written on 12.10.2007
            return result;
        } catch (IOException exp) {
            System.err.println(exp.getMessage());
            return null;
        }
    }
    /*
     * end of Input
     */

    /*
     * Output
     * it has been realised, the result array in screen to display
     * it has been realised,  output into a file
     */

    static public void print(Result[] result) {
        for (Result res : result) {
            System.err.println("Gen" + res.getGenNum() + ":   scale of " + res.getScale() + "  " + res.getRemark());
        }
    }

    /**
     * print in reverse order
     *
     * @param result
     */
    private static void reversePrint(Result[] result) {
        for (int i = (result.length - 1); i >= 0; i--) {
            System.err.println("Gen" + result[i].getGenNum() + ":   scale of " + result[i].getScale() + "  " + result[i].getRemark());
        }
    }

    /**
     * creat the output files
     *
     * @param result
     * @param fileDst
     * @param bAppend
     * @throws IOException
     */
    static public void createOutputFiles(Result[] result, String fileDst, boolean bAppend) throws IOException {

        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileDst, bAppend)));
        for (Result res : result) {
            output.write("Gen" + res.getGenNum() + ":  scale of " + res.getScale() + "  " + res.getRemark() + "\n");
        }
        output.close();
        System.err.println();
        System.err.println("Results saved to file: " + fileDst);
    }

    /**
     * create output files with data in reverse order
     *
     * @param result
     * @param fileDst
     * @param bAppend
     * @throws IOException
     */
    private static void createOutputFilesReverse(Result[] result, String fileDst, boolean bAppend) throws IOException {
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileDst, bAppend)));
        for (int i = (result.length - 1); i >= 0; i--) {
            output.write("Gen" + result[i].getGenNum() + ":  scale of " + result[i].getScale() + "  " + result[i].getRemark() + "\n");
        }
        output.close();
        System.err.println();
        System.err.println(fileDst + " is generated!");
    }

}
