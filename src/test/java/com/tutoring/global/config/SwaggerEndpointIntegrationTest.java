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
}
