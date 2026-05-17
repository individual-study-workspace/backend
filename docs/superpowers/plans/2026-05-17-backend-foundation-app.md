# Backend Foundation App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Boot 3.5.11 + Java 21 백엔드의 애플리케이션 코드 Foundation을 구현한다. 빌드 정비부터 OAuth2(Google/Kakao)→자체 JWT 인증 파이프라인 + `User` 엔티티 + `/api/v1/me` 수직 슬라이스가 통합 테스트로 검증되는 상태까지 완성한다. 배포 인프라(Dockerfile, NGINX, GitHub Actions)는 별도 Plan B에서 다룬다.

**Architecture:** Layered + Domain packaging. `global/`(횡단 관심사) + `domain/<name>/`(도메인 슬라이스). 외부 의존(JWT, Redis, OAuth2 provider)은 인터페이스 뒤로 추상화(DIP). 모든 작업은 TDD red-green-refactor로 작성. 응답 포맷은 `ApiResponse<T>` 래퍼 + RFC 7807 친화 에러 코드.

**Tech Stack:** Java 21, Spring Boot 3.5.11, Spring Security 6.4+, Spring Data JPA(Hibernate 6.6), Spring Data Redis, MySQL 8.0, Redis 7, Flyway 10, JJWT 0.12, springdoc-openapi 2.6, JUnit5, Mockito, Testcontainers 1.20, Lombok.

**Spec reference:** `docs/superpowers/specs/2026-05-17-backend-foundation-design.md`

---

## Prerequisites (each developer's machine)

- Java 21 (Temurin 권장)
- Docker Desktop + Docker Compose v2
- IntelliJ IDEA (or VS Code with Java extensions)
- `~/.testcontainers.properties`에 `testcontainers.reuse.enable=true` 추가 (테스트 부팅 가속)

---

## File Structure (이 plan에서 생성/수정되는 파일)

### 생성

```
build.gradle                                                          # 수정
src/main/resources/
├── application.yml                                                    # 신규
├── application-local.yml.template                                     # 신규
├── application-prod.yml.template                                      # 신규
├── logback-spring.xml                                                 # 신규
└── db/migration/
    └── V202605170001__create_users.sql                                # 신규
src/test/resources/
└── application-test.yml                                               # 신규

src/main/java/com/tutoring/
├── TutoringApplication.java                                           # 수정 (@EnableJpaAuditing 제거)
├── global/
│   ├── config/
│   │   ├── JpaAuditingConfig.java
│   │   ├── RedisConfig.java
│   │   ├── CorsConfig.java
│   │   └── SwaggerConfig.java
│   ├── common/
│   │   ├── ApiResponse.java
│   │   ├── ErrorResponse.java
│   │   ├── Pagination.java
│   │   └── PageResponse.java
│   ├── error/
│   │   ├── ErrorCode.java
│   │   ├── ApiException.java
│   │   ├── FieldError.java
│   │   └── GlobalExceptionHandler.java
│   ├── logging/
│   │   └── MdcLoggingFilter.java
│   └── security/
│       ├── SecurityConfig.java
│       ├── jwt/
│       │   ├── JwtProperties.java
│       │   ├── JwtTokenProvider.java                  (interface)
│       │   ├── JjwtTokenProvider.java
│       │   └── JwtAuthenticationFilter.java
│       ├── oauth2/
│       │   ├── OAuth2UserInfo.java                    (interface)
│       │   ├── GoogleOAuth2UserInfo.java
│       │   ├── KakaoOAuth2UserInfo.java
│       │   ├── OAuth2UserInfoFactory.java
│       │   ├── CustomOAuth2UserService.java
│       │   └── OAuth2LoginSuccessHandler.java
│       ├── principal/
│       │   └── CustomUserPrincipal.java
│       ├── handler/
│       │   ├── JsonAuthEntryPoint.java
│       │   └── JsonAccessDeniedHandler.java
│       └── refresh/
│           ├── RefreshTokenStore.java                 (interface)
│           └── RedisRefreshTokenStore.java
└── domain/
    └── user/
        ├── controller/
        │   ├── UserController.java
        │   └── AuthController.java
        ├── service/
        │   ├── UserService.java                       (interface)
        │   └── UserServiceImpl.java
        ├── repository/
        │   └── UserRepository.java
        ├── entity/
        │   ├── BaseEntity.java
        │   ├── User.java
        │   ├── Role.java
        │   └── AuthProvider.java
        └── dto/
            ├── UserResponse.java
            ├── TokenResponse.java
            ├── RefreshTokenRequest.java
            └── LogoutRequest.java

src/test/java/com/tutoring/
├── IntegrationTestBase.java
├── support/
│   └── UserFixture.java
├── global/...                                          (단위/슬라이스 테스트)
└── domain/user/...                                     (단위/슬라이스/통합 테스트)

docker-compose.yml                                                     # 신규 (로컬 의존성)
docs/superpowers/local-setup.md                                        # 신규
```

### 수정

- `build.gradle` (의존성 정리)
- `TutoringApplication.java` (`@EnableJpaAuditing` 제거 — `JpaAuditingConfig`로 이동)
- `.gitignore` 추가 예외 (필요 시; 이미 spec 커밋에서 반영됨)

---

## Task 1: build.gradle 의존성 정비

**Files:**
- Modify: `build.gradle`

**Why first:** 모든 후속 코드는 새 의존성을 필요로 한다.

- [ ] **Step 1: `build.gradle` 전체 교체**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.11'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'jacoco'
}

group = 'com.tutoring'
version = '0.0.1-SNAPSHOT'
description = '과외 워크스페이스'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web (REST)
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Persistence
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    runtimeOnly    'com.mysql:mysql-connector-j'

    // Migration
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'

    // Security + OAuth2
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'

    // JWT (JJWT 0.12.x)
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // OpenAPI / Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'

    // Structured JSON logging
    implementation 'net.logstash.logback:logstash-logback-encoder:8.0'

    // Actuator
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Lombok
    compileOnly        'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testCompileOnly    'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'

    // Dev only
    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter:1.20.3'
    testImplementation 'org.testcontainers:mysql:1.20.3'
    testRuntimeOnly    'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        html.required = true
        xml.required  = true
    }
}
```

변경 요약:
- 제거: `spring-boot-starter-data-jdbc`, `spring-boot-starter-web-services`
- 추가: `web`, `validation`, `data-redis`, `flyway-core`, `flyway-mysql`, `jjwt-*`, `springdoc-openapi`, `logstash-logback-encoder`, `actuator`, JaCoCo 플러그인, Testcontainers

- [ ] **Step 2: 빌드 확인**

```bash
./gradlew clean build -x test
```

Expected: `BUILD SUCCESSFUL`. 컴파일만, 테스트는 아직 없음.

- [ ] **Step 3: Commit**

```bash
git add build.gradle
git commit -m "build: REST/Redis/Flyway/JWT 의존성 정비 + JaCoCo"
```

---

## Task 2: 공통 응답 타입 (`ApiResponse`, `ErrorResponse`, `FieldError`, `Pagination`, `PageResponse`)

**Files:**
- Create: `src/main/java/com/tutoring/global/error/ErrorCode.java`
- Create: `src/main/java/com/tutoring/global/error/FieldError.java`
- Create: `src/main/java/com/tutoring/global/common/ErrorResponse.java`
- Create: `src/main/java/com/tutoring/global/common/ApiResponse.java`
- Create: `src/main/java/com/tutoring/global/common/Pagination.java`
- Create: `src/main/java/com/tutoring/global/common/PageResponse.java`
- Test:   `src/test/java/com/tutoring/global/common/ApiResponseTest.java`

**Why before everything else:** 후속 task의 컨트롤러/예외 핸들러가 모두 이 타입을 반환한다.

- [ ] **Step 1: 실패 테스트 작성** — `src/test/java/com/tutoring/global/common/ApiResponseTest.java`

```java
package com.tutoring.global.common;

