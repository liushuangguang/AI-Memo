package com.newtech.note.common;

/**
 * 用户等级enum
 */
public enum UserLevelEnum {

    /**
     * 免费
     */
    HUMAN(100,30),

    /**
     * 中等
     */
    MID(1000,30),

    /**
     * 高级
     */
    HIGH(10000,365);

    private final double point;

    private final int days;

    public double getPoint() {
        return point;
    }

    public int getDays() {
        return days;
    }

    UserLevelEnum(int point, int days) {
        this.point = point;
        this.days = days;
    }
}
