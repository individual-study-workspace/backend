package com.tutoring.global.security.jwt;

import com.tutoring.global.security.principal.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                JwtTokenProvider.Claims claims = tokenProvider.parse(token);
                // Refresh token이 Bearer로 잘못 전달된 경우 role이 null — 인증 부여하지 않음
                if (claims.role() != null) {
                    CustomUserPrincipal principal = new CustomUserPrincipal(claims.userId(), claims.role());
                    var authority = new SimpleGrantedAuthority(claims.role().getAuthority());
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    MDC.put("userId", String.valueOf(claims.userId()));
                }
            } catch (JwtTokenProvider.InvalidTokenException | JwtTokenProvider.ExpiredTokenException e) {
                // 의도적 무시 — 컨트롤러 도달 전 EntryPoint에서 401로 처리됨
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }
}
