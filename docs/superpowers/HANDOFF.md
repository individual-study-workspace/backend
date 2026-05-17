# Session Handoff — Plan A 완료, 다음 작업 인계

> **다음 세션에서 이 문서를 먼저 읽고 안내대로 진행.**
>
> 작성일: 2026-05-18
> 작성 이유: Plan A(Backend Foundation App) 21/21 task 완료. 후속 작업 인계.

---

## 어디까지 진행되었나

### ✅ 완료 (모두 main 브랜치 커밋, push는 아직 X)

**Plan A — Backend Foundation 21개 task 전부 완료** (23 commits ahead of origin/main).

핵심 commit 흐름 (오래된 것 → 최신):
```
3f34f8f docs : Add 백엔드 Foundation 설계 문서
4bc9662 docs : Add Plan A 백엔드 Foundation 구현 계획
76eb39c chore : Update build.gradle 의존성 정비 및 JaCoCo 도입         [Task 1]
ac8d9f3 feat : Add 공통 응답 타입 (ApiResponse, ErrorResponse, ErrorCode 등) [Task 2]
86b86fa feat : Add ApiException 도메인 예외 타입                       [Task 3]
58dab4c feat : Add BaseEntity, Move JPA Auditing to dedicated Config    [Task 4]
ecbb4ce feat : Add Role/AuthProvider/User 엔티티 + 정적 팩토리          [Task 5]
b2c05d1 feat : Add Flyway baseline migration - users 테이블 생성        [Task 6]
369f478 feat : Add UserRepository - findByProviderAndProviderId         [Task 7]
f49967f docs : Add CLAUDE.md 프로젝트 컨벤션 가이드                     (보조)
6b916a6 feat : Add application.yml 프로필 + Testcontainers IntegrationTestBase ... [Task 8]
5e15dd2 feat : Add JwtTokenProvider 추상화 및 JJWT 0.12 구현            [Task 9]
f47dd78 feat : Add RefreshTokenStore 추상화 및 Redis 구현               [Task 10]
ae886fe feat : Add JwtAuthenticationFilter 및 CustomUserPrincipal       [Task 11]
31f7385 feat : Add GlobalExceptionHandler 및 Security JSON 응답 핸들러  [Task 12]
ce72c9e feat : Add MdcLoggingFilter (traceId) 및 프로필별 logback 설정  [Task 13]
f2cf18b feat : Add OAuth2UserInfo 추상화 (Google/Kakao 구현체 + Factory) [Task 14]
b5f8942 feat : Add OAuth2 로그인 처리 (upsert + JWT 발급)               [Task 15]
63f9b34 feat : Add SecurityConfig 통합 체인 및 CorsConfig               [Task 16]
3cfc877 feat : Add GET /api/v1/users/me 수직 슬라이스 완성              [Task 17]
22ddf5b feat : Add POST /api/v1/auth/refresh 및 /logout (rotation 포함) [Task 18]
2b82c87 feat : Add SwaggerConfig (OpenAPI 메타데이터 + Bearer JWT SecurityScheme) [Task 19]
9e7c495 feat : Add 로컬 docker-compose 및 셋업 가이드                   [Task 20]
```

**Task 21 (최종 검증)** — 컴파일 + 29개 단위 테스트 통과. 통합 테스트는 Docker 미설치로 미실행.

---

## ⏸ 미해결 / 다음 세션에서 처리할 것

### 1. Docker 설치 후 통합 테스트 5개 검증 (Plan A 잔여 검증)

**현재 환경**: Docker Desktop 미설치로 통합 테스트는 컴파일만 확인됨.

