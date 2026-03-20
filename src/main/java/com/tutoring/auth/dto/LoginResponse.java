package com.tutoring.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    // TODO: refreshToken 추가 (JWT 구현 후)
}