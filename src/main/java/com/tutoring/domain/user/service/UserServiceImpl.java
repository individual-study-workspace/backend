package com.tutoring.domain.user.service;

import com.tutoring.domain.user.dto.UserResponse;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.global.error.ApiException;
import com.tutoring.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getMe(Long userId) {
        return userRepository.findById(userId)
            .map(UserResponse::from)
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
