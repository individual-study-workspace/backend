# Backend Foundation Design

- **Date:** 2026-05-17
- **Project:** privatelesson (com.tutoring) — 과외 워크스페이스 백엔드 API 서버
- **Stack:** Java 21, Spring Boot 3.5.11, MySQL 8.0, Redis 7, JPA(Hibernate), Flyway
- **Status:** Approved (awaiting implementation plan)

---

## 0. Overview

### 0.1 목표

엔터프라이즈급 운영을 견딜 수 있는 백엔드 API 서버의 **기반 골격(Foundation)** 을 정의한다. 이 spec은 도메인 기능을 다루지 않는다. 대신 모든 후속 도메인 spec이 올라탈 공통 레일을 만든다:

- 패키지 구조와 의존성 정책
- OAuth2 → 자체 JWT 인증 파이프라인
- 영속성 / 마이그레이션 / 캐시
- API 응답·에러·페이지네이션 규약
- 테스트 전략
- 로컬 개발 환경과 관측
- CI/CD 파이프라인
- 설정·시크릿 관리

### 0.2 범위 — 포함 vs 미포함

| 포함 (In Scope) | 미포함 (Out of Scope) |
|----|----|
| Spring Boot 프로젝트 구조 / `build.gradle` 정비 | 도메인 모델 (튜터/학생/수업/스케줄/결제 등) |
| OAuth2 (Google + Kakao) → 자체 JWT 발급 | 회원 관리 비즈니스 로직 (가입 후 프로필 편집 등) |
| Refresh Token (Redis, rotation) | 알림 / 이메일 발송 / 외부 API 연동 |
| Flyway 마이그레이션 베이스 + `users` 테이블 | 추가 도메인 테이블 |
| `User` 엔티티 1개 + `/api/v1/me` 엔드포인트 (수직 슬라이스 증명용) | 그 외 모든 도메인 엔드포인트 |
| 전역 예외 처리 + 표준 응답 포맷 | 도메인별 에러 코드 카탈로그 |
| Testcontainers 기반 TDD 셋업 | 부하 / E2E / 카오스 테스트 |
| GitHub Actions → ECR → EC2 (SSH) 배포 | ALB / Auto Scaling / Blue-Green |
| NGINX SSL termination + Swagger Basic Auth | CDN / WAF |
| Docker Compose 로컬 환경 | RDS / ElastiCache (초기 단계 EC2 컨테이너로 시작) |
| 구조화 JSON 로깅 + Actuator(`health`,`info`) | 외부 모니터링 (Prometheus/Grafana/Datadog) |

### 0.3 채택한 접근법

**Approach B — Skeleton + Auth 수직 슬라이스**

> 빈 골격만 만드는 대신, 인증/영속/캐시/예외/Swagger/테스트의 전 파이프라인이 end-to-end로 작동함을 증명하는 **단 하나의 수직 슬라이스(`/api/v1/me`)** 를 함께 구현한다. 이 슬라이스가 후속 도메인 spec들의 템플릿이 된다.

### 0.4 가이드 원칙

1. **SOLID, 특히 SRP와 DIP** — 횡단 관심사는 별도 Config로 분리. 서비스는 인터페이스 + 구현. 외부 의존(JWT 라이브러리, Redis 등)은 인터페이스 뒤로 추상화.
2. **TDD red-green-refactor** — 수직 슬라이스를 실제로 TDD 사이클로 작성하여 표준 사이클을 코드로 남긴다.
3. **YAGNI** — Foundation에서는 "지금 필요한 것만". 멀티모듈/메트릭/자동 롤백 등은 신호가 나타날 때 후속 spec에서 추가.
4. **Layered + Domain packaging** — `global/` (횡단 관심사) + `domain/<name>/` (도메인 슬라이스). 도메인은 `global`에만 의존, 도메인 간 의존 금지.

---

## 1. 프로젝트 구조 & 빌드

### 1.1 패키지 레이아웃