미실행 통합 테스트 목록:
| 테스트 파일 | Task | 검증 대상 |
|------------|------|----------|
| `src/test/java/com/tutoring/domain/user/repository/UserRepositoryIntegrationTest.java` | 8 | save/find + soft-delete 필터링 |
| `src/test/java/com/tutoring/global/security/refresh/RedisRefreshTokenStoreIntegrationTest.java` | 10 | Redis save/find/delete/overwrite |
| `src/test/java/com/tutoring/domain/user/controller/UserControllerIntegrationTest.java` | 17 | `/api/v1/users/me` 200/401/401 |
| `src/test/java/com/tutoring/domain/user/controller/AuthControllerIntegrationTest.java` | 18 | refresh/logout 3 cases |
| `src/test/java/com/tutoring/global/config/SwaggerEndpointIntegrationTest.java` | 19 | `/v3/api-docs` 200 + bearer-jwt 스키마 |

**실행 절차** (Docker Desktop 설치 후):
```powershell
$env:JAVA_HOME = "C:\Users\Lucas\.jdks\ms-21.0.11"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build
```

기대 결과: BUILD SUCCESSFUL, 총 34 tests (29 unit + 5 integration의 ~11 메서드).

설치 안내: `docs/superpowers/local-setup.md` 1단계 참조 (Docker Desktop + `docker compose up -d`).

### 2. Plan A 완료 후 수동 smoke test (Task 21 Step 3)

Docker + OAuth2 자격증명까지 준비된 후:
1. `application-local.yml` 생성 (template 복사 후 Google/Kakao client-id/secret 채움)
2. `./gradlew bootRun --args='--spring.profiles.active=local'`
3. 브라우저 확인:
   - `http://localhost:8080/actuator/health` → 200 `{"status":"UP"}`
   - `http://localhost:8080/swagger-ui.html` → User + Auth 두 태그 표시
   - `http://localhost:8080/oauth2/authorization/google` → Google 로그인 → JSON에 accessToken + refreshToken
   - 발급된 accessToken으로 Swagger Authorize → GET /api/v1/users/me → 200
   - POST /api/v1/auth/refresh → 새 토큰
   - POST /api/v1/auth/logout → 200

### 3. origin/main에 push

현재 main이 origin/main보다 23 commit 앞섬. 통합 테스트 검증 후 push.

```bash
git push origin main
```

### 4. Plan B (배포 인프라) 작성 + 실행

Plan B 미작성 상태. 다음 세션에서 spec → plan → 실행 순서로 진행.

