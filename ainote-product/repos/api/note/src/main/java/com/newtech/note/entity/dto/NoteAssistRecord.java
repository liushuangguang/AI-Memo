package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.newtech.note.entity.bo.RewrittenContentBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Document("note_assist_records")
@Schema(description = "笔记辅助记录")
public class NoteAssistRecord {
    @Id
    private String id;
    @Schema(description = "备忘录的id")
    private String noteId;
    @Schema(description = "原始笔记，指ai分析之前的笔记")
    private String rawNote;
    @Schema(description = "版本号")
    private int version;
    @Schema(description = "总体审查，对备忘录是否有正确的语义进行审核评价", nullable = true)
    private String overallReview;

    @Schema(description = "重写后的内容")
    private RewrittenContentBo rewrittenContent;

    @Schema(description = "辅助的方向")
    private List<String> assistContent;

    @Schema(description = "选中的内容，局部需要进行辅助的部分")
    private String selectedContent;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
