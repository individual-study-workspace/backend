package com.tutoring.global.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoring.domain.user.dto.TokenResponse;
import com.tutoring.domain.user.entity.User;
import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.security.jwt.JwtProperties;
import com.tutoring.global.security.jwt.JwtTokenProvider;
import com.tutoring.global.security.refresh.RefreshTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2UserAdapter principal = (OAuth2UserAdapter) authentication.getPrincipal();
        User user = principal.getUser();

        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken,
                               Duration.ofMillis(jwtProperties.refreshTokenValidityMs()));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                                ApiResponse.success(new TokenResponse(accessToken, refreshToken)));
    }
}
