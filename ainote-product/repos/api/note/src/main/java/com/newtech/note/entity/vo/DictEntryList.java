package com.newtech.note.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DictEntryList {
    @Schema(description = "字典类型名称, 例如: noteType")
    private String typeName;

    @Schema(description = "字典项列表, 例如: [Pair(key=1, value=灵感记录), Pair(key=2, value=待办事项)]")
    private List<Pair<String, String>> entryList;
}
