# EC2 단일 호스트 — MySQL + Redis + App (Docker Compose)

> 같은 EC2(Ubuntu)에 `app + MySQL + Redis` 를 한 Compose 네트워크로 올린다.
> app 은 DB/Redis 를 **서비스명(`mysql`, `redis`)** 으로 접속한다. RDS/ElastiCache 는 트래픽 신호 시 이전(upgrade path).
>
> 관련 파일: 루트의 `docker-compose.prod.yml`, `.env.prod.example`, `.github/workflows/deploy.yml`.

---

## 0. 사전 준비 (EC2에서 1회)

```bash
# Docker + Compose plugin
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# ubuntu 사용자를 docker 그룹에 (sudo 없이 docker 실행) — 재로그인 필요
sudo usermod -aG docker ubuntu

# AWS CLI (ECR 로그인용)
sudo apt-get install -y awscli
```

**ECR pull 권한**: EC2에 IAM Role 부착 권장 — 정책 `AmazonEC2ContainerRegistryReadOnly`.
이러면 access key 없이 `aws ecr get-login-password` 가 동작한다 (deploy.yml SSH 스크립트가 이걸 사용).

**보안 그룹(인바운드)**:
- `80/tcp` (앱) — 0.0.0.0/0 또는 추후 NGINX 앞단.
- `22/tcp` (SSH) — 내 IP 한정 권장.
- MySQL(3306)/Redis(6379)는 **외부 노출 금지** — compose 내부 네트워크로만 접근하므로 보안 그룹에 열지 않는다.

---

## 1. Compose 파일 + .env 배치 (EC2에서 1회)

deploy.yml 의 SSH 스크립트는 `/home/ubuntu/classit` 에서 compose 를 실행한다.

```bash
mkdir -p /home/ubuntu/classit
cd /home/ubuntu/classit

# 레포에서 docker-compose.prod.yml 와 .env.prod.example 를 가져온다
#  - git clone 후 복사하거나, scp 로 올리거나, 내용 붙여넣기
cp .env.prod.example .env
vi .env   # 실제 비밀번호/시크릿/CORS 도메인 채우기
chmod 600 .env
```

`.env` 채울 값 (예시는 `.env.prod.example` 참고):
`MYSQL_ROOT_PASSWORD`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`(64자 이상 랜덤), `CORS_ORIGINS`,
`GOOGLE_CLIENT_ID/SECRET`, `KAKAO_CLIENT_ID/SECRET`.

> 앱이 prod 프로필로 뜨면서 `DB_URL/DB_USERNAME/DB_PASSWORD/REDIS_HOST/...` 를 env 로 읽는다.
> DB 스키마는 앱 부팅 시 **Flyway** 가 자동 생성한다 (`V202605170001__create_users.sql`).

---

## 2. 먼저 DB/Redis만 띄워 연결 확인 (선택, 권장)

이미지가 ECR에 아직 없다면 app 없이 DB/Redis만 먼저 검증:

```bash
cd /home/ubuntu/classit
docker compose -f docker-compose.prod.yml up -d mysql redis
docker compose -f docker-compose.prod.yml ps          # health: healthy 확인

# MySQL 접속 확인
docker exec -it classit-mysql mysql -uroot -p"$(grep MYSQL_ROOT_PASSWORD .env | cut -d= -f2)" -e "SHOW DATABASES;"
# Redis 확인
docker exec -it classit-redis redis-cli ping           # -> PONG
```

---

## 3. 전체 기동

ECR에 이미지가 올라온 뒤 (deploy.yml 자동 push 또는 수동 push):

```bash
cd /home/ubuntu/classit
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com
export ECR_IMAGE=<ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/classit-server:latest

docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml logs -f app   # 부팅 로그 + Flyway 마이그레이션 확인
```

검증:
```bash
curl -s http://localhost/actuator/health     # -> {"status":"UP"}
```

---

## 4. 자동 배포 흐름 (deploy.yml)

`main` push 시:
1. `application-prod.yml` 생성(템플릿 복사, secret 없음) → `./gradlew clean build` → Docker 이미지 빌드
2. ECR push
3. SSH → `/home/ubuntu/classit` 에서 `docker compose pull app && up -d`

즉 **compose 파일/.env 는 EC2에 한 번 올려두면 되고**, 이후 배포는 app 이미지만 갱신된다.
compose 구조나 env 키가 바뀌면 EC2의 파일도 갱신 필요.

---

## 주의 / 남은 작업
- `./gradlew clean build` 가 통합 테스트(Testcontainers)를 실행한다. CI 러너에서 이게 실패하면 이미지가 push 안 됨 — **별도 이슈로 전체 스택트레이스 확인 필요**.
- 더 이상 `APPLICATION_YML` GitHub Secret 은 쓰지 않는다 (prod 프로필 + 런타임 env 로 대체). 대신 위 `.env` 값들을 EC2에서 관리.
- NGINX(SSL termination, Swagger Basic Auth)와 HTTPS 는 후속 (HANDOFF Plan B 범위).
