package com.newtech.note.entity.request;

import com.newtech.note.entity.filter.NoteAnalysisFilter;
import com.newtech.note.entity.filter.NoteFilterV2;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "搜索笔记请求")
public class SearchNoteRequest {
    @Schema(description = "搜索关键字")
    private String keyword;
    @Schema(description = "note类型", nullable = true)
    private Integer noteType;
    @Schema(description = "当前页码", defaultValue = "1")
    private Integer page = 1;
    @Schema(description = "每页大小", defaultValue = "10")
    private Integer size = 10;
    @Schema(description = "是否升序排列", defaultValue = "false")
    private boolean ascending;
    @Schema(description = "排序字段", defaultValue = "id", allowableValues = {"id", "updatedAt"})
    private String sortBy = "id";
    @Schema(description = "维度， 0：用户维度，1：系统维度", nullable = true)
    private Integer dimension;

    public NoteAnalysisFilter buildFilter(String deviceId) {
        NoteAnalysisFilter filter = new NoteAnalysisFilter();
        filter.setNoteType(noteType);
        filter.setDeviceId(deviceId);
        filter.setDimension(dimension);
        if (StringUtils.isNotBlank(keyword)) {
            filter.setKeyword(keyword);
        }
        return filter;
    }

    public NoteFilterV2 buildFilterV2(String deviceId) {
        NoteFilterV2 filter = new NoteFilterV2();
        filter.setNoteType(noteType);
        filter.setDeviceId(deviceId);
        filter.setDimension(dimension);
        if (StringUtils.isNotBlank(keyword)) {
            filter.setKeyword(keyword);
        }
        return filter;
    }
}
