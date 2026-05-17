package com.tutoring.global.security.jwt;

import com.tutoring.domain.user.entity.Role;

public interface JwtTokenProvider {

    String createAccessToken(Long userId, Role role);

    String createRefreshToken(Long userId);

    Claims parse(String token) throws InvalidTokenException, ExpiredTokenException;

    record Claims(Long userId, Role role) {}

    class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) { super(message, cause); }
    }

    class ExpiredTokenException extends RuntimeException {
        public ExpiredTokenException(String message, Throwable cause) { super(message, cause); }
    }
}
