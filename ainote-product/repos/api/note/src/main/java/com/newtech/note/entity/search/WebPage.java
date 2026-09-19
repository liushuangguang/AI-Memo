package com.newtech.note.entity.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.newtech.note.entity.dto.noteRelatedInfo.RelatedLink;
import lombok.Data;

/*
{
    "cachedPageUrl": "",
    "dateLastCrawled": "0001-01-01T00:00:00Z",
    "datePublished": "",
    "datePublishedDisplayText": "",
    "displayUrl": "",
    "id": "",
    "isFamilyFriendly": false,
    "isNavigational": false,
    "language": "",
    "name": "儿童手表(儿童智能穿戴设备)-百科",
    "primaryImageOfPage": {
        "height": 0,
        "imageId": "",
        "thumbnailUrl": "",
        "width": 0
    },
    "snippet": "简介：儿童安全问题在全球都是一个非常热门的话题，每年针对儿童的安全问题国家都会制定一些相应举措，但仍旧不能够避免儿童走丢等问题的发生。伴随着可穿戴设备的兴起，儿童智能手表也在市场大潮中崛起。而且对于大部分的家长来说购买电话手表的初衷是为了保证幼儿的安全。但是，儿童电话手表产业刚刚起步，所面临的安全威胁也日趋凸显，针对儿童电话手表的恶意攻击种类不断更新，儿童隐私信息泄露、中间人攻击、恶意远程操控等事件反复曝光，儿童电话手表的信息安全问题已成为制约产业发展的重要因素之一。目前国内儿童电话手表市场发展快速，产品类别繁多，安全防护水平参差不齐。\\r 2017年11月，德国联邦网络局宣布禁止销售儿童智能手表。2022年3月15日，央视3·15晚会曝光低配版儿童智能手表泄露隐私。",
    "thumbnailUrl": "",
    "url": "https://m.baike.com/wiki/%E5%84%BF%E7%AB%A5%E6%89%8B%E8%A1%A8/10256642"
}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebPage {
    private String name;
    private String snippet;
    private String thumbnailUrl;
    private String url;
    private PrimaryImageOfPage primaryImageOfPage;

    public RelatedLink transferToBo() {
        RelatedLink relatedLink = new RelatedLink();
        relatedLink.setLink(this.getUrl());
        relatedLink.setLinkName(this.getName());
        relatedLink.setSnippet(this.getSnippet());
        return relatedLink;
    }
}
