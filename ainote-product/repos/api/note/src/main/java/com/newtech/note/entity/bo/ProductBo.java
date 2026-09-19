package com.newtech.note.entity.bo;

@lombok.Data
public class ProductBo {
    private String productName;
    private String productDesc;
    private String productImageUrl;
    private String productShortUrl;
    private String minNormalPrice;
    private String recommendationReason;
    private String sourceType;
    private boolean constraintChecked;
    private int constraintVersion;
}
