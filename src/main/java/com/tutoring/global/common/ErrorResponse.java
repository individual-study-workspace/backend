package com.tutoring.global.common;

import com.tutoring.global.error.ErrorCode;
import com.tutoring.global.error.FieldError;
import lombok.Getter;

import java.util.List;

@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<FieldError> details;

    public ErrorResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    public ErrorResponse(String code, String message, List<FieldError> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
