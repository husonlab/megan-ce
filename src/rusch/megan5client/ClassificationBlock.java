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
package rusch.megan5client;

import megan.data.IClassificationBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * @author Hans-Joachim Ruscheweyh
 * 10:20:17 AM - Oct 29, 2014
 */
public class ClassificationBlock implements IClassificationBlock {
    private final Map<Integer, Integer> taxId2Count = new HashMap<>();
    private String classification;


    public ClassificationBlock(ClassificationBlockServer classificationBlockServer) {
        for (Entry<Object, Integer> e : classificationBlockServer.getTaxId2Count().entrySet()) {
            String string = (String) e.getKey();
            Integer key = Integer.valueOf(string);
            Integer value = e.getValue();
            taxId2Count.put(key, value);
        }
        this.classification = classificationBlockServer.getClassification();
    }

    @Override
    public int getSum(Integer key) {
        Integer sum = taxId2Count.get(key);
        return sum != null ? sum : 0;
    }

    @Override
    public float getWeightedSum(Integer key) {
        return taxId2Count.get(key);
    }

    @Override
    public void setSum(Integer key, int num) {
        taxId2Count.put(key, num);
    }

    @Override
    public void setWeightedSum(Integer key, float num) {
        setSum(key, (int) num);
    }

    @Override
    public String getName() {
        return classification;
    }

    @Override
    public void setName(String name) {
        this.classification = name;

    }

    @Override
    public Set<Integer> getKeySet() {
        return taxId2Count.keySet();
    }

}
