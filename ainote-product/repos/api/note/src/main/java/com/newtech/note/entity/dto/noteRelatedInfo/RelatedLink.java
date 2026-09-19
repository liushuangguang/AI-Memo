package com.newtech.note.entity.dto.noteRelatedInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "相关链接")
public class RelatedLink {
    /**
     * 链接地址, 如 https://www.baidu.com/link?url=fu3qi6fyByCiZSPWVAsLoaT8076lbDrpn_7oK9sqkiQMK2tm153zwSSzgv4TPHMcRnJty9o95gf7kTuB56SOpTajLZN-xpvYcspN7wdlQrypYI9mn4lNe2yA_lDhXs01&wd=&eqid=f231b27700e6efb4000000056735a08c
     */
    @Schema(description = "链接地址，注意favicon可以从该链接获取，如 https://www.baidu.com/favicon.ico，https://www.taobao.com/favicon.ico")
    private String link;
    /**
     * 链接名字
     */
    @Schema(description = "链接地址")
    private String linkName;
    /**
     * 小段简介
     */
    @Schema(description = "小段简介")
    private String snippet;
}