```
com.tutoring
├── TutoringApplication.java
├── global/
│   ├── config/
│   │   ├── SwaggerConfig.java
│   │   ├── JpaAuditingConfig.java          ← @EnableJpaAuditing 이동 위치
│   │   ├── RedisConfig.java
│   │   └── CorsConfig.java
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── jwt/
│   │   │   ├── JwtTokenProvider.java       (interface)
│   │   │   ├── JjwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── JwtProperties.java
│   │   ├── oauth2/
│   │   │   ├── OAuth2UserInfo.java         (interface)
│   │   │   ├── GoogleOAuth2UserInfo.java
│   │   │   ├── KakaoOAuth2UserInfo.java
│   │   │   ├── OAuth2UserInfoFactory.java
│   │   │   ├── CustomOAuth2UserService.java
│   │   │   └── OAuth2LoginSuccessHandler.java
│   │   ├── principal/
│   │   │   └── CustomUserPrincipal.java
│   │   ├── handler/
│   │   │   ├── JsonAuthEntryPoint.java     (401 → ApiResponse 포맷)
│   │   │   └── JsonAccessDeniedHandler.java (403 → ApiResponse 포맷)
│   │   └── refresh/
│   │       ├── RefreshTokenStore.java      (interface)
│   │       └── RedisRefreshTokenStore.java
│   ├── error/
│   │   ├── GlobalExceptionHandler.java     (@RestControllerAdvice)
│   │   ├── ErrorCode.java                  (enum)
│   │   ├── ApiException.java
│   │   └── FieldError.java                 (record)
│   ├── common/
│   │   ├── ApiResponse.java
│   │   ├── ErrorResponse.java
│   │   ├── Pagination.java                 (record)
│   │   └── PageResponse.java               (record)
│   └── logging/
│       └── MdcLoggingFilter.java
└── domain/
    └── user/
        ├── controller/UserController.java        (GET /api/v1/me)
        ├── service/UserService.java              (interface)
        ├── service/UserServiceImpl.java
        ├── repository/UserRepository.java
        ├── entity/User.java
        ├── entity/AuthProvider.java              (enum: GOOGLE, KAKAO)
        ├── entity/Role.java                      (enum: USER, ADMIN)
        ├── entity/BaseEntity.java                (audit fields, deletedAt)
        └── dto/UserResponse.java
```

**의존 규칙:**
- `domain/` → `global/` 단방향
- 같은 도메인 내부: `controller` → `service` → `repository`
- 도메인 간 의존 금지 (필요해지면 application service 또는 domain event로 분리 — 후속 spec)

### 1.2 build.gradle 변경

**제거:**
- `spring-boot-starter-data-jdbc` (JPA만 사용)
- `spring-boot-starter-web-services` (SOAP용, 불필요)

**추가:**
- `spring-boot-starter-web` (REST API 필수, 현재 빠져있음)
- `spring-boot-starter-data-redis`
- `spring-boot-starter-validation`
- `org.flywaydb:flyway-core`, `org.flywaydb:flyway-mysql`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.x`
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- `net.logstash.logback:logstash-logback-encoder`
- 테스트: `org.testcontainers:junit-jupiter`, `org.testcontainers:mysql`
- 빌드: JaCoCo 플러그인 (측정만, 임계값 강제 X)

**유지:**
- `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, `lombok`, `mysql-connector-j`, `spring-boot-devtools`, `spring-security-test`

---

## 2. 보안 아키텍처

### 2.1 인증 흐름

```
[Client]
   │ 1. GET /oauth2/authorization/{google|kakao}
   ▼
[Spring Security OAuth2 Client]
   │ 2. Redirect → IdP 로그인 페이지
   │ 3. User 동의 → callback /login/oauth2/code/{provider}
   ▼
[CustomOAuth2UserService → OAuth2LoginSuccessHandler]
   │ 4. OAuth2UserInfoFactory.from(provider) → 도메인 User upsert (by provider + providerId)
   │ 5. Access Token (15분) + Refresh Token (14일) 발급
   │ 6. Refresh Token → Redis (refresh:{userId}, TTL=14일)
   │ 7. JSON body 응답:
   │       { "accessToken": "...", "refreshToken": "..." }
   ▼
[Client] 이후 요청 → Authorization: Bearer <access_token>
   ▼
[JwtAuthenticationFilter] (OncePerRequestFilter)
   │ 토큰 검증 → SecurityContext에 Authentication 주입
   │ MDC.put("userId", ...)
   ▼
[Controller, @AuthenticationPrincipal CustomUserPrincipal]
```

### 2.2 토큰 정책

| 항목 | 값 |
|------|----|
| Access Token TTL | 15분 |
| Refresh Token TTL | 14일 |
| Refresh Token 저장소 | Redis (key: `refresh:{userId}`) — 멀티 디바이스는 후속 spec |
| Token 전달 | **모든 토큰을 JSON body로** (Flutter native/web 호환) |
| Token 회전 | refresh 호출 시 새 Access + 새 Refresh 발급, 이전 Refresh 즉시 무효 |
| 로그아웃 | 클라가 Refresh Token을 body로 전송 → Redis에서 삭제 |

### 2.3 핵심 추상 (DIP)

| 인터페이스 | 책임 | 초기 구현 |
|----|----|----|
| `JwtTokenProvider` | 토큰 발급·검증·파싱 | `JjwtTokenProvider` |
| `RefreshTokenStore` | refresh token 저장·조회·삭제 | `RedisRefreshTokenStore` |
| `OAuth2UserInfo` | provider별 사용자 정보 추상화 | `GoogleOAuth2UserInfo`, `KakaoOAuth2UserInfo` |
| `OAuth2UserInfoFactory` | provider 이름 → UserInfo 선택 | static factory |

