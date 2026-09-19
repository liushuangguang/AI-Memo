package com.newtech.note.entity.dto.noteModules.items;

import com.newtech.note.common.enumeration.NoteItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.io.Serializable;

@Schema(description = "Note模块元素")
@BsonDiscriminator
public interface NoteModuleItem extends Serializable {
    String getItemId();

    NoteItemType getNoteItemType();

    default String getSegmentation() {
        return null;
    }
}
