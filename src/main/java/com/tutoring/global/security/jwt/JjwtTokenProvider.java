package com.tutoring.global.security.jwt;

import com.tutoring.domain.user.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JjwtTokenProvider implements JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JjwtTokenProvider(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = props.accessTokenValidityMs();
        this.refreshTokenValidityMs = props.refreshTokenValidityMs();
    }

    @Override
    public String createAccessToken(Long userId, Role role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim(CLAIM_ROLE, role.name())
            .issuedAt(new Date(now))
            .expiration(new Date(now + accessTokenValidityMs))
            .signWith(key)
            .compact();
    }

    @Override
    public String createRefreshToken(Long userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .issuedAt(new Date(now))
            .expiration(new Date(now + refreshTokenValidityMs))
            .signWith(key)
            .compact();
    }

    @Override
    public Claims parse(String token) {
        try {
            io.jsonwebtoken.Claims raw = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            Long userId = Long.parseLong(raw.getSubject());
            String roleName = raw.get(CLAIM_ROLE, String.class);
            Role role = (roleName != null) ? Role.valueOf(roleName) : null;
            return new Claims(userId, role);

        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("Expired JWT", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid JWT", e);
        }
    }
}
