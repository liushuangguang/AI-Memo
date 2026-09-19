package com.newtech.note.entity.dto.noteModules;

import com.newtech.note.common.enumeration.NoteModuleType;
import com.newtech.note.entity.dto.Note;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.springframework.data.annotation.Transient;

import java.util.List;

@Getter
@Setter
@BsonDiscriminator(value = "NOTE_MODULE.RELATED_NOTE")
@Schema(description = "相关笔记模块")
public class RelatedNoteModule implements NoteModule {
    @Schema(description = "模块ID")
    private String moduleId;

    @Schema(description = "模块类型")
    private NoteModuleType noteModuleType = NoteModuleType.RELATED_NOTE;

    @Schema(description = "相关笔记ID")
    private List<String> relatedNoteIds;

    @Transient
    @Schema(description = "相关笔记")
    private List<Note> relatedNotes;

}
