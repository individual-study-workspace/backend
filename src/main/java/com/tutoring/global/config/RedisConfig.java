package com.tutoring.global.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    // StringRedisTemplate은 Spring Boot 자동 구성에 의해 빈으로 등록됨.
    // 추후 RedisTemplate<String, Object>가 필요해질 때 이 자리에 정의.
}
