package com.newtech.note.client.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class PddErrorResponse {
    String error_msg;
    String sub_msg;

    int error_code;
    String request_id;
}
