/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.data;

import java.util.Set;

public interface IClassificationBlock {

    /**
     * get the number associated with a key
     *
     * @param key
     * @return number
     */
    int getSum(Integer key);

    /**
     * get the weighted number associated with a key
     *
     * @param key
     * @return number
     */
    int getWeightedSum(Integer key);


    /**
     * set the number associated with a key -> just set not write to disk
     *
     * @param key
     * @param num
     */
    void setSum(Integer key, int num);


    /**
     * get the name of this classification
     *
     * @return name
     */
    String getName();

    /**
     * set the name of this classification
     *
     * @param name
     */
    void setName(String name);

    /**
     * get human readable representation
     *
     * @return human readable
     */
    String toString();

    Set<Integer> getKeySet();
}
