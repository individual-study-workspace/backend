package com.tutoring.global.security.refresh;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStore {

    void save(Long userId, String token, Duration ttl);

    Optional<String> find(Long userId);

    void delete(Long userId);
}
