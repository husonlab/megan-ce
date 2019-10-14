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
