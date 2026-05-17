package com.tutoring.support;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;

public final class UserFixture {

    private UserFixture() {}

    public static User googleUser() {
        return User.create(AuthProvider.GOOGLE, "google-sub-123",
                           "user@example.com", "홍길동", "http://img/profile.png");
    }

    public static User kakaoUser() {
        return User.create(AuthProvider.KAKAO, "kakao-id-456",
                           "kakao@example.com", "김카카오", null);
    }
}
