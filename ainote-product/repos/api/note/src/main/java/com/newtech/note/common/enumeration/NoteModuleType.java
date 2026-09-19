package com.newtech.note.common.enumeration;

import lombok.Getter;

@Getter
public enum NoteModuleType {
    TEXT_DATA("text_data"),
    RELATED_NOTE("related_note"),
    AI_PICTURE("ai_picture"),
    QUESTION_ANSWER("question_answer"),
    BACKLOG("backlog"),
    AI_RECOMMENDATION("ai_recommendation"),
    //currently used for scene conversation
    SCENARIO_RECORD("scenario_record"),
    KEY_VALUE_PAIR("key_value_pair");
    final String typeName;

    NoteModuleType(String typeName) {
        this.typeName = typeName;
    }


}
