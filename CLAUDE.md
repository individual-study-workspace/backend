# Project: 과외 워크스페이스 백엔드

Backend : Spring Boot 3.5.11 + Java 21 + JPA + Flyway + Dynamic Query + Redis
Frontend : Flutter (다른 팀)
DevOps : Figma, Aws Ec2(ec2 안에 Docker로 mysql, redis 올라가있음, nginx로 HTTPS 및 SSL 인증)

## 설정 파일 규칙 (절대 준수)

- **모든 설정은 `src/main/resources/application.yml` 단일 파일로만 관리한다.**
- `application-local.yml`, `application-prod.yml`, `application-*.yml.template`, 별도 프로파일 yml/properties 등 **추가 설정 파일을 절대 만들지 않는다.**
- 프로파일 분기(`prod`/`local` 등)로 파일을 쪼개지 않는다. 변경이 필요하면 `application.yml` 안에서 수정한다.
- Claude 는 위 규칙을 어기고 새 설정 파일을 생성/제안하지 말 것.

## API 문서화 규칙 (Swagger, 필수)

- **API 하나가 완성되면 Swagger(OpenAPI) 문서를 반드시 작성한다.** (프론트엔드(Flutter)가 API 계약을 참고)
- 문서 애노테이션은 **컨트롤러에 직접 달지 않고 `XxxApi` 인터페이스로 분리**하고, 컨트롤러가 `implements` 한다.
  - `XxxApi` 인터페이스: **OpenAPI 애노테이션만** (`@Tag`, `@Operation`, `@ApiResponses`, `@Parameter` …).
  - `XxxController implements XxxApi`: **Spring MVC 애노테이션만** (`@RestController`, `@PostMapping`, `@PreAuthorize`, `@RequestBody`, `@Valid` …) + 메서드에 `@Override`.
- `@AuthenticationPrincipal` 파라미터는 `SwaggerConfig` 의 전역 무시 설정으로 문서에서 감춘다 — 개별 `@Parameter(hidden = true)` 반복 금지.
- 인증 스킴은 `SwaggerConfig` 의 전역 `bearer-jwt` 를 사용한다 (엔드포인트마다 재선언 금지).
- 참고 구현: `domain/classroom/controller/ClassroomApi` + `ClassroomController`.

## Git Commit Convention (필수)

**MUST follow this format for every commit in this repository.**
### 커밋 스킬 위치
Git에 커밋할 때는 해당 스킬을 반드시 사용한다
- .claude/skills/git-commit/SKILL.md

## 개발 진행 방식 (TDD 사용 X)

- **방법론은 BMad Method 로 통일한다.** superpowers 플러그인·bkit 은 이 프로젝트에서 **사용하지 않는다.** (Claude 도 superpowers 의 brainstorm→plan→execute 대신 BMad 흐름을 따를 것)
- **계획/설계 문서는 `_bmad-output/planning-artifacts/` 에 저장하고 Git에 커밋한다.**
  - 구현 중 임시 산출물은 `_bmad-output/implementation-artifacts/` — **커밋 제외.**
  - BMad 프레임워크 본체 `_bmad/` 도 **커밋 제외** (설치형 도구, 팀원 각자 로컬 설치).
- 빠른 개발을 위해 **TDD(테스트 먼저 작성) 방식을 사용하지 않는다.** (기존 코드에 TDD 코드가 있을 경우 삭제)
- TDD 대신 **"계획 문서를 안전벨트로 삼는"** 방식으로 개발한다. 상세 규칙은 아래 문서를 반드시 참고한다.
  - **[팀 개발 워크플로우](./docs/팀-개발-워크플로우.md)** — 표준 개발 흐름·Story 쪼개기·테스트 정책·협업 규칙 (실무 가이드)
  - **[BMAD 방법론 요약](./docs/bmad-method-방법론.md)** — 위 워크플로우의 원본 개념
- 표준 흐름: `브레인스토밍 → 계획 문서 → Story 쪼개기 → 착수 전 점검 → 구현 → API 문서화(Swagger) → /code-review → 커밋`
- 핵심 원칙:
  1. 계획 문서는 **Git에 커밋**해 팀원·Claude 세션 간 컨텍스트를 정렬한다.
  2. Story는 **Claude 컨텍스트 창 크기**로 쪼갠다 (한 세션에 끝낼 단위).
  3. 테스트는 **구현 안정 후 핵심 경로만** 통합 테스트로 작성한다 (유닛 TDD 금지).
  4. **작은 수정은 경량 경로**로, 큰 기능만 풀 사이클로 (과잉 프로세스 금지).

