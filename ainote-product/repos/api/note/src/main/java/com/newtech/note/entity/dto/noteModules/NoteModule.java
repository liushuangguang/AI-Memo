package com.newtech.note.entity.dto.noteModules;

import com.newtech.note.common.enumeration.NoteModuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.io.Serializable;

@BsonDiscriminator
@Schema(description = "Note模块接口")
public interface NoteModule extends Serializable {
    String getModuleId();

    NoteModuleType getNoteModuleType();

    default String getSegmentation() {
        return null;
    }
}
