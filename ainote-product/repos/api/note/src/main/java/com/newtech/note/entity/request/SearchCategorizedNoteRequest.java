package com.newtech.note.entity.request;

import com.newtech.note.entity.filter.CategorizedNoteFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索分类笔记请求")
public class SearchCategorizedNoteRequest {

    @Schema(description = "note分类类型", allowableValues = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"})
    private Integer noteType;

    @Schema(description = "note分析id, note_analysis表的主键")
    private Long noteAnalysisId;

    public CategorizedNoteFilter buildFilter(String deviceId) {
        CategorizedNoteFilter filter =  new CategorizedNoteFilter();
        filter.setNoteType(noteType);
        filter.setNoteAnalysisId(noteAnalysisId);
        filter.setDeviceId(deviceId);
        return filter;
    }


}
