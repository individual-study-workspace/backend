# Project: 과외 워크스페이스 백엔드

Spring Boot 3.5.11 + Java 21 백엔드. Flutter 모바일 클라이언트.

## Git Commit Convention

**MUST follow this format for every commit in this repository.**

### Subject 형식

```
<type> : <Verb> <Korean description>
```

- `type` 과 `:` 사이, `:` 와 Subject 본문 사이 모두 **공백 1칸**.
- Verb 는 영어 imperative (Add/Remove/Simplify/Update/Implement/Prevent/Move/Rename).
- 본문은 한국어로 무엇을 변경했는지 간결히.

### Body (선택, 그러나 변경이 사소하지 않으면 권장)

빈 줄 한 줄 후 bullet 목록으로 상세/구현 내용 요약:

```
- 변경한 핵심 항목 1
- 변경한 핵심 항목 2
```

### Type 종류

| type | 의미 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 포맷팅, 세미콜론 누락 등 동작 영향 없는 변경 |
| `refactor` | 코드 리팩토링 (동작 동일) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드 업무, 패키지 매니저, 의존성 관리 |

### Verb 사전

| Verb | 한국어 |
|------|--------|
| `Add` | 추가 |
| `Remove` | 삭제 |
| `Simplify` | 단순화 |
| `Update` | 보완 |
| `Implement` | 구현 |
| `Prevent` | 방지 |
| `Move` | 이동 |
| `Rename` | 이름 변경 |

### Trailer

**없음.** `Co-Authored-By` 등 trailer는 추가하지 않는다.

### 예시

```
feat : Add ApiException 도메인 예외 타입

- Implement ErrorCode 기반 RuntimeException
- 기본 메시지 + 커스텀 메시지 생성자
```

```
chore : Update build.gradle 의존성 정비

- Remove spring-boot-starter-data-jdbc, spring-boot-starter-web-services
- Add web, validation, data-redis, Flyway, JJWT, springdoc-openapi
```

### Commit 실행 시 주의

- PowerShell heredoc(`@'...'@`)을 `git commit -m`에 직접 넘기면 `@` 문자가 message에 leak 됨 (실제로 발생함). **Bash heredoc 사용 권장**:

  ```bash
  git commit -m "$(cat <<'EOF'
  feat : Add ...

  - ...
  EOF
  )"
  ```
