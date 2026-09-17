package com.zhixiaojiang.model.vo;

/** 查询投影：DimensionScore（字段名即 JSON 键）。 */
public class DimensionScore {
    private String dimension;
    private java.math.BigDecimal score;

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public java.math.BigDecimal getScore() { return score; }
    public void setScore(java.math.BigDecimal score) { this.score = score; }
}
