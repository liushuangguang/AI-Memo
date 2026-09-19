package com.newtech.note.common.enumeration;

import lombok.Getter;

@Getter
public enum NoteItemType {
    BACKLOG("backlog"),
    AI_RECOMMENDATION("ai_recommendation"),
    QUESTION_ANSWER("question_answer"),
    KEY_VALUE_PAIR("key_value_pair");
    final String typeName;

    NoteItemType(String typeName) {
        this.typeName = typeName;
    }


}
