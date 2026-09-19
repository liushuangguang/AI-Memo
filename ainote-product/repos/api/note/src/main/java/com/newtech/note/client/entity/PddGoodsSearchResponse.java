package com.newtech.note.client.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PddGoodsSearchResponse {
    private List<Goods> goods_list;
    private String list_id;
    private String search_id;
    private int total_count;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Goods {
        private long activity_promotion_rate;
        private List<Integer> activity_tags;
        private int activity_type;
        private String brand_name;
        private long cash_gift_amount;
        private List<Long> cat_ids;
        private String clt_cpn_batch_sn;
        private long clt_cpn_discount;
        private long clt_cpn_end_time;
        private long clt_cpn_min_amt;
        private long clt_cpn_quantity;
        private long clt_cpn_remain_quantity;
        private long clt_cpn_start_time;
        private long coupon_discount;
        private long coupon_end_time;
        private long coupon_min_order_amount;
        private long coupon_remain_quantity;
        private long coupon_start_time;
        private long coupon_total_quantity;
        private long create_at;
        private String desc_txt;
        private long extra_coupon_amount;
        private String goods_desc;
        private String goods_image_url;
        private List<Integer> goods_labels;
        private String goods_name;
        private String goods_sign;
        private String goods_thumbnail_url;
        private boolean has_coupon;
        private boolean has_mall_coupon;
        private boolean has_material;
        private String lgst_txt;
        private int mall_coupon_discount_pct;
        private long mall_coupon_end_time;
        private long mall_coupon_id;
        private int mall_coupon_max_discount_amount;
        private long mall_coupon_min_order_amount;
        private long mall_coupon_remain_quantity;
        private long mall_coupon_start_time;
        private long mall_coupon_total_quantity;
        private int mall_cps;
        private long mall_id;
        private String mall_name;
        private int merchant_type;
        private long min_group_price;
        private long min_normal_price;
        private boolean only_scene_auth;
        private long opt_id;
        private List<Long> opt_ids;
        private String opt_name;
        private int plan_type;
        private long predict_promotion_rate;
        private long promotion_rate;
        private String sales_tip;
        private String search_id;
        private String serv_txt;
        private List<Long> service_tags;
        private int share_rate;
        private long subsidy_amount;
        private long subsidy_duo_amount_ten_million;
        private int subsidy_goods_type;
        private List<String> unified_tags;
        private long zs_duo_id;

    }
}