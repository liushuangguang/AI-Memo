package com.newtech.note.entity.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/*
{
                "goods_desc": "小米米兔儿童电话手表C7A学生4G智能GPS定位视频通话拍照",
                "goods_name": "小米米兔儿童电话手表C7A学生4G智能GPS定位视频通话拍照",
                "goods_price": "264.0 元",
                "goods_thumbnail_url": "https://img.pddpic.com/gaudit-image/2024-10-22/11cdacb09fad870cfcd528a90e9e21f2.jpeg",
                "url": "https://p.pinduoduo.com/SCUd2LTP"
            }
 */

@Data
public class Product {
    @JsonProperty("goods_desc")
    private String goodsDesc;
    @JsonProperty("goods_name")
    private String goodsName;
    @JsonProperty("goods_price")
    private String goodsPrice;
    @JsonProperty("goods_thumbnail_url")
    private String goodsThumbnailUrl;
    private String url;
    private String sourceType;
}
