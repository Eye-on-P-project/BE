# Usage Examples

이 페이지는 Eye:on Backend를 실제 클라이언트에서 호출할 때 바로 참고할 수 있는 예제 코드 모음입니다.

## curl 빠른 연동

### 1. APP 회원가입

```bash
curl -X POST "http://localhost:8080/api/auth/signup" \
  -H "Content-Type: application/json" \
  -H "X-Client-Type: APP" \
  -d '{
    "email": "driver@example.com",
    "password": "1234",
    "name": "홍길동",
    "nickname": "길동",
    "age": 24,
    "gender": "MALE"
  }'
```

### 2. APP 로그인

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Client-Type: APP" \
  -d '{
    "email": "driver@example.com",
    "password": "1234"
  }'
```

응답에서 `accessToken`, `refreshToken`을 저장합니다.

### 3. 모니터링 세션 시작

```bash
ACCESS_TOKEN="eyJ..."

curl -X POST "http://localhost:8080/api/monitoring/sessions/start" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d '{
    "mode": "DRIVING",
    "startedAtApp": "2026-04-20T10:00:00"
  }'
```

### 4. 졸음 이벤트 전송

```bash
SESSION_ID="223456789012345678"

curl -X POST "http://localhost:8080/api/monitoring/sessions/${SESSION_ID}/events" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d '{
    "eventType": "DROWSY",
    "occurredAtApp": "2026-04-20T10:05:12"
  }'
```

### 5. 모니터링 세션 종료

```bash
curl -X POST "http://localhost:8080/api/monitoring/sessions/${SESSION_ID}/end" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d '{
    "endedAtApp": "2026-04-20T10:30:00"
  }'
```

### 6. Refresh

```bash
REFRESH_TOKEN="eyJ..."

curl -X POST "http://localhost:8080/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -H "X-Client-Type: APP" \
  -d "{
    \"refreshToken\": \"${REFRESH_TOKEN}\"
  }"
```

## Web 관리자 예제

### WEB 로그인

WEB은 refresh token을 HttpOnly cookie로 받으므로 `curl` 테스트 시 `-c cookies.txt`, refresh 시 `-b cookies.txt`를 사용합니다.

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Client-Type: WEB" \
  -c cookies.txt \
  -d '{
    "email": "admin@example.com",
    "password": "1234"
  }'
```

### 대시보드 요약 조회

```bash
ADMIN_ACCESS_TOKEN="eyJ..."

curl "http://localhost:8080/api/monitoring/dashboard/realtime-summary" \
  -H "Authorization: Bearer ${ADMIN_ACCESS_TOKEN}"
```

### SSE 구독

```bash
curl -N "http://localhost:8080/api/monitoring/dashboard/realtime-summary/stream" \
  -H "Authorization: Bearer ${ADMIN_ACCESS_TOKEN}" \
  -H "Accept: text/event-stream"
```

### 알림 피드 cursor pagination

```bash
curl "http://localhost:8080/api/monitoring/dashboard/notifications?limit=50" \
  -H "Authorization: Bearer ${ADMIN_ACCESS_TOKEN}"
```

다음 페이지:

```bash
curl "http://localhost:8080/api/monitoring/dashboard/notifications?cursor=423456789012345678&limit=50" \
  -H "Authorization: Bearer ${ADMIN_ACCESS_TOKEN}"
```

## JavaScript Fetch 예제

### API client helper

```javascript
const API_BASE_URL = "http://localhost:8080";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.accessToken ? { Authorization: `Bearer ${options.accessToken}` } : {}),
      ...(options.clientType ? { "X-Client-Type": options.clientType } : {}),
      ...(options.headers || {})
    },
    credentials: options.credentials || "include"
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(data?.message || `HTTP ${response.status}`);
  }

  return data;
}
```

### APP 로그인

