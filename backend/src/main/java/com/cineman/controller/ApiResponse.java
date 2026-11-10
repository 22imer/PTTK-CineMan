package com.cineman.controller;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Đáp ứng JSON chung {@code { success, data?, message?, code? }} (ARCHITECTURE §7).
 *
 * <p>{@code code} tùy chọn dành cho lỗi xác thực phiên ({@code ACCESS_TOKEN_EXPIRED},
 * {@code SESSION_INVALID}, {@code REFRESH_CONFLICT}) để client phân biệt "refresh được" với "phải đăng
 * nhập lại"; các lỗi nghiệp vụ cũ giữ nguyên envelope không có {@code code}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, String code) {

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public static <T> ApiResponse<T> fail(String message, String code) {
        return new ApiResponse<>(false, null, message, code);
    }
}
