package com.tutoring.global.error;

import com.tutoring.global.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void api_exception_maps_to_error_code_status_and_body() {
        ApiException ex = new ApiException(ErrorCode.USER_NOT_FOUND, "id=42");

        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(ex);

        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().code()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getBody().getError().message()).isEqualTo("id=42");
    }

    @Test
    void unhandled_exception_maps_to_internal_error() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnknown(new RuntimeException("boom"));

        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody().getError().code()).isEqualTo("INTERNAL_ERROR");
    }
}
