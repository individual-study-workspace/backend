package com.tutoring.global.security.oauth2;

import java.util.Map;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException(
                "지원하지 않는 OAuth2 provider: " + registrationId);
        };
    }
}
