package com.tutoring.auth.service;

import com.tutoring.auth.dto.LoginRequest;
import com.tutoring.auth.dto.LoginResponse;
import com.tutoring.auth.entity.User;
import com.tutoring.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // TODO: JWT 토큰 생성 (JwtTokenProvider 구현 후 교체)
        String accessToken = "TODO_JWT_TOKEN";

        return new LoginResponse(accessToken);
    }
}