**확장 가이드 (OCP):** Naver/Apple 추가 시 `OAuth2UserInfo` 구현체 + Factory 매핑만 추가. 기존 코드 수정 없음.

### 2.4 SecurityConfig 정책

| 항목 | 정책 |
|------|------|
| 세션 | `STATELESS` |
| CSRF | 비활성화 (Authorization 헤더 + body 사용, 쿠키 미사용) |
| CORS | `CorsConfig`로 분리, `app.cors.allowed-origins` 주입 |
| 인증 제외 경로 | `/oauth2/**`, `/login/**`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health` |
| 인증 실패 (401) | `JsonAuthEntryPoint` → `ApiResponse.fail(UNAUTHORIZED)` |
| 인가 실패 (403) | `JsonAccessDeniedHandler` → `ApiResponse.fail(FORBIDDEN)` |

### 2.5 Role 모델

- `Role` enum: `USER`, `ADMIN`
- `ROLE_` prefix는 `Role.getAuthority()` 메서드 내부에서 처리 (호출자는 모름)
- 확장(`TUTOR`, `STUDENT` 등)은 후속 도메인 spec에서

---

## 3. 영속성 & 마이그레이션

### 3.1 BaseEntity

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate    @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate                          private Instant updatedAt;
    @Column                                    private Instant deletedAt;   // soft delete

    public boolean isDeleted() { return deletedAt != null; }
    public void markDeleted()  { this.deletedAt = Instant.now(); }
}
```

### 3.2 User 엔티티

```java
@Entity @Table(name = "users",
       uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@SQLRestriction("deleted_at IS NULL")           // ← 기본 조회에서 자동 필터
public class User extends BaseEntity {
    @Id @GeneratedValue(strategy = IDENTITY) private Long id;

    @Enumerated(STRING) @Column(nullable = false, length = 20)
    private AuthProvider provider;              // GOOGLE, KAKAO

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(nullable = false, length = 255)    private String email;
    @Column(nullable = false, length = 50)     private String name;
    @Column(length = 500)                      private String profileImageUrl;

    @Enumerated(STRING) @Column(nullable = false, length = 20)
    private Role role;

    // 정적 팩토리 + 도메인 메서드, setter는 금지
}
```

**제약:**
- `UNIQUE (provider, provider_id)`
- email은 NOT NULL이지만 unique 아님 (Google과 Kakao가 같은 email이라도 다른 계정)
- soft delete는 `deletedAt` timestamp로 관리 (null = active)

### 3.3 Flyway 정책

- 위치: `src/main/resources/db/migration/`
- 네이밍: `V{YYYYMMDDHHmm}__{description}.sql` (시간 기반, 다중 개발자 순서 충돌 최소화)
- baseline: `V202605170001__create_users.sql`
- JPA `ddl-auto`: 운영/테스트 `validate`, CI 빌드 `none`. **`update`/`create`는 절대 금지**
- Testcontainers 환경도 Flyway가 동일하게 적용 → 운영과 동일 스키마 보장

### 3.4 Redis (Refresh Token Store)

- 키: `refresh:{userId}` → 값: `refreshToken`
- TTL: 14일 (Spring Data Redis `expire` 사용)
- `StringRedisTemplate` 사용 (단순 KV이므로 `RedisRepository` 미사용)
- 멀티 디바이스 지원은 후속 spec (`refresh:{userId}:{deviceId}` 등으로 확장)

---

## 4. API 규약

### 4.1 응답 포맷 (필수)

**공통 래퍼:**

```java
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> success()       { return success(null); }   // ★ 추가: void 응답용
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) { ... }
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) { ... }
}

public class ErrorResponse {
    private final String code;
    private final String message;
    private final List<FieldError> details;     // ★ 추가: null 가능, validation 에러일 때만 채움
}

public record FieldError(String field, String reason) {}                 // ★ 신규

public record Pagination(boolean hasNext, String nextCursor) {}
public record PageResponse<T>(List<T> content, Pagination pagination) {} // ★ 신규
```

> ★ 표시: 사용자가 미리 정의한 원형 대비 Foundation에서 추가/확장하는 항목.

**HTTP status code는 그대로 의미 살림** — 클라이언트는 1차로 status, 2차로 `error.code`로 분기.

**성공 예:**
```http
HTTP 200
Content-Type: application/json
X-Trace-Id: 7e1d...

{ "success": true, "data": { "id": 1, "email": "..." }, "error": null }
```

**실패 예 (validation):**
```http
HTTP 400
Content-Type: application/json
X-Trace-Id: 7e1d...

{
  "success": false, "data": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "입력값이 유효하지 않습니다",
    "details": [
      { "field": "email", "reason": "올바른 이메일 형식이 아닙니다" }
    ]
  }
}
```

