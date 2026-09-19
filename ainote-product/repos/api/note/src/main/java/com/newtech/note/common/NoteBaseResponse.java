package com.newtech.note.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteBaseResponse<T> {
    private int code;
    private String message;
    private T data;

    public NoteBaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public NoteBaseResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public NoteBaseResponse(int code) {
        this.code = code;
    }

    public NoteBaseResponse() {
        this.code = 200;
    }

    public boolean isSuccess() {
        return code == 200;
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public static <T> NoteBaseResponse<T> success() {
        return new NoteBaseResponse<>(200, "success");
    }

    public static <T> NoteBaseResponse<T> success(T data) {
        return new NoteBaseResponse<>(200, "success", data);
    }

    public static <T> NoteBaseResponse<T> success(String message, T data) {
        return new NoteBaseResponse<>(200, message, data);
    }

    public static <T> NoteBaseResponse<T> success(int code, String message, T data) {
        return new NoteBaseResponse<>(code, message, data);
    }


    public static <T> NoteBaseResponse<T> success(int code, String message) {
        return new NoteBaseResponse<>(code, message);
    }

    public static <T> NoteBaseResponse<T> success(int code) {
        return new NoteBaseResponse<>(code, "success");
    }


    public static <T> NoteBaseResponse<T> failure(int code, String message) {
        return new NoteBaseResponse<>(code, message);
    }

    public static <T> NoteBaseResponse<T> failure(int code) {
        return new NoteBaseResponse<>(code, "failure");
    }

    public static <T> NoteBaseResponse<T> failure() {
        return new NoteBaseResponse<>(500, "failure");
    }

    public static <T> NoteBaseResponse<T> failure(String message) {
        return new NoteBaseResponse<>(500, message);
    }

    public static <T> NoteBaseResponse<T> failure(Exception e) {
        return new NoteBaseResponse<>(500, e.getMessage());
    }

    public static <T> NoteBaseResponse<T> failure(Exception e, String message) {
        return new NoteBaseResponse<>(500, message + " : " + e.getMessage());
    }

    public static <T> NoteBaseResponse<T> failure(int code, String message, Exception e) {
        return new NoteBaseResponse<>(code, message + " : " + e.getMessage());
    }

    public static <T> NoteBaseResponse<T> failure(int code, Exception e) {
        return new NoteBaseResponse<>(code, e.getMessage());
    }

    public static <T> NoteBaseResponse<T> failure(int code, String message, String exceptionMessage) {
        return new NoteBaseResponse<>(code, message + " : " + exceptionMessage);
    }

    public static <T> NoteBaseResponse<T> failure(String message, String exceptionMessage) {
        return new NoteBaseResponse<>(500, message + " : " + exceptionMessage);
    }

    public static <T> NoteBaseResponse<T> failure(int code, String message, String exceptionMessage, T data) {
        return new NoteBaseResponse<>(code, message + " : " + exceptionMessage, data);
    }

    public static <T> NoteBaseResponse<T> failure(String message, String exceptionMessage, T data) {
        return new NoteBaseResponse<>(500, message + " : " + exceptionMessage, data);
    }


}
