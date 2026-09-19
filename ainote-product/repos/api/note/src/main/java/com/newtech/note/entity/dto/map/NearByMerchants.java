package com.newtech.note.entity.dto.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/*
{
    "output": {
        "count": 5,
        "poi_list": [
            {
                "address": "南池子大街与缎库胡同交叉口西20米",
                "city_name": "北京市",
                "district_name": "东城区",
                "location": "116.403050,39.910887",
                "name": "心连心牛肉面馆",
                "photos": null,
                "province_name": "北京市",
                "type": "餐饮服务;中餐厅;中餐厅"
            },
            {
                "address": "南长街甲30号",
                "city_name": "北京市",
                "district_name": "西城区",
                "location": "116.391723,39.912744",
                "name": "老北京炸酱面(南长街店)",
                "photos": [
                    "https://aos-comment.amap.com/B000AA0IME/comment/20DAE25B_4A35_4B6A_AA7D_5DDF8F97B29F_L0_001_1080_1920_1729582281768_83293692.jpg",
                    "https://aos-comment.amap.com/B000AA0IME/comment/21F60F90_6D69_4E24_9A11_5959CB8F4A7C_L0_001_1080_1920_1729582281769_70348936.jpg",
                    "https://aos-comment.amap.com/B000AA0IME/comment/0feedback_sendimag_1729582340819_02489623.jpg"
                ],
                "province_name": "北京市",
                "type": "餐饮服务;中餐厅;特色/地方风味餐厅"
            },
            {
                "address": "南池子大街33号",
                "city_name": "北京市",
                "district_name": "东城区",
                "location": "116.402939,39.914028",
                "name": "老北京炸酱面",
                "photos": [
                    "http://s-pic.oss-cn-beijing.aliyuncs.com/desensitize/deep/images/publish/f433e5994624923e0f31853bddfb8ecb.jpg",
                    "http://aos-cdn-image.amap.com/sns/ugccomment/d4f8cbca-a180-4a28-b375-d49cd0b2e5ee.jpg",
                    "http://store.is.autonavi.com/showpic/0c6edb6ef60358c3b7427be0e5f6e5bd"
                ],
                "province_name": "北京市",
                "type": "餐饮服务;中餐厅;中餐厅"
            },
            {
                "address": "南河沿大街甲41-3号",
                "city_name": "北京市",
                "district_name": "东城区",
                "location": "116.406637,39.911524",
                "name": "41号炸酱面(南河沿店)",
                "photos": [
                    "https://aos-comment.amap.com/B0FFHRJ0RW/comment/85e7088e9e1a8e8aeea9843ae18e58a4_2048_2048_80.jpg",
                    "https://aos-comment.amap.com/B0FFHRJ0RW/comment/347ec4391329fa6ae0418ef0242c706d_2048_2048_80.jpg",
                    "http://store.is.autonavi.com/showpic/c7c7f272de1e0aded3996bd58d396157"
                ],
                "province_name": "北京市",
                "type": "餐饮服务;中餐厅;中餐厅"
            },
            {
                "address": "普渡寺西巷与普渡寺后巷交叉口西北40米",
                "city_name": "北京市",
                "district_name": "东城区",
                "location": "116.403775,39.914825",
                "name": "啊美丽烤肉冷面",
                "photos": null,
                "province_name": "北京市",
                "type": "餐饮服务;中餐厅;中餐厅"
            }
        ]
    }
}
 */
@Data
public class NearByMerchants {
    private Integer count;
    @JsonProperty("poi_list")
    private List<Poi> poiList;
}
