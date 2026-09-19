package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "搜索笔记请求")
public class SearchDictRequest {
    @Schema(description = "搜索的类型名称，一般为枚举值，如：noteType")
    private List<String> typeNames;
}
