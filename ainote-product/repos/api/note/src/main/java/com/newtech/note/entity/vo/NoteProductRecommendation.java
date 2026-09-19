package com.newtech.note.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "note商品推荐信息")
public class NoteProductRecommendation {

    /*@Schema(description = "note_analysis表的id")
    private long noteId;*/

    @Schema(description = "商品所属的平台")
    private int productPlatform;

    @Schema(description = "商品Id")
    private String productId;

    @Schema(description = "商品搜索名称，可以通过这个名称去平台如多多客查询正在售卖的具体商品")
    private String productSearchName;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品描述")
    private String productDesc;

    @Schema(description = "商品图片链接")
    private String productImageUrl;

    @Schema(description = "普通短链。微信环境下进入领券页点领券拉起小程序，浏览器环境下优先拉起微信小程序")
    private String productShortUrl;

    @Schema(description = "使用此推广链接，用户安装拼多多APP的情况下会唤起APP（需客户端支持schema跳转协议）")
    private String productSchemaUrl;

    @Schema(description = "推荐理由")
    @Transient
    private String recommendationReason;

    @Schema(description = "最小拼团价（单位为元，精确到分）")
    private String minGroupPrice;

    @Schema(description = "最小单买价格（单位为元，精确到分）")
    private String minNormalPrice;
}