### 4.2 ErrorCode 카탈로그 (초기)

| 그룹 | 코드 | HTTP |
|------|------|------|
| Global | `INTERNAL_ERROR` | 500 |
| Global | `METHOD_NOT_ALLOWED` | 405 |
| Global | `NOT_FOUND` | 404 |
| Validation | `VALIDATION_FAILED` | 400 |
| Validation | `INVALID_INPUT` | 400 |
| Auth | `UNAUTHORIZED` | 401 |
| Auth | `INVALID_TOKEN` | 401 |
| Auth | `EXPIRED_TOKEN` | 401 |
| Auth | `REFRESH_TOKEN_NOT_FOUND` | 401 |
| Auth | `FORBIDDEN` | 403 |
| User | `USER_NOT_FOUND` | 404 |
| User | `USER_DELETED` | 404 |

도메인별 코드는 해당 도메인 spec에서 추가.

### 4.3 traceId (X-Trace-Id)

- 매 요청마다 `MdcLoggingFilter`가 traceId 발급/주입
- `X-Trace-Id` 응답 헤더로 노출 (body 변경 없음)
- 모든 JSON 로그에 자동 포함 (MDC)
- 사용자 신고 시 traceId만으로 로그 grep 가능

### 4.4 Controller 베이스 패턴

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User")
public class UserController {
    private final UserService userService;     // 인터페이스 (DIP)

    @GetMapping("/me")
    @Operation(summary = "현재 로그인한 사용자 정보 조회")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(userService.getMe(principal.userId()));
    }
}
```

- 컨트롤러는 얇게: 입출력 변환 + 인증 정보 추출만
- 비즈니스 로직은 service
- 응답 타입은 **항상 `ApiResponse<T>`** (Swagger 스펙 일관성)

### 4.5 Validation

- DTO에 `jakarta.validation` 어노테이션 (`@Email`, `@NotBlank`, `@Size`)
- Controller 메서드 파라미터에 `@Valid`
- 실패 → `MethodArgumentNotValidException` → `GlobalExceptionHandler` → `VALIDATION_FAILED` + `FieldError[]`

### 4.6 Swagger UI

- 경로: `/swagger-ui.html`, 스펙 `/v3/api-docs`
- `SecurityScheme`으로 Bearer Token 직접 입력 가능 (`OpenAPIDefinition`)
- **운영(`prod`) 프로필에서도 노출**, 단 **NGINX Basic Auth로 보호** (섹션 7.6)
- 도메인별 자동 그룹화 (`User`, `Auth`)

### 4.7 미해결 사항 (TODO — 후속 spec)

> **Live list 갱신 전략:** Cursor-based pagination(`Pagination` record)은 deep page 성능은 해결하지만, 무한 스크롤 중 신규 항목 등장 시 사용자 가시성 문제가 남는다. 첫 list 엔드포인트가 정의되는 도메인 spec에서 결정한다. 후보: **composite cursor `(createdAt, id)` + top sentinel banner**.

---

## 5. 테스트 전략

### 5.1 계층별 분포 (목표)

| 계층 | 도구 | 대상 | 비율 |
|------|------|------|------|
| Unit | JUnit5 + Mockito | 도메인 로직, 서비스 (mock repo/store) | ~70% |
| Slice | `@WebMvcTest`, `@DataJpaTest` | Controller / Repository | ~20% |
| Integration | `@SpringBootTest` + Testcontainers | OAuth2→JWT→Redis 전 파이프라인 | ~10% |

### 5.2 IntegrationTestBase

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Container static MySQLContainer<?> mysql =
        new MySQLContainer<>("mysql:8.0").withReuse(true);

    @Container static GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.data.redis.host",     redis::getHost);
        r.add("spring.data.redis.port",     () -> redis.getMappedPort(6379));
    }
}
```

- `.withReuse(true)` + 개발자 PC의 `~/.testcontainers.properties`에 `testcontainers.reuse.enable=true` 설정 가이드
- Flyway가 각 실행 시 동일 마이그레이션 적용

### 5.3 수직 슬라이스 TDD 사이클 (표준)

후속 도메인 spec들이 따라야 할 표준 사이클:

```
1. RED:      Service 메서드 테스트 (mock 의존성) → 컴파일 실패
2. GREEN:    최소 구현 → 통과
3. REFACTOR: 인터페이스 추출, 의존성 정리

4. RED:      Controller MockMvc 테스트
5. GREEN:    컨트롤러 구현
6. REFACTOR

7. RED:      IntegrationTest (실제 흐름) — 인증 모킹/JWT 발급/엔드포인트 호출 → 200
8. GREEN:    SecurityConfig + Filter + Handler 연결
9. REFACTOR
```

### 5.4 보안 테스트