```javascript
const auth = await request("/api/auth/login", {
  method: "POST",
  clientType: "APP",
  body: JSON.stringify({
    email: "driver@example.com",
    password: "1234"
  })
});

localStorage.setItem("accessToken", auth.accessToken);
localStorage.setItem("refreshToken", auth.refreshToken);
```

### 세션 시작, 이벤트 전송, 종료

```javascript
const accessToken = localStorage.getItem("accessToken");

const session = await request("/api/monitoring/sessions/start", {
  method: "POST",
  accessToken,
  body: JSON.stringify({
    mode: "DRIVING",
    startedAtApp: new Date().toISOString().slice(0, 19)
  })
});

await request(`/api/monitoring/sessions/${session.sessionId}/events`, {
  method: "POST",
  accessToken,
  body: JSON.stringify({
    eventType: "DROWSY",
    occurredAtApp: new Date().toISOString().slice(0, 19)
  })
});

await request(`/api/monitoring/sessions/${session.sessionId}/end`, {
  method: "POST",
  accessToken,
  body: JSON.stringify({
    endedAtApp: new Date().toISOString().slice(0, 19)
  })
});
```

### SSE 구독

기본 `EventSource`는 Authorization header를 직접 넣을 수 없습니다. 운영에서는 cookie 인증 방식, 프록시, 또는 event-source-polyfill 계열 라이브러리 사용을 고려합니다. 현재 API는 Bearer token 인증을 요구하므로 header를 지원하는 SSE 클라이언트를 사용합니다.

```javascript
import { EventSourcePolyfill } from "event-source-polyfill";

const accessToken = localStorage.getItem("accessToken");

const events = new EventSourcePolyfill(
  "http://localhost:8080/api/monitoring/dashboard/realtime-summary/stream",
  {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  }
);

events.addEventListener("connected", event => {
  console.log("connected", JSON.parse(event.data));
});

events.addEventListener("summary", event => {
  console.log("summary", JSON.parse(event.data));
});

events.addEventListener("alert", event => {
  console.log("alert", JSON.parse(event.data));
});

events.addEventListener("heartbeat", event => {
  console.log("heartbeat", JSON.parse(event.data));
});
```

## Android Kotlin 예제

아래 예제는 Retrofit + OkHttp 기준의 최소 형태입니다.

### DTO

```kotlin
data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthTokenResponse(
    val userId: String,
    val accessToken: String?,
    val refreshToken: String?,
    val role: String
)

data class StartMonitoringSessionRequest(
    val mode: String,
    val startedAtApp: String
)

data class MonitoringSessionStartResponse(
    val sessionId: String,
    val userId: String,
    val mode: String,
    val startedAtApp: String,
    val startedAtServer: String,
    val drowsyCount: Int,
    val sleepCount: Int
)

data class CreateMonitoringEventRequest(
    val eventType: String,
    val occurredAtApp: String
)

data class EndMonitoringSessionRequest(
    val endedAtApp: String
)
```

### Retrofit interface

```kotlin
interface EyeOnApi {
    @POST("/api/auth/login")
    suspend fun login(
        @Header("X-Client-Type") clientType: String = "APP",
        @Body body: LoginRequest
    ): AuthTokenResponse

    @POST("/api/monitoring/sessions/start")
    suspend fun startSession(
        @Header("Authorization") authorization: String,
        @Body body: StartMonitoringSessionRequest
    ): MonitoringSessionStartResponse

    @POST("/api/monitoring/sessions/{sessionId}/events")
    suspend fun createEvent(
        @Header("Authorization") authorization: String,
        @Path("sessionId") sessionId: String,
        @Body body: CreateMonitoringEventRequest
    )

    @POST("/api/monitoring/sessions/{sessionId}/end")
    suspend fun endSession(
        @Header("Authorization") authorization: String,
        @Path("sessionId") sessionId: String,
        @Body body: EndMonitoringSessionRequest
    )
}
```

### 호출 흐름

