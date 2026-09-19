package com.newtech.note.client.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
public class PddGoodsPromotionUrlGenerateResponse {
    private List<GoodsPromotionUrl> goods_promotion_url_list;

    @Getter
    @NoArgsConstructor
    @Setter
    public static class GoodsPromotionUrl {
        private String mobile_short_url;
        private String mobile_url;
        private QQAppInfo qq_app_info;
        private String schema_url;
        private String short_url;
        private String tz_schema_url;
        private String url;
        private WeAppInfo we_app_info;
        private String weixin_code;
        private String weixin_short_link;

    }

    @Getter
    @NoArgsConstructor
    @Setter
    static class QQAppInfo {
        private String app_id;
        private String banner_url;
        private String desc;
        private String page_path;
        private String qq_app_icon_url;
        private String source_display_name;
        private String title;
        private String user_name;
    }

    @Getter
    @NoArgsConstructor
    @Setter
    static class WeAppInfo {
        private String app_id;
        private String banner_url;
        private String desc;
        private String page_path;
        private String source_display_name;
        private String title;
        private String user_name;
        private String we_app_icon_url;
    }

}