- `spring-security-test`의 `SecurityMockMvcRequestPostProcessors.jwt()` 활용
- 자체 `@WithMockJwtUser` 어노테이션 정의 가능 (보일러플레이트 절감)
- **인증 없는 보호 엔드포인트 호출 → 401 검증 케이스 필수**

### 5.5 Test Fixture

```java
public final class UserFixture {
    public static User googleUser() {
        return User.create(AuthProvider.GOOGLE, "sub-123",
                           "user@example.com", "홍길동", null, Role.USER);
    }
}
```

→ 보일러플레이트 절감 + SRP (fixture 단일 출처).

### 5.6 커버리지

- JaCoCo 측정만 활성화, **임계값 강제 X** (Foundation 단계)
- 도메인 spec 진행 중 자연스럽게 라인 60%+ 유지가 목표
- 100% 커버리지는 비목표 — 의미 있는 테스트가 우선

---

## 6. 로컬 개발 환경 & 관측

### 6.1 환경별 실행 모드

| 환경 | Spring Boot | MySQL/Redis | NGINX |
|------|------------|-------------|-------|
| **로컬 (개발자 PC)** | IDE 실행 + devtools hot reload | `docker-compose.yml` (개발용) | 없음 (직접 `:8080` 접속) |
| **테스트 (CI/로컬)** | JVM in-process | Testcontainers 자동 기동 | 없음 |
| **운영 (EC2)** | ECR 이미지 → 컨테이너 | `docker-compose.prod.yml` 컨테이너 | EC2 위 SSL termination |

### 6.2 로컬 `docker-compose.yml`

```yaml
version: '3.9'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: tutoring
      MYSQL_USER: tutoring
      MYSQL_PASSWORD: tutoring
    ports: ["3306:3306"]
    volumes: ["mysql_data:/var/lib/mysql"]
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      retries: 10
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      retries: 10
volumes:
  mysql_data:
```

### 6.3 로깅

- **logback-spring.xml**, 프로필별 appender:
  - `local`: 사람이 읽는 pretty (콘솔)
  - `prod`/`test`: JSON 한 줄당 한 로그 (logstash-logback-encoder)
- JSON 필드: `@timestamp`, `level`, `logger`, `message`, `traceId`, `userId`(MDC), `thread`, `exception`

### 6.4 MdcLoggingFilter

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(req, res, chain) {
        String traceId = Optional.ofNullable(req.getHeader("X-Trace-Id"))
                                  .orElse(UUID.randomUUID().toString());
        MDC.put("traceId", traceId);
        res.setHeader("X-Trace-Id", traceId);
        try { chain.doFilter(req, res); }
        finally { MDC.clear(); }
    }
}
```

- 인증 후 `JwtAuthenticationFilter` 안에서 `MDC.put("userId", ...)` 추가

### 6.5 Actuator

- 노출: `health`, `info`만
- `/actuator/health`는 무인증 (NGINX/ALB healthcheck 용)
- `health.show-details: never` (내부 구성 정보 노출 X)
- `/actuator/metrics`, `/actuator/prometheus`는 **추가 안 함** — 모니터링 SaaS 도입 결정 시 후속 spec

---

## 7. CI/CD 파이프라인

### 7.1 워크플로 구조

| 파일 | 트리거 | 역할 |
|------|--------|------|
| `.github/workflows/ci.yml` | PR, `main` push | 빌드 + 테스트 |
| `.github/workflows/deploy.yml` | CI 성공 후 (`workflow_run`) | 이미지 빌드 + ECR push + EC2 배포 |

→ PR은 CI만, main 머지 commit만 배포 흐름 진입.

### 7.2 ci.yml 핵심

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main]
jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew clean build
      - if: failure()
        uses: actions/upload-artifact@v4
        with: { name: test-reports, path: build/reports/tests/ }
```

### 7.3 deploy.yml 핵심 (요약)

