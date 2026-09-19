package com.newtech.note.entity.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
public class Message<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private T content;
}