import com.tutoring.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_with_data() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getError()).isNull();
    }

    @Test
    void success_without_data() {
        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
    }

    @Test
    void fail_with_error_code() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.UNAUTHORIZED);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError().getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getError().getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void fail_with_custom_message() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.VALIDATION_FAILED, "email 형식이 잘못됨");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getError().getMessage()).isEqualTo("email 형식이 잘못됨");
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

Expected: `cannot find symbol` 에러 — `ApiResponse`, `ErrorCode` 등이 아직 없음.

- [ ] **Step 3: `ErrorCode` 작성** — `src/main/java/com/tutoring/global/error/ErrorCode.java`

```java
package com.tutoring.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Global
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),

    // Validation
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh 토큰을 찾을 수 없습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    USER_DELETED(HttpStatus.NOT_FOUND, "탈퇴한 사용자입니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getCode() {
        return name();
    }
}
```

- [ ] **Step 4: `FieldError` 작성** — `src/main/java/com/tutoring/global/error/FieldError.java`

```java
package com.tutoring.global.error;

public record FieldError(String field, String reason) {}
```

- [ ] **Step 5: `ErrorResponse` 작성** — `src/main/java/com/tutoring/global/common/ErrorResponse.java`

```java
package com.tutoring.global.common;

import com.tutoring.global.error.ErrorCode;
import com.tutoring.global.error.FieldError;
import lombok.Getter;

import java.util.List;

@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<FieldError> details;

    public ErrorResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    public ErrorResponse(String code, String message, List<FieldError> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
```

- [ ] **Step 6: `ApiResponse` 작성** — `src/main/java/com/tutoring/global/common/ApiResponse.java`

```java
package com.tutoring.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tutoring.global.error.ErrorCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorResponse(errorCode));
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(errorCode.getCode(), message));
    }

    public static <T> ApiResponse<T> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}
```

- [ ] **Step 7: `Pagination` 작성** — `src/main/java/com/tutoring/global/common/Pagination.java`

```java
package com.tutoring.global.common;

public record Pagination(boolean hasNext, String nextCursor) {}
```

- [ ] **Step 8: `PageResponse` 작성** — `src/main/java/com/tutoring/global/common/PageResponse.java`

```java
package com.tutoring.global.common;

import java.util.List;

public record PageResponse<T>(List<T> content, Pagination pagination) {}
```