```yaml
name: Deploy
on:
  workflow_run:
    workflows: [CI]
    types: [completed]
    branches: [main]
jobs:
  deploy:
    if: github.event.workflow_run.conclusion == 'success'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew bootJar
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id:     ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region:            ap-northeast-2
      - id: ecr-login
        uses: aws-actions/amazon-ecr-login@v2
      - name: Build & push image
        env:
          REGISTRY: ${{ steps.ecr-login.outputs.registry }}
          REPO:     tutoring
        run: |
          docker build -t $REGISTRY/$REPO:${{ github.sha }} \
                       -t $REGISTRY/$REPO:latest .
          docker push $REGISTRY/$REPO:${{ github.sha }}
          docker push $REGISTRY/$REPO:latest
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        env:
          # 모든 운영 secret을 환경변수로 SSH 세션에 전달
          JWT_SECRET:           ${{ secrets.JWT_SECRET }}
          DB_URL:               ${{ secrets.DB_URL }}
          DB_USERNAME:          ${{ secrets.DB_USERNAME }}
          DB_PASSWORD:          ${{ secrets.DB_PASSWORD }}
          REDIS_HOST:           ${{ secrets.REDIS_HOST }}
          GOOGLE_CLIENT_ID:     ${{ secrets.GOOGLE_CLIENT_ID }}
          GOOGLE_CLIENT_SECRET: ${{ secrets.GOOGLE_CLIENT_SECRET }}
          KAKAO_CLIENT_ID:      ${{ secrets.KAKAO_CLIENT_ID }}
          KAKAO_CLIENT_SECRET:  ${{ secrets.KAKAO_CLIENT_SECRET }}
          CORS_ORIGINS:         ${{ secrets.CORS_ORIGINS }}
          ECR_REGISTRY:         ${{ steps.ecr-login.outputs.registry }}
        with:
          host:     ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key:      ${{ secrets.EC2_SSH_KEY }}
          envs: JWT_SECRET,DB_URL,DB_USERNAME,DB_PASSWORD,REDIS_HOST,GOOGLE_CLIENT_ID,GOOGLE_CLIENT_SECRET,KAKAO_CLIENT_ID,KAKAO_CLIENT_SECRET,CORS_ORIGINS,ECR_REGISTRY
          script: |
            set -euo pipefail
            umask 077
            cat > /opt/tutoring/.env.prod <<EOF
            SPRING_PROFILES_ACTIVE=prod
            ECR_REGISTRY=${ECR_REGISTRY}
            DB_URL=${DB_URL}
            DB_USERNAME=${DB_USERNAME}
            DB_PASSWORD=${DB_PASSWORD}
            REDIS_HOST=${REDIS_HOST}
            JWT_SECRET=${JWT_SECRET}
            GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
            GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
            KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID}
            KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
            CORS_ORIGINS=${CORS_ORIGINS}
            EOF
            chmod 600 /opt/tutoring/.env.prod
            cd /opt/tutoring
            aws ecr get-login-password --region ap-northeast-2 \
              | docker login --username AWS --password-stdin ${ECR_REGISTRY}
            docker compose -f docker-compose.prod.yml pull app
            docker compose -f docker-compose.prod.yml up -d --no-deps app
            docker image prune -f
```

### 7.4 Dockerfile (multi-stage, non-root)

```dockerfile
FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 7.5 docker-compose.prod.yml (EC2)

```yaml
version: '3.9'
services:
  nginx:
    image: nginx:1.27-alpine
    ports: ["80:80", "443:443"]
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - ./nginx/logs:/var/log/nginx
      - ./nginx/.htpasswd:/etc/nginx/.htpasswd:ro
    depends_on: [app]
    restart: unless-stopped
  app:
    image: ${ECR_REGISTRY}/tutoring:latest
    expose: ["8080"]
    env_file: [./.env.prod]
    depends_on: [mysql, redis]
    restart: unless-stopped
  mysql:
    image: mysql:8.0
    volumes: [mysql_data:/var/lib/mysql]
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-rootpassword}
      MYSQL_DATABASE: tutoring
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    restart: unless-stopped
  redis:
    image: redis:7-alpine
    volumes: [redis_data:/data]
    command: ["redis-server", "--appendonly", "yes"]
    restart: unless-stopped
volumes:
  mysql_data:
  redis_data:
