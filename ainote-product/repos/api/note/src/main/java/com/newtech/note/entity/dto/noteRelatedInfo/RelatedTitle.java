package com.newtech.note.entity.dto.noteRelatedInfo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RelatedTitle {
    /**
     * 相关标题
     */
    private String title;
    /**
     * 相关标题的emoji
     */
    private String emoji;
}
