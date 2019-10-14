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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Hans-Joachim Ruscheweyh
 * 3:02:58 PM - Oct 27, 2014
 * <p/>
 * Just an adapter for the MEGAN {@link IClassificationBlock} with getters and setters.
 */
public class ClassificationBlockServer implements Serializable {


    private Map<Object, Integer> taxId2Count = new HashMap<>();
    private String classification;

    public ClassificationBlockServer() {

    }

    public ClassificationBlockServer(IClassificationBlock block) {
        load(block);
    }


    public Map<Object, Integer> getTaxId2Count() {
        return taxId2Count;
    }


    public void setTaxId2Count(Map<Object, Integer> taxId2Count) {
        this.taxId2Count = taxId2Count;
    }


    public String getClassification() {
        return classification;
    }


    public void setClassification(String classification) {
        this.classification = classification;
    }


    private void load(IClassificationBlock block) {
        classification = block.getName();
        for (Integer i : block.getKeySet()) {
            taxId2Count.put(i, block.getSum(i));
        }
    }


}
