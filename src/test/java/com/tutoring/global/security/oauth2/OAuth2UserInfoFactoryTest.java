package com.tutoring.global.security.oauth2;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2UserInfoFactoryTest {

    @Test
    void google_attributes_are_parsed() {
        Map<String, Object> attrs = Map.of(
            "sub", "google-sub-123",
            "email", "u@example.com",
            "name", "홍길동",
            "picture", "http://img/p.png"
        );
        OAuth2UserInfo info = OAuth2UserInfoFactory.from("google", attrs);

        assertThat(info.getProviderId()).isEqualTo("google-sub-123");
        assertThat(info.getEmail()).isEqualTo("u@example.com");
        assertThat(info.getName()).isEqualTo("홍길동");
        assertThat(info.getProfileImageUrl()).isEqualTo("http://img/p.png");
    }

    @Test
    void kakao_attributes_are_parsed_from_nested_structure() {
        Map<String, Object> attrs = Map.of(
            "id", 987654321L,
            "kakao_account", Map.of(
                "email", "k@example.com",
                "profile", Map.of(
                    "nickname", "김카카오",
                    "profile_image_url", "http://kakao/p.png"
                )
            )
        );
        OAuth2UserInfo info = OAuth2UserInfoFactory.from("kakao", attrs);

        assertThat(info.getProviderId()).isEqualTo("987654321");
        assertThat(info.getEmail()).isEqualTo("k@example.com");
        assertThat(info.getName()).isEqualTo("김카카오");
        assertThat(info.getProfileImageUrl()).isEqualTo("http://kakao/p.png");
    }

    @Test
    void unsupported_provider_throws() {
        assertThatThrownBy(() -> OAuth2UserInfoFactory.from("facebook", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
