package com.newtech.note.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class NotePageData<T> {
    private List<T> content;
    private int currentPage;
    private long totalPages;
    private long totalItems;
}
