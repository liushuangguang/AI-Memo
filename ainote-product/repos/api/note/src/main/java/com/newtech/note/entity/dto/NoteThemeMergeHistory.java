package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteThemeMergeHistory {
    private String operationId;
    private String mergedNoteId;
    private String mergedTitle;
    private String mergedContentPreview;
    private List<String> imageUrls;
    private List<NoteThemeSourceRef> sources;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime mergedAt;
}
