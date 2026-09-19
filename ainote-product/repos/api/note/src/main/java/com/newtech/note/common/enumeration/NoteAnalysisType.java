package com.newtech.note.common.enumeration;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

@Getter
public enum NoteAnalysisType {
    ORGANIZE_WITH_ONE_CLICK(1, "一键整理"),
    ASSIST_CONTENT(2, "内容辅助"),
    DISCUSS_BY_VOICE(3, "语音讨论");


    private final int type;
    private final String name;

    NoteAnalysisType(int type, String name) {
        this.type = type;
        this.name = name;
    }


    public static List<Pair<String, String>> getDictEntries() {
        return Arrays.stream(NoteAnalysisType.values()).map(e -> Pair.of(String.valueOf(e.type), e.name)).toList();
    }

    public static NoteAnalysisType of(int analysisType) {
        if (analysisType == 0) {
            return null;
        }
        for (NoteAnalysisType e : NoteAnalysisType.values()) {
            if (e.type == analysisType) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid analysis type: " + analysisType);
    }
}
