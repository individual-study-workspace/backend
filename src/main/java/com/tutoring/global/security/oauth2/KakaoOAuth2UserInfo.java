package com.tutoring.global.security.oauth2;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account == null ? null : (String) account.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> profile = profile();
        return profile == null ? null : (String) profile.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> profile = profile();
        return profile == null ? null : (String) profile.get("profile_image_url");
    }

    private Map<String, Object> profile() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) return null;
        return (Map<String, Object>) account.get("profile");
    }
}
