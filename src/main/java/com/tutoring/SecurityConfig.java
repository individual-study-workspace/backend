package com.tutoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // OPTIONS preflight 전체 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 나머지 /api/** 는 인증 필요
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                );

        // TODO: JWT 인증 필터 연동
        // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // PasswordEncoder  : 비밀번호 암호화 방식을 정의한 인터페이스 (encode, matches 메서드 제공)
    // BCryptPasswordEncoder : PasswordEncoder 의 구현체. BCrypt 해시 알고리즘으로 암호화.
    // 반환 타입을 인터페이스(PasswordEncoder)로 선언하면, 나중에 알고리즘을 바꿔도
    // 이 Bean 을 주입받는 코드(AuthService 등)를 수정할 필요가 없다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Nginx 리버스 프록시 환경에서 X-Forwarded-* 헤더를 처리한다.
     * application.yaml 의 server.forward-headers-strategy: framework 와 함께 동작.
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}