```kotlin
val auth = api.login(
    body = LoginRequest(
        email = "driver@example.com",
        password = "1234"
    )
)

val bearer = "Bearer ${auth.accessToken}"
val now = java.time.LocalDateTime.now().withNano(0).toString()

val session = api.startSession(
    authorization = bearer,
    body = StartMonitoringSessionRequest(
        mode = "DRIVING",
        startedAtApp = now
    )
)

api.createEvent(
    authorization = bearer,
    sessionId = session.sessionId,
    body = CreateMonitoringEventRequest(
        eventType = "DROWSY",
        occurredAtApp = java.time.LocalDateTime.now().withNano(0).toString()
    )
)

api.endSession(
    authorization = bearer,
    sessionId = session.sessionId,
    body = EndMonitoringSessionRequest(
        endedAtApp = java.time.LocalDateTime.now().withNano(0).toString()
    )
)
```

## 조직 관리자 연동 시나리오

### 1. WEB 조직 관리자 가입 신청

```bash
curl -X POST "http://localhost:8080/api/auth/signup" \
  -H "Content-Type: application/json" \
  -H "X-Client-Type: WEB" \
  -d '{
    "email": "admin@example.com",
    "password": "1234",
    "organizationName": "아이온 운송",
    "businessmanNum": "123-45-67890",
    "establishedAt": "2024-01-15",
    "representativeName": "김대표",
    "corporateNum": "110111-1234567",
    "businessName": "아이온 운송 주식회사",
    "businessAddress": "서울특별시 강남구 테헤란로 1"
  }'
```

### 2. 시스템 관리자가 승인

```bash
SYSTEM_ADMIN_TOKEN="eyJ..."
ORGANIZATION_ID="623456789012345678"

curl -X PATCH "http://localhost:8080/api/system-admin/organizations/signups/${ORGANIZATION_ID}/approve" \
  -H "Authorization: Bearer ${SYSTEM_ADMIN_TOKEN}"
```

### 3. 조직 관리자 로그인 후 구성원 추가

```bash
ADMIN_ACCESS_TOKEN="eyJ..."

curl -X POST "http://localhost:8080/api/organizations/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ADMIN_ACCESS_TOKEN}" \
  -d '{
    "email": "driver@example.com"
  }'
```

### 4. 사용자가 조직 모드로 모니터링 시작

```bash
USER_ACCESS_TOKEN="eyJ..."

curl -X POST "http://localhost:8080/api/monitoring/sessions/start" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${USER_ACCESS_TOKEN}" \
  -d '{
    "mode": "ORGANIZATION",
    "startedAtApp": "2026-04-20T10:00:00"
  }'
```

## 예상 발생 가능한 에러 (로컬 실행 중 에러가 있을 경우 참고)

| Code | Situation | How To Fix |
| --- | --- | --- |
| `INVALID_CLIENT_TYPE` | `X-Client-Type`에 `MOBILE` 등 미지원 값 사용 | `APP` 또는 `WEB` 사용 |
| `WEB_ADMIN_LOGIN_ONLY` | 일반 사용자가 WEB으로 로그인 | APP으로 로그인하거나 관리자 계정 사용 |
| `ORGANIZATION_SIGNUP_PENDING` | 승인 전 조직 관리자 로그인 | 시스템 관리자 승인 후 로그인 |
| `ORGANIZATION_MEMBER_NOT_FOUND` | 조직 구성원이 아닌 사용자가 `ORGANIZATION` 모드 시작 | 조직 관리자 계정으로 구성원 추가 |
| `TOKEN_BLACKLISTED` | 이미 refresh/logout 처리된 token 재사용 | 최신 refresh token 사용 |
| `REFRESH_TOKEN_MISMATCH` | Redis whitelist와 다른 refresh token 사용 | 다시 로그인 |
| `AGENT_SUBSCRIPTION_REQUIRED` | FREE 사용자가 AI Agent chat 호출 | 사용자 또는 조직 구독 필요 |
