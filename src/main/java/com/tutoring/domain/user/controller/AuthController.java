package com.tutoring.domain.user.controller;

import com.tutoring.domain.user.dto.LogoutRequest;
import com.tutoring.domain.user.dto.RefreshTokenRequest;
import com.tutoring.domain.user.dto.TokenResponse;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.error.ApiException;
import com.tutoring.global.error.ErrorCode;
import com.tutoring.global.security.jwt.JwtProperties;
import com.tutoring.global.security.jwt.JwtTokenProvider;
import com.tutoring.global.security.refresh.RefreshTokenStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @PostMapping("/refresh")
    @Operation(summary = "Refresh 토큰으로 새 Access/Refresh 발급 (rotation)")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        JwtTokenProvider.Claims claims;
        try {
            claims = tokenProvider.parse(request.refreshToken());
        } catch (JwtTokenProvider.ExpiredTokenException e) {
            throw new ApiException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtTokenProvider.InvalidTokenException e) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        String stored = refreshTokenStore.find(claims.userId())
            .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        if (!stored.equals(request.refreshToken())) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        User user = userRepository.findById(claims.userId())
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String newAccess  = tokenProvider.createAccessToken(user.getId(), user.getRole());
        String newRefresh = tokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), newRefresh,
                               Duration.ofMillis(jwtProperties.refreshTokenValidityMs()));

        return ApiResponse.success(new TokenResponse(newAccess, newRefresh));
    }

    @PostMapping("/logout")
    @Operation(summary = "Refresh 토큰 무효화 (로그아웃)")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            JwtTokenProvider.Claims claims = tokenProvider.parse(request.refreshToken());
            refreshTokenStore.delete(claims.userId());
        } catch (JwtTokenProvider.InvalidTokenException | JwtTokenProvider.ExpiredTokenException e) {
            // 의도적 무시 — 토큰이 무효라도 로그아웃은 멱등
        }
        return ApiResponse.success();
    }
}