- [ ] **Step 9: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.common.ApiResponseTest"
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/tutoring/global/common src/main/java/com/tutoring/global/error src/test/java/com/tutoring/global/common
git commit -m "feat(common): ApiResponse/ErrorResponse/Pagination 공통 타입 + ErrorCode"
```

---

## Task 3: `ApiException`

**Files:**
- Create: `src/main/java/com/tutoring/global/error/ApiException.java`
- Test:   `src/test/java/com/tutoring/global/error/ApiExceptionTest.java`

- [ ] **Step 1: 실패 테스트** — `ApiExceptionTest.java`

```java
package com.tutoring.global.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionTest {

    @Test
    void carries_error_code_with_default_message() {
        ApiException ex = new ApiException(ErrorCode.USER_NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void carries_error_code_with_custom_message() {
        ApiException ex = new ApiException(ErrorCode.USER_NOT_FOUND, "id=42");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("id=42");
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: 구현** — `ApiException.java`

```java
package com.tutoring.global.error;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.error.ApiExceptionTest"
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tutoring/global/error/ApiException.java src/test/java/com/tutoring/global/error/ApiExceptionTest.java
git commit -m "feat(error): ApiException 도메인 예외 타입"
```

---

## Task 4: `BaseEntity` + `JpaAuditingConfig`

**Files:**
- Create: `src/main/java/com/tutoring/global/config/JpaAuditingConfig.java`
- Create: `src/main/java/com/tutoring/domain/user/entity/BaseEntity.java`
- Modify: `src/main/java/com/tutoring/TutoringApplication.java` (remove `@EnableJpaAuditing`)
- Test:   `src/test/java/com/tutoring/domain/user/entity/BaseEntityTest.java`

- [ ] **Step 1: 실패 테스트** — `BaseEntityTest.java`

```java
package com.tutoring.domain.user.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    private static class TestEntity extends BaseEntity {}

    @Test
    void initial_state_is_not_deleted() {
        TestEntity e = new TestEntity();
        assertThat(e.isDeleted()).isFalse();
        assertThat(e.getDeletedAt()).isNull();
    }

    @Test
    void markDeleted_sets_deletedAt_timestamp() {
        TestEntity e = new TestEntity();
        Instant before = Instant.now();
        e.markDeleted();
        Instant after = Instant.now();

        assertThat(e.isDeleted()).isTrue();
        assertThat(e.getDeletedAt()).isBetween(before, after);
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `BaseEntity` 구현** — `BaseEntity.java`

```java
package com.tutoring.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }
}
```

- [ ] **Step 4: `JpaAuditingConfig` 구현** — `JpaAuditingConfig.java`

```java
package com.tutoring.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

- [ ] **Step 5: `TutoringApplication`에서 `@EnableJpaAuditing` 제거**

`TutoringApplication.java`를 다음과 같이 수정:

```java
package com.tutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TutoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TutoringApplication.class, args);
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.domain.user.entity.BaseEntityTest"
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/tutoring/global/config/JpaAuditingConfig.java \
        src/main/java/com/tutoring/domain/user/entity/BaseEntity.java \
        src/main/java/com/tutoring/TutoringApplication.java \
        src/test/java/com/tutoring/domain/user/entity/BaseEntityTest.java
git commit -m "feat(global): BaseEntity 도입, JPA Auditing 별도 Config로 분리"
```

---

## Task 5: `Role`, `AuthProvider`, `User` 엔티티

**Files:**
- Create: `src/main/java/com/tutoring/domain/user/entity/Role.java`
- Create: `src/main/java/com/tutoring/domain/user/entity/AuthProvider.java`
- Create: `src/main/java/com/tutoring/domain/user/entity/User.java`
- Test:   `src/test/java/com/tutoring/domain/user/entity/UserTest.java`

- [ ] **Step 1: 실패 테스트** — `UserTest.java`

```java
package com.tutoring.domain.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void create_factory_initializes_required_fields_with_default_role() {
        User user = User.create(AuthProvider.GOOGLE, "sub-123",
                                "user@example.com", "홍길동", "http://img/profile.png");

        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getProviderId()).isEqualTo("sub-123");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getProfileImageUrl()).isEqualTo("http://img/profile.png");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void updateProfile_changes_name_and_image() {
        User user = User.create(AuthProvider.KAKAO, "k-1", "u@e.com", "old", null);

        user.updateProfile("new", "http://new.png");

        assertThat(user.getName()).isEqualTo("new");
        assertThat(user.getProfileImageUrl()).isEqualTo("http://new.png");
    }

    @Test
    void role_authority_has_role_prefix() {
        assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_USER");
        assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `Role` 구현** — `Role.java`

```java
package com.tutoring.domain.user.entity;

public enum Role {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
```

- [ ] **Step 4: `AuthProvider` 구현** — `AuthProvider.java`

```java
package com.tutoring.domain.user.entity;

import java.util.Arrays;

public enum AuthProvider {
    GOOGLE,
    KAKAO;

    public static AuthProvider from(String registrationId) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 provider: " + registrationId));
    }
}
```

- [ ] **Step 5: `User` 구현** — `User.java`

```java
package com.tutoring.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_users_provider_provider_id",
        columnNames = {"provider", "provider_id"}
    )
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private User(AuthProvider provider, String providerId, String email,
                 String name, String profileImageUrl, Role role) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    public static User create(AuthProvider provider, String providerId,
                              String email, String name, String profileImageUrl) {
        return new User(provider, providerId, email, name, profileImageUrl, Role.USER);
    }

    public void updateProfile(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.domain.user.entity.UserTest"
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/tutoring/domain/user/entity/Role.java \
        src/main/java/com/tutoring/domain/user/entity/AuthProvider.java \
        src/main/java/com/tutoring/domain/user/entity/User.java \
        src/test/java/com/tutoring/domain/user/entity/UserTest.java
git commit -m "feat(user): Role/AuthProvider/User 엔티티 + 정적 팩토리"
```

---

## Task 6: Flyway baseline migration

**Files:**
- Create: `src/main/resources/db/migration/V202605170001__create_users.sql`

- [ ] **Step 1: SQL 파일 작성** — `V202605170001__create_users.sql`

```sql
CREATE TABLE users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    provider            VARCHAR(20)  NOT NULL,
    provider_id         VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    name                VARCHAR(50)  NOT NULL,
    profile_image_url   VARCHAR(500) NULL,
    role                VARCHAR(20)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    deleted_at          DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id),
    INDEX idx_users_email (email),
    INDEX idx_users_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 2: Commit (테스트는 Task 8에서 DB 통합 시 검증)**

```bash
git add src/main/resources/db/migration/V202605170001__create_users.sql
git commit -m "feat(db): Flyway baseline - create users table"
```

---

## Task 7: `UserRepository`

**Files:**
- Create: `src/main/java/com/tutoring/domain/user/repository/UserRepository.java`

**Test는 Task 8(IntegrationTestBase)에서 함께 작성** — 격리된 단위 테스트는 불가하고, Testcontainers 기반 `@DataJpaTest`가 필요.

- [ ] **Step 1: 인터페이스 작성** — `UserRepository.java`

```java
package com.tutoring.domain.user.repository;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tutoring/domain/user/repository/UserRepository.java
git commit -m "feat(user): UserRepository - findByProviderAndProviderId"
```

---

## Task 8: application.yml 프로필 + Testcontainers `IntegrationTestBase` + `UserRepository` 통합 테스트

**Files:**
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml.template`
- Create: `src/main/resources/application-prod.yml.template`
- Create: `src/test/resources/application-test.yml`
- Create: `src/test/java/com/tutoring/IntegrationTestBase.java`
- Create: `src/test/java/com/tutoring/support/UserFixture.java`
- Create: `src/test/java/com/tutoring/domain/user/repository/UserRepositoryIntegrationTest.java`

**Why here:** 이후 task들은 Spring 컨텍스트 부팅 가능한 통합 테스트를 필요로 한다.

- [ ] **Step 1: `application.yml` (공통)** — `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: tutoring
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate.format_sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080
  shutdown: graceful

spring.lifecycle.timeout-per-shutdown-phase: 30s

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    root: INFO
    com.tutoring: DEBUG

app:
  jwt:
    secret: change-me-in-real-env-min-256-bits-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    access-token-validity-ms: 900000        # 15분
    refresh-token-validity-ms: 1209600000   # 14일
  cors:
    allowed-origins: ""
```

- [ ] **Step 2: `application-local.yml.template`** — `src/main/resources/application-local.yml.template`

```yaml
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:mysql://localhost:3306/tutoring?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: tutoring
    password: tutoring
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: REPLACE_ME
            client-secret: REPLACE_ME
            scope:
              - openid
              - email
              - profile
          kakao:
            client-id: REPLACE_ME
            client-secret: REPLACE_ME
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
            scope:
              - profile_nickname
              - account_email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

app:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:8080
```

- [ ] **Step 3: `application-prod.yml.template`** — `src/main/resources/application-prod.yml.template`

```yaml
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - openid
              - email
              - profile
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
            scope:
              - profile_nickname
              - account_email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

app:
  jwt:
    secret: ${JWT_SECRET}
  cors:
    allowed-origins: ${CORS_ORIGINS}

logging:
  level:
    com.tutoring: INFO
```

- [ ] **Step 4: `application-test.yml`** — `src/test/resources/application-test.yml`

```yaml
spring:
  config:
    activate:
      on-profile: test
  jpa:
    hibernate:
      ddl-auto: validate
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test-google-id
            client-secret: test-google-secret
          kakao:
            client-id: test-kakao-id
            client-secret: test-kakao-secret
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
            scope:
              - profile_nickname
              - account_email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

app:
  jwt:
    secret: test-secret-test-secret-test-secret-test-secret-test-secret-test-secret
    access-token-validity-ms: 900000
    refresh-token-validity-ms: 1209600000
  cors:
    allowed-origins: http://localhost:3000
```

- [ ] **Step 5: `IntegrationTestBase`** — `src/test/java/com/tutoring/IntegrationTestBase.java`

```java
package com.tutoring;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Container
    static final MySQLContainer<?> mysql =
        new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("tutoring")
            .withUsername("tutoring")
            .withPassword("tutoring")
            .withReuse(true);

    @Container
    static final GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    static {
        mysql.start();
        redis.start();
    }

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.data.redis.host",     redis::getHost);
        r.add("spring.data.redis.port",     () -> redis.getMappedPort(6379));
    }
}
```

- [ ] **Step 6: `UserFixture`** — `src/test/java/com/tutoring/support/UserFixture.java`

```java
package com.tutoring.support;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;

public final class UserFixture {

    private UserFixture() {}

    public static User googleUser() {
        return User.create(AuthProvider.GOOGLE, "google-sub-123",
                           "user@example.com", "홍길동", "http://img/profile.png");
    }

    public static User kakaoUser() {
        return User.create(AuthProvider.KAKAO, "kakao-id-456",
                           "kakao@example.com", "김카카오", null);
    }
}
```

- [ ] **Step 7: `UserRepositoryIntegrationTest`** — `src/test/java/com/tutoring/domain/user/repository/UserRepositoryIntegrationTest.java`

```java
package com.tutoring.domain.user.repository;

import com.tutoring.IntegrationTestBase;
import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UserRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;

    @Test
    void save_and_find_by_provider_and_provider_id() {
        User saved = userRepository.save(UserFixture.googleUser());

        Optional<User> found = userRepository.findByProviderAndProviderId(
            AuthProvider.GOOGLE, "google-sub-123");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void soft_deleted_user_is_excluded_from_default_query() {
        User user = userRepository.save(UserFixture.googleUser());
        user.markDeleted();
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByProviderAndProviderId(
            AuthProvider.GOOGLE, "google-sub-123");

        assertThat(found).isEmpty();
    }
}
```

- [ ] **Step 8: 테스트 실행 (Docker 필요)**

```bash
./gradlew test --tests "com.tutoring.domain.user.repository.UserRepositoryIntegrationTest"
```

Expected: 2 tests passed. 첫 실행은 Docker 이미지 pull로 1~2분 소요.

- [ ] **Step 9: Commit**

```bash
git add src/main/resources/application.yml \
        src/main/resources/application-local.yml.template \
        src/main/resources/application-prod.yml.template \
        src/test/resources/application-test.yml \
        src/test/java/com/tutoring/IntegrationTestBase.java \
        src/test/java/com/tutoring/support/UserFixture.java \
        src/test/java/com/tutoring/domain/user/repository/UserRepositoryIntegrationTest.java
