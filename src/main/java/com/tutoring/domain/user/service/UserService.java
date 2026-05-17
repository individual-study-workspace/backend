package com.tutoring.domain.user.service;

import com.tutoring.domain.user.dto.UserResponse;

public interface UserService {

    UserResponse getMe(Long userId);
}
