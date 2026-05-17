package com.tutoring.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tutoring.global.error.ErrorCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorResponse(errorCode));
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(errorCode.getCode(), message));
    }

    public static <T> ApiResponse<T> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}
