package com.tutoring.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoring.IntegrationTestBase;
import com.tutoring.domain.user.dto.LogoutRequest;
import com.tutoring.domain.user.dto.RefreshTokenRequest;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.global.security.jwt.JwtProperties;
import com.tutoring.global.security.jwt.JwtTokenProvider;
import com.tutoring.global.security.refresh.RefreshTokenStore;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired RefreshTokenStore refreshTokenStore;
    @Autowired JwtProperties jwtProperties;

    @Test
    void refresh_issues_new_tokens_when_refresh_valid() throws Exception {
        User saved = userRepository.save(UserFixture.googleUser());
        String refreshToken = tokenProvider.createRefreshToken(saved.getId());
        refreshTokenStore.save(saved.getId(), refreshToken,
                               Duration.ofMillis(jwtProperties.refreshTokenValidityMs()));

        String body = objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_returns_401_when_token_not_in_store() throws Exception {
        User saved = userRepository.save(UserFixture.googleUser());
        String refreshToken = tokenProvider.createRefreshToken(saved.getId());
        // 일부러 store에 저장하지 않음

        String body = objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_NOT_FOUND"));
    }

    @Test
    void logout_deletes_refresh_token() throws Exception {
        User saved = userRepository.save(UserFixture.googleUser());
        String refreshToken = tokenProvider.createRefreshToken(saved.getId());
        refreshTokenStore.save(saved.getId(), refreshToken, Duration.ofMinutes(5));

        String body = objectMapper.writeValueAsString(new LogoutRequest(refreshToken));

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertThat(refreshTokenStore.find(saved.getId())).isEmpty();
    }
}
