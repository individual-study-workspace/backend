package com.tutoring.global.common;

import com.tutoring.global.error.ErrorCode;
import com.tutoring.global.error.FieldError;
import lombok.Builder;

import java.util.List;

public record ErrorResponse(String code, String message, List<FieldError> details) {

    public ErrorResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }

    @Builder
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

}
