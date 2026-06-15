# Installation And Environment

## 설치 환경

| Tool | Version / Note |
| --- | --- |
| Java | 17 |
| Spring Boot | 4.0.5 |
| Gradle | Gradle Wrapper 사용 |
| MySQL | 8.4, Docker Compose 제공 |
| Redis | 7.2-alpine, Docker Compose 제공 |
| Timezone | `Asia/Seoul` 기준 운영 |

## 주요 라이브러리

| Library | Purpose |
| --- | --- |
| `spring-boot-starter-webmvc` | REST API, SSE |
| `spring-boot-starter-security` | Stateless security filter chain |
| `spring-boot-starter-data-jpa` | JPA repository and query |
| `spring-boot-starter-data-redis` | Refresh token whitelist and blacklist |
| `spring-boot-starter-validation` | Request DTO validation |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI and OpenAPI JSON |
| `io.jsonwebtoken:jjwt-*` | JWT create/parse/validate |
| `io.hypersistence:hypersistence-utils-hibernate-71` | TSID identifier |
| `com.mysql:mysql-connector-j` | MySQL JDBC driver |
| `lombok` | Boilerplate reduction |

## Local Setup

### 1. 환경변수 파일 생성

```bash
cp .env.example .env
```

로컬 실행용 최소값 예시는 다음과 같습니다.

```properties
PORT=8080

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=eye_on
MYSQL_USER=eyeon
MYSQL_PASSWORD=eyeon-local-password
MYSQL_ROOT_PASSWORD=eyeon-root-local-password

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=eyeon-redis-local-password

JWT_SECRET=replace-this-with-a-long-random-secret-at-least-32-bytes
JWT_ACCESS_EXP=900
JWT_REFRESH_EXP=1209600

COOKIE_SECURE=false
COOKIE_SAMESITE=Lax
COOKIE_DOMAIN=
EXPOSE_SWAGGER=true
ALLOWED_ORIGINS=http://localhost:5173
```

### 2. MySQL/Redis 실행

```bash
docker compose up -d mysql redis
```

`docker-compose.yml`은 다음 컨테이너를 실행합니다.

| Service | Image | Port | Data Volume |
| --- | --- | --- | --- |
| `mysql` | `mysql:8.4` | `${MYSQL_PORT:-3306}:3306` | `mysql-data` |
| `redis` | `redis:7.2-alpine` | `${REDIS_PORT:-6379}:6379` | `redis-data` |

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 서버 주소:

```text
http://localhost:8080
```

### 4. Swagger UI 확인

`.env`에 `EXPOSE_SWAGGER=true`를 설정하고 서버를 재시작합니다.

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## 환경변수 상세

| Name | Required | Default | Description |
| --- | --- | --- | --- |
| `PORT` | No | `8080` | API 서버 포트 |
| `MYSQL_HOST` | No | `localhost` | MySQL host |
| `MYSQL_PORT` | No | `3306` | MySQL port |
| `MYSQL_DATABASE` | No | `eye_on` | database name |
| `MYSQL_USER` | Yes | - | database user |
| `MYSQL_PASSWORD` | Yes | - | database password |
| `MYSQL_ROOT_PASSWORD` | Yes for docker compose | - | root password used by MySQL container |
| `REDIS_HOST` | No | `localhost` | Redis host |
| `REDIS_PORT` | No | `6379` | Redis port |
| `REDIS_PASSWORD` | Yes | - | Redis password |
| `JWT_SECRET` | Yes | - | JWT 서명 secret |
| `JWT_ACCESS_EXP` | No | `900` | Access token 유효 시간 초 |
| `JWT_REFRESH_EXP` | No | `1209600` | Refresh token 유효 시간 초 |
| `COOKIE_SECURE` | No | `false` | WEB refresh cookie Secure flag |
| `COOKIE_SAMESITE` | No | `Lax` | WEB refresh cookie SameSite |
| `COOKIE_DOMAIN` | No | empty | WEB refresh cookie domain |
| `ALLOWED_ORIGINS` | No | `http://localhost:5173` | CORS origin 목록 |
| `EXPOSE_SWAGGER` | No | `false` | Swagger/OpenAPI 공개 여부 |
| `GEMINI_PROVIDER` | No | `vertex` | Gemini provider. `vertex`는 GCP Vertex AI, `ai-studio`는 Gemini API key 사용 |
| `GEMINI_API_KEY` | No | empty | `GEMINI_PROVIDER=ai-studio`일 때 사용하는 Gemini API key |
| `GEMINI_MODEL` | No | `gemini-2.5-flash` | Gemini model |
| `GEMINI_ENDPOINT` | No | Google endpoint | AI Studio Gemini endpoint |
| `GEMINI_VERTEX_ENDPOINT` | No | region-derived Vertex AI endpoint | Vertex AI endpoint override |
| `GCP_PROJECT_ID` | No on GCE | metadata server project ID | Vertex AI용 GCP project ID. GCE에서는 비워두면 자동 탐지 |
| `GCP_LOCATION` | No | `global` | Vertex AI location. regional PayGo 429를 줄이려면 `global` 권장 |
| `GEMINI_TIMEOUT_SECONDS` | No | `8` | Gemini timeout |
| `GEMINI_MAX_OUTPUT_TOKENS` | No | `512` | Gemini output token limit |
| `GEMINI_THINKING_BUDGET` | No | `0` | Gemini thinking budget |

### GCP Credit / Vertex AI Gemini

GCP 크레딧을 사용하려면 AI Studio API key 방식 대신 Vertex AI provider를 사용한다.

```properties
GEMINI_PROVIDER=vertex
GEMINI_MODEL=gemini-2.5-flash
GCP_LOCATION=global
```

GCE VM에서는 VM service account의 Application Default Credentials를 사용한다. 
프로젝트에서 Vertex AI/Gemini API를 활성화하고 VM service account에 `Vertex AI User` 권한을 부여한다.

기존 AI Studio API key 방식으로 되돌리려면:

```properties
GEMINI_PROVIDER=ai-studio
GEMINI_API_KEY=your-api-key
```

## Local Verification

컴파일 확인:

```bash
./gradlew compileJava
```

테스트 실행:

```bash
./gradlew test
```

컨테이너 종료:

```bash
docker compose down
```

볼륨까지 삭제하려면:

```bash
docker compose down -v
```

## 운영 설정 팁

- 운영 환경에서는 `COOKIE_SECURE=true`와 HTTPS 종단 설정이 필요합니다.
- `ALLOWED_ORIGINS`는 와일드카드 대신 실제 Web origin을 명시합니다.
- `EXPOSE_SWAGGER=false`를 기본값으로 두고, 운영에서는 필요한 경우에만 내부망에서 노출합니다.
- `JWT_SECRET`은 충분히 긴 랜덤 문자열을 사용하고 저장소에 커밋하지 않습니다.
- `.env`는 `.gitignore`에 포함되어 있으므로 실제 secret은 로컬/서버에만 둡니다.
