/*
 * DESeq.java Copyright (C) 2022 Daniel H. Huson
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

package megan.stats;

import jloda.util.BitSetUtils;

import java.util.*;

/**
 * implementation of the DESeq method described in https://www.nature.com/articles/npre.2010.4282.1.pdf?origin=ppub
 * Daniel Huson, 7.2021
 */
public class DESeq {
	/**
	 * applies the DESeq analysis method
	 *
	 * @param class2counts for each class (either taxon or gene), the counts for all samples 1...m
	 * @param condition1   which samples 1..m belong to first condition?
	 * @param condition2   which samples 1..m belong to second condition?
	 * @param pThreshold   threshold for p value
	 * @return result
	 */
	public static ArrayList<Row> apply(Map<String, Double[]> class2counts, BitSet condition1, BitSet condition2, double pThreshold) {
		if (condition1.cardinality() == 0 || condition2.cardinality() == 0 || BitSetUtils.intersection(condition1, condition2).cardinality() > 0)
			throw new IllegalArgumentException("Invalid conditions");
		var samples = BitSetUtils.union(condition1, condition2);

		var sizes = estimateSizes(class2counts, samples);

		var counts1 = estimateCounts(class2counts, sizes, condition1);

		var counts2 = estimateCounts(class2counts, sizes, condition2);


		return null;
	}


	private static double[] estimateSizes(Map<String, Double[]> class2counts, BitSet samples) {
		var m = samples.cardinality();
		var values = (ArrayList<Double>[]) new ArrayList[BitSetUtils.max(samples)];

		for (var counts : class2counts.values()) {
			var denominator = 1.0;
			for (int s : BitSetUtils.members(samples)) {
				denominator *= counts[s];
			}
			denominator = Math.pow(denominator, 1.0 / m);
			for (int s : BitSetUtils.members(samples)) {
				values[s].add(counts[s] / denominator);
			}
		}
		var result = new double[BitSetUtils.max(samples)];
		for (int s : BitSetUtils.members(samples)) {
			var list = values[s];
			list.sort(Double::compare);
			result[s] = list.get(list.size() / 2);
		}
		return result;
	}

	private static Map<String, Double> estimateCounts(Map<String, Double[]> class2counts, double[] sizes, BitSet condition) {
		var map = new HashMap<String, Double>();

		for (var entry : class2counts.entrySet()) {
			var name = entry.getKey();
			var counts = entry.getValue();

			var sum = 0.0;
			for (var s : BitSetUtils.members(condition)) {
				sum += counts[s] / sizes[s];
			}
			map.put(name, sum / condition.cardinality());
		}
		return map;
	}


	public static class Row {
		private final String name;
		private final double foldValue;
		private final double pValue;

		public Row(String name, double foldValue, double pValue) {
			this.name = name;
			this.foldValue = foldValue;
			this.pValue = pValue;
		}

		public String getName() {
			return name;
		}

		public double getFoldValue() {
			return foldValue;
		}

		public double getpValue() {
			return pValue;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Row)) return false;
			Row row = (Row) o;
			return Double.compare(row.foldValue, foldValue) == 0 && Double.compare(row.pValue, pValue) == 0 && name.equals(row.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, foldValue, pValue);
		}
	}
}
