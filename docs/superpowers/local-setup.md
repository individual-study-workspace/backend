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
