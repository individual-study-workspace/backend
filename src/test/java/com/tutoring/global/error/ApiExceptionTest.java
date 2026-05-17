package com.tutoring.global.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionTest {

    @Test
    void carries_error_code_with_default_message() {
        ApiException ex = new ApiException(ErrorCode.USER_NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void carries_error_code_with_custom_message() {
        ApiException ex = new ApiException(ErrorCode.USER_NOT_FOUND, "id=42");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("id=42");
    }
}
