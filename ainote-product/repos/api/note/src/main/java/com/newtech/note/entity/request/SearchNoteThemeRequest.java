package com.newtech.note.entity.request;

import com.newtech.note.entity.filter.NoteThemeFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "搜索笔记请求")
public class SearchNoteThemeRequest {
    @Schema(description = "搜索关键字")
    private String keyword;
    private Integer page = 1;
    @Schema(description = "每页大小", defaultValue = "10")
    private Integer size = 10;
    @Schema(description = "是否升序排列", defaultValue = "false")
    private boolean ascending;
    @Schema(description = "排序字段", defaultValue = "id", allowableValues = {"id", "updatedAt"})
    private String sortBy = "id";

    public NoteThemeFilter buildFilter(String deviceId) {
        NoteThemeFilter filter = new NoteThemeFilter();
        filter.setDeviceId(deviceId);
        if (StringUtils.isNotBlank(keyword)) {
            filter.setKeyword(keyword);
        }
        return filter;
    }
}
