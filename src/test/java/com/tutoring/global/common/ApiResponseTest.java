package com.tutoring.global.common;

import com.tutoring.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_with_data() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getError()).isNull();
    }

    @Test
    void success_without_data() {
        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
    }

    @Test
    void fail_with_error_code() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.UNAUTHORIZED);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError().getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getError().getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void fail_with_custom_message() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.VALIDATION_FAILED, "email 형식이 잘못됨");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getError().getMessage()).isEqualTo("email 형식이 잘못됨");
    }
}