**Plan B 범위 (원안 spec에서):**
- `Dockerfile` (multi-stage build, Java 21 base)
- `docker-compose.prod.yml` (EC2용 — app + MySQL + Redis + NGINX 한 호스트)
- NGINX 설정
  - SSL termination (Let's Encrypt / certbot)
  - Swagger UI Basic Auth 보호 (`/swagger-ui/**`, `/v3/api-docs/**`)
  - HTTP → HTTPS 리다이렉트
- GitHub Actions
  - `ci.yml`: PR/push 시 build + test
  - `deploy.yml`: main 머지 시 SSH로 EC2에 배포 (ECR push → SSH pull → restart)
- EC2 사전 셋업 가이드 (Docker 설치, ECR 권한, GitHub Secrets 등록, NGINX 설치, 도메인 + DNS, Let's Encrypt 갱신 cron)
- GitHub Secrets 목록 (DB_*, REDIS_*, JWT_SECRET, GOOGLE_*, KAKAO_*, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, ECR_REPOSITORY, EC2_HOST, EC2_SSH_KEY 등)

**시작 명령 (다음 세션):**
> "Plan B(배포 인프라) spec 작성해줘. superpowers brainstorming부터."

### 5. 후속 / 보류 (Plan A에서 결정 안 함)

원안 spec(`docs/superpowers/specs/2026-05-17-backend-foundation-design.md`)에서 명시한 미해결 사항:

1. **Live list 갱신 전략** (composite cursor + top sentinel) — 첫 도메인 list 엔드포인트 등장 시 결정
2. **멀티 디바이스 refresh token** — 현재 `refresh:{userId}` 단일 키. 필요 시 `refresh:{userId}:{deviceId}` 확장
3. **RDS / ElastiCache 이전** — 트래픽 신호 시
4. **자동 롤백 / 무중단 배포** — ALB + 2대 구성 결정 시
5. **외부 모니터링** — Prometheus/Grafana/Datadog 결정 시
6. **Role 확장** (`TUTOR`/`STUDENT` 등) — 도메인 spec에서
7. **OAuth2 provider 확장** (Naver/Apple) — 신규 IdP 도입 시
8. **첫 도메인 spec** — 과외 워크스페이스의 실제 도메인 모델 (수업/과제/일정/메시지 등). Plan A 끝났으니 다음 우선순위 후보.

---

## 핵심 결정사항 (변경 사항 반영, 빠른 파악용)

| 항목 | 결정 |
|------|------|
| 범위 | Foundation 완료. 도메인 기능 spec은 후속 |
| 아키텍처 | Layered + Domain packaging |
| 인증 | OAuth2(Google/Kakao) → 자체 JWT (Access 15분 + Refresh 14일) |
| Token 전달 | JSON body 통일 (HttpOnly 쿠키 X) — Flutter 호환 |
| Refresh Token store | Redis (`refresh:{userId}` 키) |
| AWS | EC2 + ECR + NGINX (SSL termination via Let's Encrypt) — 초기엔 MySQL/Redis도 EC2 docker로 |
| CI/CD | GitHub Actions + SSH 직접 (SSM 아님) — **Plan B에서 구현** |
| Secrets | **GitHub Secrets**로 통일 (AWS Parameter Store 사용 X — 비용 절감) |
| DB Migration | Flyway (`V{YYYYMMDDHHmm}__*.sql`) — `V202605170001__create_users.sql` 작성됨 |
| Test | JUnit5 + Mockito + Testcontainers (`.withReuse(true)`) |
| API 규약 | `/api/v1` URL versioning + `ApiResponse<T>` wrapper + `ErrorResponse.details[]` for validation |
| Pagination | Cursor-based `Pagination(hasNext, nextCursor)` + `PageResponse<T>` |
| Swagger | 운영에서도 노출, NGINX Basic Auth로 보호 (**Plan B에서 NGINX 설정**) |
| 로깅 | local=pretty, prod/test=JSON (logstash-logback-encoder) + MDC traceId/userId |
| **Commit convention** | `<type> : <Verb> <Korean>` + bullets, **NO trailer (Co-Authored-By 금지)**. 자세한 규칙은 repo `CLAUDE.md` |

---

## 사용자 선호 (메모리에서 자동 로드되지만 강조)

- **언어:** 한국어 우선, 기술 용어는 영어/한국어 혼용 OK
- **응답 스타일:** 간결하고 결정적
- **결정 스타일:** 옵션 제시 후 추천 명시하면 그쪽으로 가는 편. 명시적으로 권한을 위임받으면 끝까지 진행 (Plan A에서 검증됨)
- **AWS 비용:** 최소화 우선. managed 서비스는 upgrade path로만
- **Frontend:** Flutter (모바일 우선, 웹 후순위)
- **Commit message**: 위 convention 엄격 준수

---

## 알아둘 환경 특이사항

### Java 환경
- Java 21 (Microsoft OpenJDK 21.0.11): `C:\Users\Lucas\.jdks\ms-21.0.11`
- **PATH에 없음** — gradle 실행 시 매번 prefix 필요:
  ```powershell
  $env:JAVA_HOME = "C:\Users\Lucas\.jdks\ms-21.0.11"; $env:Path = "$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat <task>
  ```

### Docker
- **미설치 상태**. Plan A 통합 테스트 5개와 로컬 부팅 smoke test가 미실행 상태로 남아 있음.
- Plan B 작업 전에 설치 강력 권장 (NGINX/Dockerfile 검증에도 필요).

### PowerShell heredoc 주의
- `git commit -m "@'...'@"` 형식 사용 시 `@` 문자가 commit message에 leak됨 (실제 사고 있음 — Task 4에서 발생, filter-branch로 복구).
- **반드시 Bash heredoc 사용**: `git commit -m "$(cat <<'EOF' ... EOF)"`.

### `.gitignore` 동작 (예외 규칙 추가됨)
- `*.md` 전역 ignore + 예외: `docs/**/*.md`, `CLAUDE.md`
- `*.yml`, `*.yaml`, `*.properties` 전역 ignore + 예외:
  - `**/src/main/resources/**/application.yml`
  - `**/src/main/resources/**/application-*.yml.template`
  - `**/src/test/resources/**/application*.yml`
  - `docker-compose.yml` (Plan A 진행 중 추가)
- 새 yml/md를 추가할 때 위 예외에 해당하지 않으면 ignore 되므로 주의.

### Spring Boot 3.5 / Hibernate 6.6 / JJWT 0.12 주의
- `@SQLRestriction` (Hibernate 6.3+) 정상 동작
- JJWT 0.12 신규 API: `.subject()`, `.issuedAt()`, `.expiration()`, `.signWith(key)`, `Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()`
- `spring-boot-starter-web-services`는 SOAP용 — REST에 사용 X (build.gradle에서 이미 제거됨)

---

## 다음 세션 시작 방법 (시나리오별)

### A. Docker 설치 → 통합 테스트 검증 + Plan B 작성
> "docs/superpowers/HANDOFF.md 읽고, Docker 설치했어. 통합 테스트 5개 검증한 다음 Plan B(배포 인프라) brainstorming부터 시작해줘."

### B. Docker 없이 Plan B만 먼저 작성 (실행은 보류)
> "HANDOFF.md 읽고, Plan B(배포 인프라) spec/plan 먼저 작성해줘. 통합 테스트와 Plan B 실행은 Docker 준비된 다음 진행."

### C. 첫 도메인 spec부터 작성 (Plan B 미루기)
> "HANDOFF.md 읽고, Plan B는 미루고 첫 도메인(예: 수업 관리) spec brainstorming부터 진행."

### D. 무엇부터 시작할지 같이 정하기
> "HANDOFF.md 읽고, 다음에 뭘 먼저 할지 옵션 정리해서 추천해줘."

---

## 새로 도입된 운영 아티팩트 (Plan A에서 추가됨)

| 파일 | 용도 |
|------|------|
| `CLAUDE.md` (repo 루트) | 프로젝트 commit convention 명문화 (auto-load) |
| `docker-compose.yml` | 로컬 MySQL 8 + Redis 7 의존성 (`docker compose up -d`) |
| `docs/superpowers/local-setup.md` | 5단계 로컬 셋업 (Docker → yml 복사 → IDE 실행 → 검증) |
| `application-local.yml.template` | local profile 템플릿 (REPLACE_ME 채우기) |
| `application-prod.yml.template` | prod profile 템플릿 (env var 주입) |
| `~/.claude/projects/.../memory/feedback_commit_convention.md` | commit convention 메모리 (cross-session) |

---

## 만약 다른 방향으로 진행하고 싶다면

| 시나리오 | 명령 예시 |
|---------|----------|
| Plan A의 특정 코드 리뷰/수정 | "Task N의 X 파일 리뷰해줘" |
| 통합 테스트 디버깅 | "통합 테스트 실패하는데 로그 같이 봐줘" |
| Plan A 위에 새 도메인 endpoint 빠르게 추가 | "예제로 /api/v1/lessons GET 하나 만들어줘" |
| `application.yml` 추가 키 더하기 | "actuator metrics endpoint 추가해줘" |

---

**이 파일은 다음 세션에서 첫 reference로 읽으면 됩니다. 작업 진행하면서 outdated 되면 업데이트 또는 삭제.**
