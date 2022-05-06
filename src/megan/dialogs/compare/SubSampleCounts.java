package megan.dialogs.compare;

import jloda.util.*;
import jloda.util.progress.ProgressListener;

import java.util.*;

/**
 * subsample counts
 * Daniel Huson, 11.2011, 5.2022
 */
public class SubSampleCounts {
    /**
     * subsample from a map of counts
     *
     * @param sampleSize
     * @param runs
     * @param ignoreUnassigned
     * @param class2count
     * @param progress
     * @return
     * @throws CanceledException
     */
    public static Map<Integer, Integer> apply(long sampleSize, int runs, boolean ignoreUnassigned, Map<Integer, Integer[]> class2count, final ProgressListener progress) throws CanceledException {
        // setup lookup data-structure:
        final var runningTotal2Class = new TreeMap<Long, Integer>();
        var total = 0L;
        for (var entry : class2count.entrySet()) {
            if (!ignoreUnassigned || entry.getKey() > 0) {
                total += CollectionUtils.getSum(entry.getValue());
                runningTotal2Class.put(total, entry.getKey());
            }
        }
        final var numberOfReads = (int) Math.min(Integer.MAX_VALUE, total);

        final var result = new HashMap<Integer, Integer>();
        final var rand = new Random(666);

        progress.setProgress(0);
        progress.setMaximum(runs);

        for(var run=0;run<runs;run++) {
        for (var i = 1; i <= sampleSize ; i++) {
                    var which = rand.nextInt(numberOfReads);
                    var key = runningTotal2Class.tailMap((long) which).firstKey();
                    var classId = runningTotal2Class.get(key);
                    result.merge(classId, 1, Integer::sum);
            }
        progress.incrementProgress();
        }

          var totalReadsSampled = normalize(result, 1.0 / runs);
        if (totalReadsSampled != sampleSize && totalReadsSampled > 0)
            totalReadsSampled = normalize(result, (double) sampleSize / (double) totalReadsSampled);
        System.err.println("Number of reads sampled: " + totalReadsSampled);

        return result;
    }

    /**
     * multiple all counts by the given number and then round to integer
     *
     * @param class2count
     * @param factor
     * @return total reads after normalization
     */
    private static long normalize(Map<Integer, Integer> class2count, double factor) {
        var total = 0L;
        for (var classId : class2count.keySet()) {
            var count = class2count.get(classId);
            if (count != null) {
                var newValue = (int) Math.round(factor * count);
                class2count.put(classId, newValue);
                total += newValue;
            }
        }
        return total;
    }
}
