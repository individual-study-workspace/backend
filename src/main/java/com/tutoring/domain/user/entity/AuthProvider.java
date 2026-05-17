package com.tutoring.domain.user.entity;

import java.util.Arrays;

public enum AuthProvider {
    GOOGLE,
    KAKAO;

    public static AuthProvider from(String registrationId) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 provider: " + registrationId));
    }
}
