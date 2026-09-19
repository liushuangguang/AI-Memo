package com.newtech.note.entity.request;

import lombok.Data;

@Data
public class LoginReq {
    private String mobile;
    private String code;
}