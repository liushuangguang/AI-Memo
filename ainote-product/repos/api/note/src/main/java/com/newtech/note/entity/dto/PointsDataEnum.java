package com.newtech.note.entity.dto;

import lombok.Getter;

@Getter
public enum PointsDataEnum {
    MONTH(30),
    YEAR(365);

    private final int days;

    PointsDataEnum(int days) {
        this.days = days;
    }
}