git commit -m "feat(test): Testcontainers IntegrationTestBase + UserRepository 통합 테스트"
```

---

## Task 9: `JwtProperties` + `JwtTokenProvider` (interface) + `JjwtTokenProvider`

**Files:**
- Create: `src/main/java/com/tutoring/global/security/jwt/JwtProperties.java`
- Create: `src/main/java/com/tutoring/global/security/jwt/JwtTokenProvider.java`
- Create: `src/main/java/com/tutoring/global/security/jwt/JjwtTokenProvider.java`
- Modify: `src/main/java/com/tutoring/TutoringApplication.java` (add `@ConfigurationPropertiesScan`)
- Test:   `src/test/java/com/tutoring/global/security/jwt/JjwtTokenProviderTest.java`

- [ ] **Step 1: 실패 테스트** — `JjwtTokenProviderTest.java`

```java
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
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `JwtProperties`** — `JwtProperties.java`

```java
package com.tutoring.global.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    @NotBlank String secret,
    @Positive long accessTokenValidityMs,
    @Positive long refreshTokenValidityMs
) {}
```

- [ ] **Step 4: `JwtTokenProvider` 인터페이스** — `JwtTokenProvider.java`

```java
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
```

- [ ] **Step 5: `JjwtTokenProvider` 구현** — `JjwtTokenProvider.java`

```java
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
```

- [ ] **Step 6: `TutoringApplication`에 `@ConfigurationPropertiesScan` 추가**

```java
package com.tutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TutoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TutoringApplication.class, args);
    }
}
```

- [ ] **Step 7: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.security.jwt.JjwtTokenProviderTest"
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/tutoring/global/security/jwt \
        src/main/java/com/tutoring/TutoringApplication.java \
        src/test/java/com/tutoring/global/security/jwt
git commit -m "feat(security): JwtTokenProvider 추상화 + JJWT 0.12 구현"
```

---

## Task 10: `RedisConfig` + `RefreshTokenStore` (interface) + `RedisRefreshTokenStore`

**Files:**
- Create: `src/main/java/com/tutoring/global/config/RedisConfig.java`
- Create: `src/main/java/com/tutoring/global/security/refresh/RefreshTokenStore.java`
- Create: `src/main/java/com/tutoring/global/security/refresh/RedisRefreshTokenStore.java`
- Test:   `src/test/java/com/tutoring/global/security/refresh/RedisRefreshTokenStoreIntegrationTest.java`

- [ ] **Step 1: 실패 통합 테스트** — `RedisRefreshTokenStoreIntegrationTest.java`

```java
package com.tutoring.global.security.refresh;

import com.tutoring.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RedisRefreshTokenStoreIntegrationTest extends IntegrationTestBase {

    @Autowired RefreshTokenStore store;

    @Test
    void save_and_find_token() {
        store.save(1L, "refresh-token-abc", Duration.ofMinutes(5));

        Optional<String> found = store.find(1L);

        assertThat(found).contains("refresh-token-abc");
    }

    @Test
    void delete_removes_token() {
        store.save(2L, "refresh-token-xyz", Duration.ofMinutes(5));
        store.delete(2L);

        assertThat(store.find(2L)).isEmpty();
    }

    @Test
    void overwrite_replaces_previous_token() {
        store.save(3L, "old", Duration.ofMinutes(5));
        store.save(3L, "new", Duration.ofMinutes(5));

        assertThat(store.find(3L)).contains("new");
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `RefreshTokenStore` 인터페이스** — `RefreshTokenStore.java`

```java
package com.tutoring.global.security.refresh;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStore {

    void save(Long userId, String token, Duration ttl);

    Optional<String> find(Long userId);

    void delete(Long userId);
}
```

- [ ] **Step 4: `RedisRefreshTokenStore`** — `RedisRefreshTokenStore.java`

```java
package com.tutoring.global.security.refresh;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redis;

    @Override
    public void save(Long userId, String token, Duration ttl) {
        redis.opsForValue().set(key(userId), token, ttl);
    }

    @Override
    public Optional<String> find(Long userId) {
        return Optional.ofNullable(redis.opsForValue().get(key(userId)));
    }

    @Override
    public void delete(Long userId) {
        redis.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
```

- [ ] **Step 5: `RedisConfig`** — `RedisConfig.java`

`StringRedisTemplate`은 Spring Boot 자동 구성이 빈으로 등록한다. 별도 설정이 필요 없지만, 향후 직렬화 정책 변경을 위한 자리만 둔다.

```java
package com.tutoring.global.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    // StringRedisTemplate은 Spring Boot 자동 구성에 의해 빈으로 등록됨.
    // 추후 RedisTemplate<String, Object>가 필요해질 때 이 자리에 정의.
}
```

- [ ] **Step 6: 통합 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.security.refresh.RedisRefreshTokenStoreIntegrationTest"
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/tutoring/global/config/RedisConfig.java \
        src/main/java/com/tutoring/global/security/refresh \
        src/test/java/com/tutoring/global/security/refresh
git commit -m "feat(security): RefreshTokenStore 추상화 + Redis 구현"
```

---

## Task 11: `CustomUserPrincipal` + `JwtAuthenticationFilter`

**Files:**
- Create: `src/main/java/com/tutoring/global/security/principal/CustomUserPrincipal.java`
- Create: `src/main/java/com/tutoring/global/security/jwt/JwtAuthenticationFilter.java`
- Test:   `src/test/java/com/tutoring/global/security/jwt/JwtAuthenticationFilterTest.java`

- [ ] **Step 1: 실패 테스트** — `JwtAuthenticationFilterTest.java`

```java
package com.tutoring.global.security.jwt;

import com.tutoring.domain.user.entity.Role;
import com.tutoring.global.security.principal.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider provider = mock(JwtTokenProvider.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void valid_bearer_token_sets_authentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        when(provider.parse("valid-token"))
            .thenReturn(new JwtTokenProvider.Claims(42L, Role.USER));

        filter.doFilter(req, res, chain);

        UsernamePasswordAuthenticationToken auth =
            (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void missing_header_leaves_context_empty() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalid_token_leaves_context_empty_and_continues() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        when(provider.parse("bad"))
            .thenThrow(new JwtTokenProvider.InvalidTokenException("bad", new RuntimeException()));

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `CustomUserPrincipal`** — `CustomUserPrincipal.java`

```java
package com.tutoring.global.security.principal;

import com.tutoring.domain.user.entity.Role;

public record CustomUserPrincipal(Long userId, Role role) {}
```

- [ ] **Step 4: `JwtAuthenticationFilter`** — `JwtAuthenticationFilter.java`

```java
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
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.security.jwt.JwtAuthenticationFilterTest"
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/tutoring/global/security/principal \
        src/main/java/com/tutoring/global/security/jwt/JwtAuthenticationFilter.java \
        src/test/java/com/tutoring/global/security/jwt/JwtAuthenticationFilterTest.java
git commit -m "feat(security): JwtAuthenticationFilter + CustomUserPrincipal"
```

---

## Task 12: `GlobalExceptionHandler` + `JsonAuthEntryPoint` + `JsonAccessDeniedHandler`

**Files:**
- Create: `src/main/java/com/tutoring/global/error/GlobalExceptionHandler.java`
- Create: `src/main/java/com/tutoring/global/security/handler/JsonAuthEntryPoint.java`
- Create: `src/main/java/com/tutoring/global/security/handler/JsonAccessDeniedHandler.java`
- Test:   `src/test/java/com/tutoring/global/error/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: 실패 슬라이스 테스트** — `GlobalExceptionHandlerTest.java`

```java
package com.tutoring.global.error;

import com.tutoring.global.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void api_exception_maps_to_error_code_status_and_body() {
        ApiException ex = new ApiException(ErrorCode.USER_NOT_FOUND, "id=42");

        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(ex);

        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("id=42");
    }

    @Test
    void unhandled_exception_maps_to_internal_error() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnknown(new RuntimeException("boom"));

        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `GlobalExceptionHandler`** — `GlobalExceptionHandler.java`

```java
package com.tutoring.global.error;

import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.info("ApiException: {} - {}", ex.getErrorCode(), ex.getMessage());
        ErrorCode code = ex.getErrorCode();
        ErrorResponse error = new ErrorResponse(code.getCode(), ex.getMessage());
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(),
                                      fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
            .toList();
        ErrorResponse error = new ErrorResponse(
            ErrorCode.VALIDATION_FAILED.getCode(),
            ErrorCode.VALIDATION_FAILED.getMessage(),
            details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.fail(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.fail(ErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.fail(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }
}
```

- [ ] **Step 4: `JsonAuthEntryPoint`** — `JsonAuthEntryPoint.java`

```java
package com.tutoring.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                                ApiResponse.fail(ErrorCode.UNAUTHORIZED));
    }
}
```

- [ ] **Step 5: `JsonAccessDeniedHandler`** — `JsonAccessDeniedHandler.java`

```java
package com.tutoring.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                                ApiResponse.fail(ErrorCode.FORBIDDEN));
    }
}
```

- [ ] **Step 6: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.error.GlobalExceptionHandlerTest"
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/tutoring/global/error/GlobalExceptionHandler.java \
        src/main/java/com/tutoring/global/security/handler \
        src/test/java/com/tutoring/global/error/GlobalExceptionHandlerTest.java
git commit -m "feat(error): GlobalExceptionHandler + Security 핸들러 통일 응답"
```

---

## Task 13: `MdcLoggingFilter` + `logback-spring.xml`

**Files:**
- Create: `src/main/java/com/tutoring/global/logging/MdcLoggingFilter.java`
- Create: `src/main/resources/logback-spring.xml`
- Test:   `src/test/java/com/tutoring/global/logging/MdcLoggingFilterTest.java`

- [ ] **Step 1: 실패 테스트** — `MdcLoggingFilterTest.java`

```java
package com.tutoring.global.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MdcLoggingFilterTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();

    @Test
    void generates_trace_id_when_header_absent() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("X-Trace-Id")).isNotBlank();
    }

    @Test
    void reuses_inbound_trace_id_header() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Trace-Id", "external-123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("X-Trace-Id")).isEqualTo("external-123");
    }

    @Test
    void mdc_is_cleared_after_request() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(MDC.get("traceId")).isNull();
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `MdcLoggingFilter`** — `MdcLoggingFilter.java`

