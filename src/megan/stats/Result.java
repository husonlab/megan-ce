/*
 * Result.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */

package megan.stats;

import java.util.Comparator;

class Result {

    private int gennum;
    private double scale;
    private String remark;

    //constructor

    public Result() {
        this.gennum = 0;
        this.scale = 0;
        this.remark = "Not tested";
    }

    public int getGenNum() {
        return gennum;
    }

    public void setGenNum(int num) {
        gennum = num;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double f) {
        scale = f;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String s) {
        remark = s;
    }

    static public Comparator<Result> getScaleComparator() {
        return (r1, r2) -> {

            if (r1.scale < r2.scale)
                return -1;
            else if (r1.scale > r2.scale)
                return 1;
            else if (r1.gennum < r2.gennum)
                return -1;
            else if (r1.gennum > r2.gennum)
                return 1;
            else
                return 0;
        };
    }

}
