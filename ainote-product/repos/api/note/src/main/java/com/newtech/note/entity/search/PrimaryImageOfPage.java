package com.newtech.note.entity.search;

import lombok.Data;

/*
"primaryImageOfPage": {
        "height": 0,
        "imageId": "",
        "thumbnailUrl": "",
        "width": 0
    }
 */
@Data
public class PrimaryImageOfPage {
    private Integer height;
    private String imageId;
    private String thumbnailUrl;
    private Integer width;
}