```java
package com.tutoring.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_KEY = "traceId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_TRACE_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_KEY);
        }
    }
}
```

- [ ] **Step 4: `logback-spring.xml`** — `src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <springProfile name="local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} %-5level [%X{traceId:-}] [%X{userId:-}] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
        <logger name="com.tutoring" level="DEBUG"/>
    </springProfile>

    <springProfile name="prod | test">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>userId</includeMdcKeyName>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>

</configuration>
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.logging.MdcLoggingFilterTest"
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/tutoring/global/logging \
        src/main/resources/logback-spring.xml \
        src/test/java/com/tutoring/global/logging
git commit -m "feat(logging): MdcLoggingFilter(traceId) + 프로필별 logback 설정"
```

---

## Task 14: OAuth2 Provider 추상화 (`OAuth2UserInfo` + Google + Kakao + Factory)

**Files:**
- Create: `src/main/java/com/tutoring/global/security/oauth2/OAuth2UserInfo.java`
- Create: `src/main/java/com/tutoring/global/security/oauth2/GoogleOAuth2UserInfo.java`
- Create: `src/main/java/com/tutoring/global/security/oauth2/KakaoOAuth2UserInfo.java`
- Create: `src/main/java/com/tutoring/global/security/oauth2/OAuth2UserInfoFactory.java`
- Test:   `src/test/java/com/tutoring/global/security/oauth2/OAuth2UserInfoFactoryTest.java`

- [ ] **Step 1: 실패 테스트** — `OAuth2UserInfoFactoryTest.java`

```java
package com.tutoring.global.security.oauth2;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2UserInfoFactoryTest {

    @Test
    void google_attributes_are_parsed() {
        Map<String, Object> attrs = Map.of(
            "sub", "google-sub-123",
            "email", "u@example.com",
            "name", "홍길동",
            "picture", "http://img/p.png"
        );
        OAuth2UserInfo info = OAuth2UserInfoFactory.from("google", attrs);

        assertThat(info.getProviderId()).isEqualTo("google-sub-123");
        assertThat(info.getEmail()).isEqualTo("u@example.com");
        assertThat(info.getName()).isEqualTo("홍길동");
        assertThat(info.getProfileImageUrl()).isEqualTo("http://img/p.png");
    }

    @Test
    void kakao_attributes_are_parsed_from_nested_structure() {
        Map<String, Object> attrs = Map.of(
            "id", 987654321L,
            "kakao_account", Map.of(
                "email", "k@example.com",
                "profile", Map.of(
                    "nickname", "김카카오",
                    "profile_image_url", "http://kakao/p.png"
                )
            )
        );
        OAuth2UserInfo info = OAuth2UserInfoFactory.from("kakao", attrs);

        assertThat(info.getProviderId()).isEqualTo("987654321");
        assertThat(info.getEmail()).isEqualTo("k@example.com");
        assertThat(info.getName()).isEqualTo("김카카오");
        assertThat(info.getProfileImageUrl()).isEqualTo("http://kakao/p.png");
    }

    @Test
    void unsupported_provider_throws() {
        assertThatThrownBy(() -> OAuth2UserInfoFactory.from("facebook", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `OAuth2UserInfo` 인터페이스** — `OAuth2UserInfo.java`

```java
package com.tutoring.global.security.oauth2;

public interface OAuth2UserInfo {

    String getProviderId();

    String getEmail();

    String getName();

    String getProfileImageUrl();
}
```

- [ ] **Step 4: `GoogleOAuth2UserInfo`** — `GoogleOAuth2UserInfo.java`

```java
package com.tutoring.global.security.oauth2;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    @Override public String getProviderId()      { return (String) attributes.get("sub"); }
    @Override public String getEmail()           { return (String) attributes.get("email"); }
    @Override public String getName()            { return (String) attributes.get("name"); }
    @Override public String getProfileImageUrl() { return (String) attributes.get("picture"); }
}
```

- [ ] **Step 5: `KakaoOAuth2UserInfo`** — `KakaoOAuth2UserInfo.java`

```java
package com.tutoring.global.security.oauth2;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account == null ? null : (String) account.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> profile = profile();
        return profile == null ? null : (String) profile.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> profile = profile();
        return profile == null ? null : (String) profile.get("profile_image_url");
    }

    private Map<String, Object> profile() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) return null;
        return (Map<String, Object>) account.get("profile");
    }
}
```

- [ ] **Step 6: `OAuth2UserInfoFactory`** — `OAuth2UserInfoFactory.java`

```java
package com.tutoring.global.security.oauth2;

