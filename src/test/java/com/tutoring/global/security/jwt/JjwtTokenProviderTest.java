package com.tutoring.global.security.jwt;

import com.tutoring.domain.user.entity.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JjwtTokenProviderTest {

    private final JwtProperties props = new JwtProperties(
        "test-secret-test-secret-test-secret-test-secret-test-secret-test-secret",
        900_000L,
        1_209_600_000L
    );
    private final JjwtTokenProvider provider = new JjwtTokenProvider(props);

    @Test
    void issued_access_token_carries_user_id_and_role() {
        String token = provider.createAccessToken(42L, Role.USER);

        JwtTokenProvider.Claims claims = provider.parse(token);

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.role()).isEqualTo(Role.USER);
    }

    @Test
    void refresh_token_does_not_contain_role() {
        String token = provider.createRefreshToken(42L);

        JwtTokenProvider.Claims claims = provider.parse(token);

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.role()).isNull();
    }

    @Test
    void tampered_token_throws_invalid_token() {
        String token = provider.createAccessToken(42L, Role.USER);
        String tampered = token + "X";

        assertThatThrownBy(() -> provider.parse(tampered))
            .isInstanceOf(JwtTokenProvider.InvalidTokenException.class);
    }
}
