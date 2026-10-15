package com.qrorder.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebResponse<T>(
        T data,
        String message,
        Object errors
) {
    public static <T> WebResponse<T> success(T data) {
        return new WebResponse<>(data, "OK", null);
    }

    public static <T> WebResponse<T> success(T data, String message) {
        return new WebResponse<>(data, message, null);
    }

    public static <T> WebResponse<T> error(String message, Object errors) {
        return new WebResponse<>(null, message, errors);
    }
}