import java.util.Map;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException(
                "지원하지 않는 OAuth2 provider: " + registrationId);
        };
    }
}
```

- [ ] **Step 7: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.security.oauth2.OAuth2UserInfoFactoryTest"
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/tutoring/global/security/oauth2 \
        src/test/java/com/tutoring/global/security/oauth2
git commit -m "feat(security): OAuth2UserInfo 추상화 + Google/Kakao 구현체"
```

---

## Task 15: `CustomOAuth2UserService` + `OAuth2LoginSuccessHandler`

**Files:**
- Create: `src/main/java/com/tutoring/global/security/oauth2/CustomOAuth2UserService.java`
- Create: `src/main/java/com/tutoring/global/security/oauth2/OAuth2LoginSuccessHandler.java`
- Create: `src/main/java/com/tutoring/domain/user/dto/TokenResponse.java`
- Test:   `src/test/java/com/tutoring/global/security/oauth2/CustomOAuth2UserServiceTest.java`

- [ ] **Step 1: `TokenResponse` DTO** — `TokenResponse.java`

```java
package com.tutoring.domain.user.dto;

public record TokenResponse(String accessToken, String refreshToken) {}
```

- [ ] **Step 2: 실패 테스트** — `CustomOAuth2UserServiceTest.java`

```java
package com.tutoring.global.security.oauth2;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomOAuth2UserServiceTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final CustomOAuth2UserService service = new CustomOAuth2UserService(repo);

    @Test
    void upsert_creates_when_user_not_found() {
        when(repo.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.upsert(AuthProvider.GOOGLE, "sub-1", "e@e.com", "name", "img");

        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        verify(repo).save(any(User.class));
    }

    @Test
    void upsert_updates_profile_when_user_exists() {
        User existing = UserFixture.googleUser();
        when(repo.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-sub-123"))
            .thenReturn(Optional.of(existing));

        User user = service.upsert(AuthProvider.GOOGLE, "google-sub-123",
                                   "user@example.com", "new-name", "new-img");

        assertThat(user.getName()).isEqualTo("new-name");
        assertThat(user.getProfileImageUrl()).isEqualTo("new-img");
        verify(repo, never()).save(any(User.class));
    }
}
```

- [ ] **Step 3: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 4: `CustomOAuth2UserService` 구현** — `CustomOAuth2UserService.java`

```java
package com.tutoring.global.security.oauth2;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.from(registrationId);
        OAuth2UserInfo info = OAuth2UserInfoFactory.from(registrationId, oauth2User.getAttributes());

        User user = upsert(provider, info.getProviderId(), info.getEmail(),
                           info.getName(), info.getProfileImageUrl());

        Map<String, Object> attributes = oauth2User.getAttributes();
        return new OAuth2UserAdapter(user, attributes);
    }

    @Transactional
    public User upsert(AuthProvider provider, String providerId,
                       String email, String name, String profileImageUrl) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
            .map(u -> { u.updateProfile(name, profileImageUrl); return u; })
            .orElseGet(() -> userRepository.save(
                User.create(provider, providerId, email, name, profileImageUrl)));
    }
}
```

- [ ] **Step 5: `OAuth2UserAdapter`** (별도 파일, 같은 패키지) — `src/main/java/com/tutoring/global/security/oauth2/OAuth2UserAdapter.java`

```java
package com.tutoring.global.security.oauth2;

import com.tutoring.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class OAuth2UserAdapter implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public OAuth2UserAdapter(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().getAuthority()));
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }
}
```

- [ ] **Step 6: `OAuth2LoginSuccessHandler`** — `OAuth2LoginSuccessHandler.java`

```java
package com.tutoring.global.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoring.domain.user.dto.TokenResponse;
import com.tutoring.domain.user.entity.User;
import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.security.jwt.JwtProperties;
import com.tutoring.global.security.jwt.JwtTokenProvider;
import com.tutoring.global.security.refresh.RefreshTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2UserAdapter principal = (OAuth2UserAdapter) authentication.getPrincipal();
        User user = principal.getUser();

        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken,
                               Duration.ofMillis(jwtProperties.refreshTokenValidityMs()));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                                ApiResponse.success(new TokenResponse(accessToken, refreshToken)));
    }
}
```

- [ ] **Step 7: 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.security.oauth2.CustomOAuth2UserServiceTest"
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/tutoring/global/security/oauth2 \
        src/main/java/com/tutoring/domain/user/dto/TokenResponse.java \
        src/test/java/com/tutoring/global/security/oauth2/CustomOAuth2UserServiceTest.java