```

### 7.6 NGINX 설정

```nginx
server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate     /etc/letsencrypt/live/api.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.example.com/privkey.pem;

    location /api/    { proxy_pass http://app:8080; include /etc/nginx/conf.d/proxy.conf; }
    location /oauth2/ { proxy_pass http://app:8080; include /etc/nginx/conf.d/proxy.conf; }
    location /login/  { proxy_pass http://app:8080; include /etc/nginx/conf.d/proxy.conf; }

    # Swagger — Basic Auth로 별도 보호
    location ~ ^/(swagger-ui|v3/api-docs) {
        auth_basic "Restricted";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://app:8080;
        include /etc/nginx/conf.d/proxy.conf;
    }
}
server { listen 80; return 301 https://$host$request_uri; }
```

- SSL: **Let's Encrypt + certbot** (EC2 호스트에 설치). NGINX는 컨테이너에서 실행되므로 인증서 갱신은 **webroot 챌린지** 방식 사용:
  - certbot이 `/var/www/letsencrypt/`에 챌린지 파일 작성
  - NGINX `location /.well-known/acme-challenge/ { root /var/www/letsencrypt; }` 추가
  - 갱신: `certbot renew` cron (매월 1회), reload는 `docker compose exec nginx nginx -s reload`
- `/etc/nginx/.htpasswd`는 EC2 디스크에 직접 (배포 흐름과 별도 관리)
- 프론트엔드 개발자에게 Basic Auth ID/PW 공유하여 Swagger 열람 가능

### 7.6.1 EC2 사전 요구사항

- **AWS CLI 설치** (ECR login 용)
- **IAM Instance Profile** 부착: `AmazonEC2ContainerRegistryReadOnly` 권한 (액세스 키 디스크 저장 불필요, 비용 0)
- **Docker / Docker Compose v2 설치**
- **certbot 설치 + Let's Encrypt 초기 발급 수동 1회**
- `/opt/tutoring/` 디렉터리에 `docker-compose.prod.yml`, `nginx/` 디렉터리, `.htpasswd` 사전 배치

### 7.7 Graceful Shutdown

- `server.shutdown: graceful`
- `spring.lifecycle.timeout-per-shutdown-phase: 30s`
- 컨테이너 교체 시 in-flight 요청 보호 (brief downtime 5~10초 허용)

### 7.8 롤백

수동 롤백 절차 (EC2에서):
```bash
# 이전 git SHA 태그로 되돌리기
docker pull $ECR_REGISTRY/tutoring:<previous-sha>
docker tag  $ECR_REGISTRY/tutoring:<previous-sha> $ECR_REGISTRY/tutoring:latest
docker compose -f docker-compose.prod.yml up -d --no-deps app
```

- ECR lifecycle policy: 최근 10개 + `latest` 보존, 나머지 30일 후 자동 정리
- 자동 롤백은 Foundation 범위 밖 (ALB + 2대 구성 시 후속 spec)

---

## 8. 설정 & 시크릿 관리

### 8.1 설정 우선순위

```
[높음] 환경변수 / 시스템 프로퍼티     ← 운영에서 secret 주입
       application-{profile}.yml      ← 프로필별 오버라이드
[낮음] application.yml                ← 공통 기본값
```

### 8.2 프로필

| 프로필 | 활성 조건 | 용도 |
|--------|-----------|------|
| `local` | 개발자 PC | IDE 실행, devtools, pretty 로그 |
| `test` | `@ActiveProfiles("test")` | Testcontainers, 더미 secret |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` (EC2) | JSON 로그, env 주입 secret |

### 8.3 application.yml (공통)

```yaml
spring:
  application.name: tutoring
  jpa:
    hibernate.ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
server:
  port: 8080
  shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 30s
management:
  endpoints.web.exposure.include: health,info
  endpoint.health.show-details: never
springdoc:
  api-docs.path: /v3/api-docs
  swagger-ui.path: /swagger-ui.html
logging:
  level.root: INFO
  level.com.tutoring: DEBUG
```

### 8.4 application-prod.yml.template

```yaml
spring:
  datasource:
    url:      ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data.redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT:6379}
  security.oauth2.client.registration:
    google:
      client-id:     ${GOOGLE_CLIENT_ID}
      client-secret: ${GOOGLE_CLIENT_SECRET}
    kakao:
      client-id:     ${KAKAO_CLIENT_ID}
      client-secret: ${KAKAO_CLIENT_SECRET}
  security.oauth2.client.provider:
    kakao:
      authorization-uri:   https://kauth.kakao.com/oauth/authorize
      token-uri:           https://kauth.kakao.com/oauth/token
      user-info-uri:       https://kapi.kakao.com/v2/user/me
      user-name-attribute: id
app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-validity-ms:  900000       # 15분
    refresh-token-validity-ms: 1209600000   # 14일
  cors:
    allowed-origins: ${CORS_ORIGINS}
logging.level.com.tutoring: INFO
```

### 8.5 Secret 관리 — GitHub Secrets 단일 진실

| 단계 | 동작 |
|------|------|
| 등록 | GitHub repo → Settings → Secrets and variables → Actions |
| 주입 | `deploy.yml`의 SSH step이 EC2에 `/opt/tutoring/.env.prod` 생성 (`chmod 600`) |
| 사용 | `docker-compose.prod.yml`의 `env_file` → Spring Boot `${ENV_VAR}` 치환 |
| 회전 | GitHub Secrets 수정 → `deploy.yml` 재실행 |

**필수 운영 규칙:**
> 모든 GitHub Secrets 값은 **팀 비밀번호 관리자(1Password/Bitwarden 등)에 동시 보관**한다. GitHub repo 손실/권한 회수 사고 시 복구 경로 확보.

### 8.6 Secrets 카탈로그

| Secret | 용도 |
|--------|------|
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | ECR push (IAM 권한: `AmazonEC2ContainerRegistryPowerUser`만) |
| `EC2_HOST` / `EC2_USER` / `EC2_SSH_KEY` | SSH 접속 |
| `JWT_SECRET` | JWT 서명 (최소 256bit, `openssl rand -hex 32`) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 접속 |
| `REDIS_HOST` | Redis 호스트 (현재는 EC2 내부 `redis`) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | OAuth2 |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | OAuth2 |
| `CORS_ORIGINS` | CORS 허용 origin (comma-separated) |

### 8.7 설정 검증

```java
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
    @NotBlank String secret,
    @Positive long accessTokenValidityMs,
    @Positive long refreshTokenValidityMs
) {}
```

- 부팅 시 검증 실패하면 즉시 종료
- 컨테이너 healthcheck도 자연스럽게 fail → 배포 자체 중단 → "secret 누락" 사고 방지

### 8.8 .gitignore 변경

기존 `*.md`, `*.yaml`, `*.yml`, `*.properties` 전체 ignore는 유지하되 다음 예외 추가:

```
# 예외: 설계 문서 및 운영 가이드
!docs/**/*.md

# 예외: 커밋 대상 yml (secret 없음)
!**/src/main/resources/**/application.yml
!**/src/main/resources/**/application-*.yml.template
!**/src/test/resources/**/application*.yml
```

규칙 동작:
- `application.yml`, `application-test.yml`, `application-local.yml.template`, `application-prod.yml.template` → 커밋됨
- `application-local.yml`, `application-prod.yml` (실제 secret 포함) → 위 예외에 매칭 안 됨 → `*.yml` 규칙으로 ignore 유지
- `docs/` 하위 `.md` → 커밋. 기타 루트의 `.md` (예: `HELP.md`) → ignore 유지

---

## 9. 미해결 사항 (TODO — 후속 spec)

| # | 주제 | 비고 |
|---|------|------|
| 1 | Live list 갱신 전략 | 첫 list 엔드포인트 등장 도메인 spec에서. 후보: composite cursor + top sentinel |
| 2 | 멀티 디바이스 refresh token | 현재 `refresh:{userId}` 단일 키. 디바이스별 필요 시 `refresh:{userId}:{deviceId}` 등으로 확장 |
| 3 | RDS / ElastiCache 이전 | 트래픽 증가 또는 데이터 보존 요구 시. 현재는 EC2 내 컨테이너로 시작 |
| 4 | 자동 롤백 / 무중단 배포 | ALB + 2대 구성 결정 시 |
| 5 | 외부 모니터링 (Prometheus/Grafana/Datadog 등) | SaaS 도입 결정 시 |
| 6 | Domain 확장 (`TUTOR`, `STUDENT` 등 Role) | 도메인 spec에서 |
| 7 | OAuth2 provider 확장 (Naver/Apple 등) | 신규 IdP 도입 결정 시. `OAuth2UserInfo` 구현체 추가만으로 가능 |

---

## 10. 구현 작업 분해 (writing-plans 입력용 힌트)

writing-plans skill이 다음 단위로 분해해서 작업할 수 있음 (참고용, 강제 아님):

1. `build.gradle` 의존성 정리 + JaCoCo + 플러그인 추가
2. `.gitignore` 예외 규칙 반영
3. `BaseEntity` + `User` + `Role` + `AuthProvider` + Flyway baseline
4. 공통 응답 타입 (`ApiResponse`, `ErrorResponse`, `FieldError`, `Pagination`, `PageResponse`) + `ErrorCode` enum + `ApiException`
5. `GlobalExceptionHandler` + `JsonAuthEntryPoint` + `JsonAccessDeniedHandler`
6. `MdcLoggingFilter` + logback-spring.xml (local/prod 분리)
7. JWT 추상화 (`JwtTokenProvider`/`JjwtTokenProvider` + `JwtProperties` + 검증 필터)
8. RefreshTokenStore (`RedisRefreshTokenStore`) + `RedisConfig`
9. OAuth2 흐름 (`OAuth2UserInfo*`, `CustomOAuth2UserService`, `OAuth2LoginSuccessHandler`)
10. `SecurityConfig` 통합 + `CorsConfig`
11. `UserService`(인터페이스/구현) + `UserRepository` + `UserController` (`/api/v1/me`) + `/api/v1/auth/refresh` + `/api/v1/auth/logout`
12. Swagger 설정 (`SwaggerConfig` + `OpenAPIDefinition` + SecurityScheme)
13. `application.yml` + `application-local.yml.template` + `application-prod.yml.template` + `application-test.yml`
14. Testcontainers `IntegrationTestBase` + 수직 슬라이스 TDD 테스트
15. `Dockerfile` (multi-stage, non-root) + `docker-compose.yml` (로컬) + `docker-compose.prod.yml` (운영)
16. NGINX 설정 (`nginx/conf.d/tutoring.conf` + `nginx/.htpasswd` 가이드)
17. GitHub Actions (`ci.yml` + `deploy.yml`)
18. 로컬 셋업 가이드 (`docs/superpowers/local-setup.md`)

각 단계는 TDD 가능한 단위로 작게 — 5.3의 red-green-refactor 사이클 준수.
