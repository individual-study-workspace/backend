package com.tutoring.domain.user.dto;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.Role;
import com.tutoring.domain.user.entity.User;

public record UserResponse(
    Long id,
    AuthProvider provider,
    String email,
    String name,
    String profileImageUrl,
    Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getProvider(),
            user.getEmail(),
            user.getName(),
            user.getProfileImageUrl(),
            user.getRole()
        );
    }
}
