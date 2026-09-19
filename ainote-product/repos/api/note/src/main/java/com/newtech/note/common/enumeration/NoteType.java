package com.newtech.note.common.enumeration;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
public enum NoteType {
    UNCLASSIFIED(0, "未分类"),
    INSPIRATION(1, "灵感记录"),
    TODO(2, "待办事项"),
    STUDY(3, "学习笔记"),
    TRAVEL(4, "旅行规划"),
    SHOPPING(5, "日常购物"),
    GOALS(6, "个人目标"),
    MEETING(7, "会议记录"),
    DIARY(8, "心情日记"),
    NETWORK(9, "人脉管理"),
    CREATIVE(10, "创意项目"),
    INDUSTRY(11, "行业了解"),
    ARTICLE(12, "好文摘录"),
    PASSWORD(13, "密码管理"),
    HEALTH(14, "健康相关"),
    FINANCE(15, "财务管理"),
    IMPORTANT_DATES(16, "重要日期"),
    LIFE_PLANNING(17, "人生规划"),
    MOVIES(18, "影片记录"),
    WORK_SOLUTIONS(19, "工作解决方案"),
    OTHER(20, "其他");


    private final int noteType;

    private final String noteTypeName;

    NoteType(int noteType, String noteTypeName) {
        this.noteType = noteType;
        this.noteTypeName = noteTypeName;
    }

    public static NoteType getType(int noteType) {
        return Stream.of(NoteType.values())
                .filter(t -> t.getNoteType() == noteType)
                .findFirst().orElse(null);
    }

    public static NoteType ofType(String noteTypeName) {
        Optional<NoteType> noteTypeOptional = Stream.of(NoteType.values())
                .filter(t -> t.getNoteTypeName().equals(noteTypeName))
                .findFirst();
        if (noteTypeOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid note type name: " + noteTypeName);
        }
        return noteTypeOptional.get();
    }

    public static List<Pair<String, String>> getDictEntries() {
        return Arrays.stream(NoteType.values()).map(e -> Pair.of(String.valueOf(e.noteType), e.noteTypeName)).toList();
    }
}
