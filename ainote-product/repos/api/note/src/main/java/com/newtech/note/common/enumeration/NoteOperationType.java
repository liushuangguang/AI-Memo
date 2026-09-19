package com.newtech.note.common.enumeration;

import lombok.Getter;

@Getter
public enum NoteOperationType {
    REPLACE_TEXT_MODULE("取代文本模块，替换原有文本，目前用于一键替换功能，使用AI整理后的文本替换用户编辑的文本");


    private final String desc;

    NoteOperationType(String desc) {
        this.desc = desc;
    }


}
