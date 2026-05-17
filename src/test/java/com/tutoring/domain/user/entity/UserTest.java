package com.tutoring.domain.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void create_factory_initializes_required_fields_with_default_role() {
        User user = User.create(AuthProvider.GOOGLE, "sub-123",
                                "user@example.com", "홍길동", "http://img/profile.png");

        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getProviderId()).isEqualTo("sub-123");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getProfileImageUrl()).isEqualTo("http://img/profile.png");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void updateProfile_changes_name_and_image() {
        User user = User.create(AuthProvider.KAKAO, "k-1", "u@e.com", "old", null);

        user.updateProfile("new", "http://new.png");

        assertThat(user.getName()).isEqualTo("new");
        assertThat(user.getProfileImageUrl()).isEqualTo("http://new.png");
    }

    @Test
    void role_authority_has_role_prefix() {
        assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_USER");
        assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
