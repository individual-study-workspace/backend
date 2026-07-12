# Project: 과외 워크스페이스 백엔드

Backend : Spring Boot 3.5.11 + Java 21 + JPA + Flyway + Dynamic Query + Redis
Frontend : Flutter (다른 팀)
DevOps : Figma, Aws Ec2(ec2 안에 Docker로 mysql, redis 올라가있음, nginx로 HTTPS 및 SSL 인증)

## 설정 파일 규칙 (절대 준수)

- **모든 설정은 `src/main/resources/application.yml` 단일 파일로만 관리한다.**
- `application-local.yml`, `application-prod.yml`, `application-*.yml.template`, 별도 프로파일 yml/properties 등 **추가 설정 파일을 절대 만들지 않는다.**
- 프로파일 분기(`prod`/`local` 등)로 파일을 쪼개지 않는다. 변경이 필요하면 `application.yml` 안에서 수정한다.
- Claude 는 위 규칙을 어기고 새 설정 파일을 생성/제안하지 말 것.

## Git Commit Convention (필수)

**MUST follow this format for every commit in this repository.**
### 커밋 스킬 위치
Git에 커밋할 때는 해당 스킬을 반드시 사용한다
- .claude/skills/git-commit/SKILL.md

## 개발 진행 방식 (TDD 사용 X)

- 빠른 개발을 위해 **TDD(테스트 먼저 작성) 방식을 사용하지 않는다.** (기존 코드에 TDD 코드가 있을 경우 삭제)
- TDD 대신 **"계획 문서를 안전벨트로 삼는"** 방식으로 개발한다. 상세 규칙은 아래 문서를 반드시 참고한다.
  - **[팀 개발 워크플로우](./docs/팀-개발-워크플로우.md)** — 표준 개발 흐름·Story 쪼개기·테스트 정책·협업 규칙 (실무 가이드)
  - **[BMAD 방법론 요약](./docs/bmad-method-방법론.md)** — 위 워크플로우의 원본 개념
- 표준 흐름: `브레인스토밍 → 계획 문서 → Story 쪼개기 → 착수 전 점검 → 구현 → /code-review → 커밋`
- 핵심 원칙:
  1. 계획 문서는 **Git에 커밋**해 팀원·Claude 세션 간 컨텍스트를 정렬한다.
  2. Story는 **Claude 컨텍스트 창 크기**로 쪼갠다 (한 세션에 끝낼 단위).
  3. 테스트는 **구현 안정 후 핵심 경로만** 통합 테스트로 작성한다 (유닛 TDD 금지).
  4. **작은 수정은 경량 경로**로, 큰 기능만 풀 사이클로 (과잉 프로세스 금지).