git commit -m "feat(security): OAuth2 로그인 핸들러 + 토큰 발급"
```

---

## Task 16: `CorsConfig` + `SecurityConfig`

**Files:**
- Create: `src/main/java/com/tutoring/global/config/CorsConfig.java`
- Create: `src/main/java/com/tutoring/global/security/SecurityConfig.java`

**Note:** SecurityConfig는 다음 task의 `/api/v1/me` 통합 테스트로 검증한다.

- [ ] **Step 1: `CorsConfig`** — `CorsConfig.java`

```java
package com.tutoring.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {

        CorsConfiguration cfg = new CorsConfiguration();
        if (!allowedOrigins.isBlank()) {
            cfg.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("X-Trace-Id"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
```

- [ ] **Step 2: `SecurityConfig`** — `SecurityConfig.java`

```java
package com.tutoring.global.security;

import com.tutoring.global.security.handler.JsonAccessDeniedHandler;
import com.tutoring.global.security.handler.JsonAuthEntryPoint;
import com.tutoring.global.security.jwt.JwtAuthenticationFilter;
import com.tutoring.global.security.oauth2.CustomOAuth2UserService;
import com.tutoring.global.security.oauth2.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JsonAuthEntryPoint authEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService oauth2UserService;
    private final OAuth2LoginSuccessHandler oauth2SuccessHandler;
    private final UrlBasedCorsConfigurationSource corsSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(corsSource))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/oauth2/**",
                    "/login/**",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(u -> u.userService(oauth2UserService))
                .successHandler(oauth2SuccessHandler)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 3: 빌드 확인 (no test yet for this task)**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/tutoring/global/config/CorsConfig.java \
        src/main/java/com/tutoring/global/security/SecurityConfig.java
git commit -m "feat(security): SecurityConfig 통합 (JWT/OAuth2/CORS/예외 응답)"
```

---

## Task 17: `UserService` + `UserController` (`/api/v1/me`) + 통합 테스트

**Files:**
- Create: `src/main/java/com/tutoring/domain/user/service/UserService.java`
- Create: `src/main/java/com/tutoring/domain/user/service/UserServiceImpl.java`
- Create: `src/main/java/com/tutoring/domain/user/dto/UserResponse.java`
- Create: `src/main/java/com/tutoring/domain/user/controller/UserController.java`
- Test:   `src/test/java/com/tutoring/domain/user/service/UserServiceImplTest.java`
- Test:   `src/test/java/com/tutoring/domain/user/controller/UserControllerIntegrationTest.java`

- [ ] **Step 1: 실패 단위 테스트** — `UserServiceImplTest.java`

```java
package com.tutoring.domain.user.service;

import com.tutoring.domain.user.dto.UserResponse;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.global.error.ApiException;
import com.tutoring.global.error.ErrorCode;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final UserService service = new UserServiceImpl(repo);

    @Test
    void getMe_returns_response_for_existing_user() {
        User user = UserFixture.googleUser();
        when(repo.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = service.getMe(1L);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
    }

    @Test
    void getMe_throws_USER_NOT_FOUND_when_missing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMe(99L))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `UserResponse` DTO** — `UserResponse.java`

```java
package com.tutoring.domain.user.dto;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.Role;
import com.tutoring.domain.user.entity.User;

public record UserResponse(
    Long id,
    AuthProvider provider,
    String email,
    String name,
    String profileImageUrl,
    Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getProvider(),
            user.getEmail(),
            user.getName(),
            user.getProfileImageUrl(),
            user.getRole()
        );
    }
}
```

- [ ] **Step 4: `UserService` 인터페이스** — `UserService.java`

```java
package com.tutoring.domain.user.service;

import com.tutoring.domain.user.dto.UserResponse;

public interface UserService {

    UserResponse getMe(Long userId);
}
```

- [ ] **Step 5: `UserServiceImpl`** — `UserServiceImpl.java`

```java
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
```

- [ ] **Step 6: `UserController`** — `UserController.java`

```java
package com.tutoring.domain.user.controller;

import com.tutoring.domain.user.dto.UserResponse;
import com.tutoring.domain.user.service.UserService;
import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "현재 로그인한 사용자 정보 조회")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(userService.getMe(principal.userId()));
    }
}
```

- [ ] **Step 7: 단위 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.domain.user.service.UserServiceImplTest"
```

- [ ] **Step 8: 통합 테스트 작성** — `UserControllerIntegrationTest.java`

```java
package com.tutoring.domain.user.controller;

import com.tutoring.IntegrationTestBase;
import com.tutoring.domain.user.entity.Role;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.global.security.jwt.JwtTokenProvider;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider tokenProvider;

    @Test
    void me_returns_current_user_when_authorized() throws Exception {
        User saved = userRepository.save(UserFixture.googleUser());
        String token = tokenProvider.createAccessToken(saved.getId(), Role.USER);

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(saved.getId()))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void me_returns_401_when_no_token() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_returns_401_when_invalid_token() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
```

- [ ] **Step 9: 통합 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.domain.user.controller.UserControllerIntegrationTest"
```

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/tutoring/domain/user/dto/UserResponse.java \
        src/main/java/com/tutoring/domain/user/service \
        src/main/java/com/tutoring/domain/user/controller/UserController.java \
        src/test/java/com/tutoring/domain/user
git commit -m "feat(user): GET /api/v1/users/me - 수직 슬라이스 완성"
```

---

## Task 18: `AuthController` (refresh + logout)

**Files:**
- Create: `src/main/java/com/tutoring/domain/user/dto/RefreshTokenRequest.java`
- Create: `src/main/java/com/tutoring/domain/user/dto/LogoutRequest.java`
- Create: `src/main/java/com/tutoring/domain/user/controller/AuthController.java`
- Test:   `src/test/java/com/tutoring/domain/user/controller/AuthControllerIntegrationTest.java`

- [ ] **Step 1: 실패 통합 테스트** — `AuthControllerIntegrationTest.java`

```java
package com.tutoring.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutoring.IntegrationTestBase;
import com.tutoring.domain.user.dto.LogoutRequest;
import com.tutoring.domain.user.dto.RefreshTokenRequest;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.global.security.jwt.JwtProperties;
import com.tutoring.global.security.jwt.JwtTokenProvider;
import com.tutoring.global.security.refresh.RefreshTokenStore;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired RefreshTokenStore refreshTokenStore;
    @Autowired JwtProperties jwtProperties;

    @Test
    void refresh_issues_new_tokens_when_refresh_valid() throws Exception {
        User saved = userRepository.save(UserFixture.googleUser());
        String refreshToken = tokenProvider.createRefreshToken(saved.getId());
        refreshTokenStore.save(saved.getId(), refreshToken,
                               Duration.ofMillis(jwtProperties.refreshTokenValidityMs()));

        String body = objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_returns_401_when_token_not_in_store() throws Exception {
        User saved = userRepository.save(UserFixture.googleUser());
        String refreshToken = tokenProvider.createRefreshToken(saved.getId());
        // 일부러 store에 저장하지 않음

        String body = objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_NOT_FOUND"));
    }

    @Test
    void logout_deletes_refresh_token() throws Exception {
        User saved = userRepository.save(UserFixture.googleUser());
        String refreshToken = tokenProvider.createRefreshToken(saved.getId());
        refreshTokenStore.save(saved.getId(), refreshToken, Duration.ofMinutes(5));

        String body = objectMapper.writeValueAsString(new LogoutRequest(refreshToken));

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertThat(refreshTokenStore.find(saved.getId())).isEmpty();
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: DTO들** — `RefreshTokenRequest.java`, `LogoutRequest.java`

```java
package com.tutoring.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {}
```

```java
package com.tutoring.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {}
```

- [ ] **Step 4: `AuthController`** — `AuthController.java`

```java
package com.tutoring.domain.user.controller;

import com.tutoring.domain.user.dto.LogoutRequest;
import com.tutoring.domain.user.dto.RefreshTokenRequest;
import com.tutoring.domain.user.dto.TokenResponse;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.error.ApiException;
import com.tutoring.global.error.ErrorCode;
import com.tutoring.global.security.jwt.JwtProperties;
import com.tutoring.global.security.jwt.JwtTokenProvider;
import com.tutoring.global.security.refresh.RefreshTokenStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @PostMapping("/refresh")
    @Operation(summary = "Refresh 토큰으로 새 Access/Refresh 발급 (rotation)")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        JwtTokenProvider.Claims claims;
        try {
            claims = tokenProvider.parse(request.refreshToken());
        } catch (JwtTokenProvider.ExpiredTokenException e) {
            throw new ApiException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtTokenProvider.InvalidTokenException e) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        String stored = refreshTokenStore.find(claims.userId())
            .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        if (!stored.equals(request.refreshToken())) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        User user = userRepository.findById(claims.userId())
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String newAccess  = tokenProvider.createAccessToken(user.getId(), user.getRole());
        String newRefresh = tokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), newRefresh,
                               Duration.ofMillis(jwtProperties.refreshTokenValidityMs()));

        return ApiResponse.success(new TokenResponse(newAccess, newRefresh));
    }

    @PostMapping("/logout")
    @Operation(summary = "Refresh 토큰 무효화 (로그아웃)")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            JwtTokenProvider.Claims claims = tokenProvider.parse(request.refreshToken());
            refreshTokenStore.delete(claims.userId());
        } catch (JwtTokenProvider.InvalidTokenException | JwtTokenProvider.ExpiredTokenException e) {
            // 의도적 무시 — 토큰이 무효라도 로그아웃은 멱등
        }
        return ApiResponse.success();
    }
}
```

- [ ] **Step 5: 통합 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.domain.user.controller.AuthControllerIntegrationTest"
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/tutoring/domain/user/dto/RefreshTokenRequest.java \
        src/main/java/com/tutoring/domain/user/dto/LogoutRequest.java \
        src/main/java/com/tutoring/domain/user/controller/AuthController.java \
        src/test/java/com/tutoring/domain/user/controller/AuthControllerIntegrationTest.java
git commit -m "feat(auth): POST /api/v1/auth/refresh + /logout (rotation 포함)"
```

---

## Task 19: `SwaggerConfig`

**Files:**
- Create: `src/main/java/com/tutoring/global/config/SwaggerConfig.java`
- Test:   `src/test/java/com/tutoring/global/config/SwaggerEndpointIntegrationTest.java`

- [ ] **Step 1: 실패 통합 테스트** — `SwaggerEndpointIntegrationTest.java`

```java
package com.tutoring.global.config;

import com.tutoring.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SwaggerEndpointIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;

    @Test
    void openapi_spec_is_publicly_accessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Tutoring API"))
            .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt").exists());
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

```bash
./gradlew compileTestJava
```

- [ ] **Step 3: `SwaggerConfig` 구현** — `SwaggerConfig.java`

```java
package com.tutoring.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SCHEME_NAME = "bearer-jwt";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Tutoring API")
                .description("과외 워크스페이스 백엔드 API")
                .version("v1"))
            .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
            .components(new Components().addSecuritySchemes(SCHEME_NAME,
                new SecurityScheme()
                    .name(SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

- [ ] **Step 4: 통합 테스트 통과 확인**

```bash
./gradlew test --tests "com.tutoring.global.config.SwaggerEndpointIntegrationTest"
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tutoring/global/config/SwaggerConfig.java \
        src/test/java/com/tutoring/global/config/SwaggerEndpointIntegrationTest.java
git commit -m "feat(swagger): OpenAPI 메타데이터 + Bearer JWT SecurityScheme"
```

---

## Task 20: 로컬 docker-compose.yml + 셋업 가이드

**Files:**
- Create: `docker-compose.yml` (프로젝트 루트)
- Create: `docs/superpowers/local-setup.md`

- [ ] **Step 1: `docker-compose.yml`** — `C:\woong\GITHUB\backend\docker-compose.yml`

```yaml
version: '3.9'
services:
  mysql:
    image: mysql:8.0
    container_name: tutoring-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: tutoring
      MYSQL_USER: tutoring
      MYSQL_PASSWORD: tutoring
      TZ: Asia/Seoul
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    container_name: tutoring-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      retries: 10

volumes:
  mysql_data:
```

- [ ] **Step 2: `docs/superpowers/local-setup.md`**

```markdown
# 로컬 개발 환경 셋업

## 사전 요구사항

- Java 21 (Temurin 권장)
- Docker Desktop (Mac/Windows) 또는 Docker Engine + Compose v2 (Linux)
- IntelliJ IDEA (or VS Code with Java extensions)

## 1단계 — 의존성 컨테이너 기동

```bash
docker compose up -d
docker compose ps    # mysql / redis healthy 확인
```

## 2단계 — `application-local.yml` 생성

```bash
cp src/main/resources/application-local.yml.template \
   src/main/resources/application-local.yml
```

`application-local.yml`을 열어 다음을 채운다:
- `google.client-id` / `google.client-secret` — Google Cloud Console에서 OAuth 2.0 Client ID 발급
- `kakao.client-id` / `kakao.client-secret` — Kakao Developers에서 앱 등록 후 발급

**Redirect URI 등록:**
- Google: `http://localhost:8080/login/oauth2/code/google`
- Kakao:  `http://localhost:8080/login/oauth2/code/kakao`

## 3단계 — Testcontainers 재사용 활성화 (선택, 추천)

```bash
# ~/.testcontainers.properties
testcontainers.reuse.enable=true
```

→ 테스트 실행 사이에 컨테이너가 살아남아 두 번째 이후 실행이 빨라진다.

## 4단계 — IDE에서 실행

IntelliJ:
1. Run → Edit Configurations
2. Spring Boot — Active profiles: `local`
3. Run

또는 CLI:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 5단계 — 동작 확인

- `http://localhost:8080/actuator/health` → 200 + `{"status":"UP"}`
- `http://localhost:8080/swagger-ui.html` → API 문서 UI
- `http://localhost:8080/oauth2/authorization/google` → Google 로그인 페이지
- 로그인 성공 시 응답 body의 `accessToken`을 가지고 Swagger UI 우상단 Authorize에 입력

## 테스트 실행

```bash
./gradlew test
./gradlew jacocoTestReport
```

커버리지 리포트: `build/reports/jacoco/test/html/index.html`

## 문제 해결

| 증상 | 해결 |
|------|------|
| `Flyway: Schema validate failed` | `docker compose down -v` 로 볼륨까지 삭제 후 재시작 |
| `Connection refused: localhost:6379` | `docker compose ps` 로 redis 상태 확인 |
| OAuth2 콜백에서 `redirect_uri_mismatch` | Provider Console의 redirect URI와 `application-local.yml`의 URI가 정확히 일치하는지 확인 |
```

- [ ] **Step 3: 로컬에서 전체 빌드 + 테스트 실행 (검증)**

```bash
docker compose up -d
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`. 모든 통합 테스트가 Docker MySQL/Redis와 함께 통과.

- [ ] **Step 4: 부팅 검증 — IDE 또는 CLI**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

별도 터미널에서:

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/v3/api-docs
```

Expected: 둘 다 200. health → `{"status":"UP"}`, api-docs → JSON OpenAPI spec.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml docs/superpowers/local-setup.md
git commit -m "feat(local): docker-compose 의존성 + 로컬 셋업 가이드"
```

---

## Task 21: 최종 검증 — 전체 테스트 + 빌드

**Goal:** Plan A 완료 게이트. 이 단계가 통과해야 Plan B(배포 인프라)로 넘어갈 수 있다.

- [ ] **Step 1: 전체 클린 빌드 + 테스트**

```bash
./gradlew clean build
```

Expected:
- 컴파일 성공
- 모든 단위 테스트 통과
- 모든 통합 테스트 통과 (Testcontainers MySQL/Redis 자동 기동)
- BUILD SUCCESSFUL

- [ ] **Step 2: 커버리지 리포트 확인**

```bash
./gradlew jacocoTestReport
```

Expected: `build/reports/jacoco/test/html/index.html` 생성. 라인 커버리지 60% 이상이면 좋음 (강제 아님).

- [ ] **Step 3: 부팅 + 수동 smoke test**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

브라우저에서:
1. `http://localhost:8080/swagger-ui.html` 열림 + 두 API (`User`, `Auth`) 표시됨
2. `http://localhost:8080/oauth2/authorization/google` → Google 로그인 → 성공 응답 JSON에 `accessToken` + `refreshToken` 포함
3. 받은 access token으로 Swagger UI Authorize → GET /api/v1/users/me → 200 + 본인 정보
4. POST /api/v1/auth/refresh → 새 토큰 발급
5. POST /api/v1/auth/logout → 200 + Redis에서 토큰 제거됨

- [ ] **Step 4: 최종 commit (작업 트리에 변경 없으면 skip)**

```bash
git status
# clean working tree 확인
git log --oneline -25  # Plan A의 커밋 흐름 가시화
```

**완료 기준:**
- 모든 commit이 main 브랜치에 누적됨
- `./gradlew clean build` 무결성
- 수동 smoke test 5단계 통과

---

## 다음 단계

Plan A 완료 후 **Plan B (배포 인프라)** 로 진입:
- Dockerfile (multi-stage)
- `docker-compose.prod.yml` (EC2용)
- NGINX 설정 (SSL termination + Swagger Basic Auth)
- GitHub Actions `ci.yml` + `deploy.yml`
- EC2 사전 셋업 가이드

Plan B는 별도 spec/plan 문서로 작성한다.
