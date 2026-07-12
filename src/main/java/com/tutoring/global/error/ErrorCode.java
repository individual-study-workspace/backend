package com.tutoring.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Global
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),

    // Validation
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh 토큰을 찾을 수 없습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    USER_DELETED(HttpStatus.NOT_FOUND, "탈퇴한 사용자입니다"),

    // Classroom
    CLASSROOM_INVALID_BILLING_POLICY(HttpStatus.BAD_REQUEST, "청구정책 입력이 유효하지 않습니다"),
    INVITE_CODE_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 초대코드입니다"),
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초대코드 생성에 실패했습니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getCode() {
        return name();
    }
}
