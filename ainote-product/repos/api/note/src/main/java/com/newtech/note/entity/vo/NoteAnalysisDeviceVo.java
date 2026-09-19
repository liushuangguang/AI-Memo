package com.newtech.note.entity.vo;

import com.newtech.note.entity.dto.NoteDevice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteAnalysisDeviceVo {
    private Long id;

    private String title;

    private String deviceId;

    private String noteId;

    private String rawNote;

    private String noteAnalysisContent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private NoteDevice noteDevice;
}
