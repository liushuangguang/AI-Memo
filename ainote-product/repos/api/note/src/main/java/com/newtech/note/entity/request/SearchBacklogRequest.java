package com.newtech.note.entity.request;

import com.newtech.note.entity.filter.BacklogFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "搜索待办事项请求")
public class SearchBacklogRequest {
    @Schema(description = "搜索关键字")
    private String keyword;
    @Schema(description = "是否包含删除线的待办事项, false表示不包含, true表示包含", allowableValues = {"true", "false"}, defaultValue = "false")
    private boolean includeStrikethrough;
    @Schema(description = "当前页码", defaultValue = "1")
    private Integer page = 1;
    @Schema(description = "每页大小", defaultValue = "10")
    private Integer size = 10;
    @Schema(description = "是否升序排列", defaultValue = "false")
    private boolean ascending;
    @Schema(description = "排序字段", defaultValue = "id", allowableValues = {"id", "updatedAt"})
    private String sortBy = "id";

    public BacklogFilter buildFilter(String deviceId) {
        BacklogFilter filter = new BacklogFilter();
        filter.setDeviceId(deviceId);
        if (StringUtils.isNotBlank(keyword)) {
            filter.setKeyword(keyword);
        }
        filter.setIncludeStrikethrough(includeStrikethrough);
        return filter;
    }
}
