package com.tutoring.global.config;

import com.tutoring.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SwaggerEndpointIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;

    @Test
    void openapi_spec_is_publicly_accessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Tutoring API"))
            .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt").exists());
    }

    @Test
    void classroom_create_is_documented_via_interface() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            // ClassroomApi 인터페이스의 @Operation/@Tag 가 문서에 반영된다
            .andExpect(jsonPath("$.paths['/api/v1/classrooms'].post.summary").value("강의실 생성"))
            .andExpect(jsonPath("$.paths['/api/v1/classrooms'].post.tags[0]").value("Classroom"))
            .andExpect(jsonPath("$.paths['/api/v1/classrooms/invite-code'].get.summary").value("초대코드 발급"))
            .andExpect(jsonPath("$.components.schemas.CreateClassroomRequest").exists())
            // @AuthenticationPrincipal 파라미터는 문서에 노출되지 않는다
            .andExpect(jsonPath("$.components.schemas.CustomUserPrincipal").doesNotExist());
    }
}
