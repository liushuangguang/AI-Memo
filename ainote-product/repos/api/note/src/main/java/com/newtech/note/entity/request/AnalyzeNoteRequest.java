package com.newtech.note.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "版本号传入为创建note_analysis表的版本号，后端判断这个版本号下的数据是否存在，" +
                "如果不存在调用相应的agent进行数据的采集，并将采集到的数据存入note_analysis_history表中" +
                "如果存在则直接从note_analysis_history表中获取数据并返回" +
                "注意：当客户端获取不到版本号时，版本号传入为1")

public class AnalyzeNoteRequest {
        @Schema(description = "note_analysis表的id")
        private long id;
        @Schema(description = "版本号")
        private int version;
        /*
         * @Schema(description = "是否从本地数据库中获取")
         * private boolean fromDB;
         */
}
