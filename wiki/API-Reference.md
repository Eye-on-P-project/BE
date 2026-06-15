# API Reference

## 공통 규칙

Base URL:

```text
http://localhost:8080
```

인증이 필요한 API:

```http
Authorization: Bearer {accessToken}
```

클라이언트 구분:

```http
X-Client-Type: APP
```

| Header | Values | Description |
| --- | --- | --- |
| `X-Client-Type` | `APP`, `WEB` | 미입력 시 `APP`으로 처리 |
| `Authorization` | `Bearer {accessToken}` | Auth API 일부를 제외한 보호 API에서 필요 |
| `Content-Type` | `application/json` | JSON body API에서 필요 |
| `Accept` | `text/event-stream` | SSE 구독 시 권장 |

공통 에러 응답:

```json
{
  "timestamp": "2026-04-20T10:00:00Z",
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "요청 값이 올바르지 않습니다.",
  "path": "/api/auth/login",
  "errors": [
    {
      "field": "email",
      "reason": "이메일은 필수입니다."
    }
  ]
}
```

Long ID는 JavaScript number precision 문제를 줄이기 위해 일부 응답에서 문자열로 직렬화됩니다.

## Auth API

### POST `/api/auth/signup`

회원가입 API입니다. `X-Client-Type`에 따라 동작이 다릅니다.

| Client | Behavior |
| --- | --- |
| `APP` | 일반 사용자(`USER`) 생성 후 access/refresh token 발급 |
| `WEB` | 조직 관리자(`ADMIN`)와 `PENDING` 조직 신청 생성. 승인 전까지 token은 발급하지 않음 |

APP request:

```json
{
  "email": "driver@example.com",
  "password": "1234",
  "name": "홍길동",
  "nickname": "길동",
  "age": 24,
  "gender": "MALE"
}
```

WEB request:

```json
{
  "email": "admin@example.com",
  "password": "1234",
  "organizationName": "아이온 운송",
  "businessmanNum": "123-45-67890",
  "establishedAt": "2024-01-15",
  "representativeName": "김대표",
  "corporateNum": "110111-1234567",
  "businessName": "아이온 운송 주식회사",
  "coRepresentativeName": "박공동",
  "businessAddress": "서울특별시 강남구 테헤란로 1"
}
```

APP response:

```json
{
  "userId": "123456789012345678",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "role": "USER"
}
```

WEB pending response:

```json
{
  "userId": "123456789012345679",
  "accessToken": null,
  "refreshToken": null,
  "role": "ADMIN"
}
```

### POST `/api/auth/login`

로그인 후 token을 발급합니다.

Request:

```json
{
  "email": "driver@example.com",
  "password": "1234"
}
```

APP response body에는 refresh token이 포함됩니다.

```json
{
  "userId": "123456789012345678",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "role": "USER"
}
```

WEB response body의 `refreshToken`은 `null`이고, refresh token은 HttpOnly cookie로 설정됩니다.

```json
{
  "userId": "123456789012345679",
  "accessToken": "eyJ...",
  "refreshToken": null,
  "role": "ADMIN"
}
```

WEB 로그인 정책:

- `ADMIN`, `SYSTEM_ADMIN`만 WEB 로그인이 가능합니다.
- `ADMIN`의 조직 상태가 `PENDING`이면 `ORGANIZATION_SIGNUP_PENDING` 에러가 발생합니다.
- `ADMIN`의 조직 상태가 `REJECTED`이면 `ORGANIZATION_SIGNUP_REJECTED` 에러가 발생합니다.

### POST `/api/auth/refresh`

Refresh token을 검증하고 access/refresh token을 재발급합니다. 기존 refresh token jti는 blacklist에 들어가므로 refresh token rotation 방식입니다.

APP request:

```json
{
  "refreshToken": "eyJ..."
}
```

WEB request:

```json
{}
```

WEB은 request body가 없어도 refresh cookie에서 token을 읽습니다.

Response:

```json
{
  "userId": "123456789012345678",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "role": "USER"
}
```

### POST `/api/auth/logout`

Access token과 refresh token을 blacklist 처리하고 refresh whitelist를 제거합니다. WEB은 refresh cookie도 만료시킵니다.

APP request:

```json
{
  "refreshToken": "eyJ..."
}
```

Response:

```json
{
  "success": true
}
```

## User API

### GET `/api/users/me`

현재 로그인 사용자를 조회합니다.

Response:

```json
{
  "userId": "123456789012345678",
  "email": "driver@example.com",
  "role": "USER",
  "organization": null,
  "subscription": "FREE",
  "name": "홍길동",
  "nickname": "길동",
  "age": 24,
  "gender": "MALE"
}
```

### PATCH `/api/users/me/password`

현재 비밀번호를 검증하고 새 비밀번호로 변경합니다. 요청 body의 `organization`은 조직 코드 검증에 사용됩니다.

Request:

```json
{
  "currentPassword": "1234",
  "newPassword": "5678",
  "organization": "A1B2C3"
}
```

Response:

```json
{
  "success": true
}
```

## Monitoring API

### POST `/api/monitoring/sessions/start`

모니터링 세션을 시작합니다.

Request:

```json
{
  "mode": "ORGANIZATION",
  "startedAtApp": "2026-04-20T10:00:00"
}
```

`mode` 값:

| Value | Description |
| --- | --- |
| `ORGANIZATION` | 조직 관제 대상 세션. 조직 구성원 등록 필요 |
| `DRIVING` | 개인 운전 모드 |
| `STUDY` | 개인 학습 모드 |

Response:

```json
{
  "sessionId": "223456789012345678",
  "userId": "123456789012345678",
  "mode": "ORGANIZATION",
  "startedAtApp": "2026-04-20T10:00:00",
  "startedAtServer": "2026-04-20T10:00:01",
  "drowsyCount": 0,
  "sleepCount": 0
}
```

### POST `/api/monitoring/sessions/{sessionId}/events`

세션에 상태 이벤트를 기록합니다.

Request:

```json
{
  "eventType": "DROWSY",
  "occurredAtApp": "2026-04-20T10:05:12"
}
```

`eventType` 값:

| Value | Description |
| --- | --- |
| `NORMAL` | 정상 상태 또는 정상 복귀 |
| `DROWSY` | 졸음 의심 |
| `SLEEP` | 수면 상태 |

현재 구현은 요청 DTO에 `occurredAtApp`이 필수로 존재하지만, 타임존이 다른 문제사 생겨서
이벤트 순서와 서버 저장 일관성을 위해 실제 저장 시각은 서버 수신 시각을 기준으로 `occurredAtApp`, `occurredAtServer`에 기록합니다.

Response:

```json
{
  "eventId": "323456789012345678",
  "sessionId": "223456789012345678",
  "eventType": "DROWSY",
  "occurredAtApp": "2026-04-20T10:05:13",
  "occurredAtServer": "2026-04-20T10:05:13",
  "drowsyCount": 1,
  "sleepCount": 0
}
```

### POST `/api/monitoring/sessions/{sessionId}/end`

세션을 종료합니다.

Request:

```json
{
  "endedAtApp": "2026-04-20T10:30:00"
}
```

Response:

```json
{
  "sessionId": "223456789012345678",
  "userId": "123456789012345678",
  "mode": "ORGANIZATION",
  "startedAtApp": "2026-04-20T10:00:00",
  "startedAtServer": "2026-04-20T10:00:01",
  "endedAtApp": "2026-04-20T10:30:00",
  "endedAtServer": "2026-04-20T10:30:01",
  "durationMinutes": 30,
  "drowsyCount": 3,
  "sleepCount": 1
}
```

### GET `/api/monitoring/dashboard/realtime-summary`

조직 관리자 대시보드의 실시간 요약을 조회합니다.

Response:

```json
{
  "totalMemberCount": 15,
  "activeSessionCount": 8,
  "warningSessionCount": 2,
  "drowsyWarningSessionCount": 1,
  "sleepWarningSessionCount": 1
}
```

### GET `/api/monitoring/dashboard/realtime-summary/stream`

SSE 실시간 스트림을 구독합니다.

Events:

| Event | Data |
| --- | --- |
| `connected` | `{ "organizationId": "...", "connectedAt": "..." }` |
| `summary` | `MonitoringRealtimeSummaryResponse` |
| `alert` | `MonitoringNotificationResponse` |
| `heartbeat` | `{ "at": "..." }` |

### GET `/api/monitoring/dashboard/hourly-risk-24h`

최근 24시간 시간대별 위험 이벤트 수를 조회합니다.

Response:

```json
{
  "rangeStart": "2026-04-19T11:00:00",
  "rangeEnd": "2026-04-20T10:59:59",
  "buckets": [
    {
      "bucketStart": "2026-04-19T11:00:00",
      "bucketEnd": "2026-04-19T11:59:59",
      "totalRiskCount": 2
    }
  ]
}
```

### GET `/api/monitoring/dashboard/recent-ended-sessions?limit=20`

최근 종료 세션을 조회합니다. `limit`은 1부터 100까지로 정규화됩니다.

Response:

```json
[
  {
    "sessionId": "223456789012345678",
    "userId": "123456789012345678",
    "userName": "홍길동",
    "startedAtApp": "2026-04-20T10:00:00",
    "endedAtApp": "2026-04-20T10:30:00",
    "durationMinutes": 30,
    "drowsyCount": 3,
    "sleepCount": 1,
    "totalRiskCount": 4
  }
]
```

### GET `/api/monitoring/dashboard/notifications?cursor={notificationId}&limit=50`

알림 피드를 cursor pagination으로 조회합니다. `cursor`는 이전 응답의 `nextCursor`를 넣습니다. `limit`은 1부터 200까지로 정규화됩니다.

Response:

```json
{
  "items": [
    {
      "notificationId": "423456789012345678",
      "userId": "123456789012345678",
      "targetUserId": "123456789012345678",
      "userName": "홍길동",
      "type": "SLEEP",
      "content": "홍길동 사용자에게 수면 상태 경고가 감지되었습니다.",
      "occurredAt": "2026-04-20T10:05:13"
    }
  ],
  "nextCursor": "423456789012345678",
  "hasNext": true
}
```

## Organization API

### POST `/api/organizations/members`

조직 관리자가 일반 사용자를 이메일로 조직 구성원에 추가합니다.

Request:

```json
{
  "email": "driver@example.com"
}
```

Response:

```json
{
  "memberId": "523456789012345678",
  "organizationId": "623456789012345678",
  "userId": "123456789012345678",
  "email": "driver@example.com",
  "name": "홍길동",
  "nickname": "길동",
  "role": "USER",
  "createdAt": "2026-04-20T10:00:00"
}
```

### GET `/api/organizations/members`

조직 구성원을 조회합니다.

Response:

```json
[
  {
    "memberId": "523456789012345678",
    "organizationId": "623456789012345678",
    "userId": "123456789012345678",
    "email": "driver@example.com",
    "name": "홍길동",
    "nickname": "길동",
    "role": "USER",
    "createdAt": "2026-04-20T10:00:00"
  }
]
```

### DELETE `/api/organizations/members/{memberId}`

조직 구성원을 삭제합니다.

Response:

```json
{
  "success": true
}
```

### GET `/api/organizations/{organizationId}/risk-users`

조직 위험 사용자를 조회합니다.

Response:

```json
[
  {
    "userId": "123456789012345678",
    "email": "driver@example.com",
    "name": "홍길동",
    "nickname": "길동",
    "totalSessionCount": 10,
    "drowsyCount": 12,
    "sleepCount": 3,
    "totalRiskCount": 15,
    "isMonitoringActive": true
  }
]
```

### GET `/api/organizations/{organizationId}/analysis/risk-stats`

기간별 위험 통계를 조회합니다.

Query params:

| Name | Required | Example | Description |
| --- | --- | --- | --- |
| `granularity` | Yes | `DAY` | `HOUR`, `DAY`, `WEEK`, `MONTH`, `YEAR` 또는 `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `from` | Yes | `2026-04-01` | 시작 날짜 |
| `to` | Yes | `2026-04-20` | 종료 날짜 |

Response:

```json
{
  "granularity": "DAY",
  "from": "2026-04-01",
  "to": "2026-04-20",
  "series": [
    {
      "bucketStart": "2026-04-01T00:00:00",
      "bucketEnd": "2026-04-01T23:59:59",
      "sessionCount": 8,
      "drowsyCount": 10,
      "sleepCount": 2,
      "totalRiskCount": 12
    }
  ],
  "top5Members": [
    {
      "userId": "123456789012345678",
      "name": "홍길동",
      "totalRiskCount": 12
    }
  ]
}
```

## System Admin API

### GET `/api/system-admin/organizations/signups?status=PENDING&query=아이온`

조직 가입 신청 목록을 조회합니다. `SYSTEM_ADMIN` 권한이 필요합니다.

`status` 값:

| Value |
| --- |
| `PENDING` |
| `ACTIVE` |
| `REJECTED` |

Response:

```json
[
  {
    "organizationId": "623456789012345678",
    "organizationCode": "A1B2C3",
    "organizationName": "아이온 운송",
    "businessName": "아이온 운송 주식회사",
    "businessmanNum": "123-45-67890",
    "establishedAt": "2024-01-15",
    "representativeName": "김대표",
    "coRepresentativeName": "박공동",
    "corporateNum": "110111-1234567",
    "businessAddress": "서울특별시 강남구 테헤란로 1",
    "status": "PENDING",
    "subscription": "FREE",
    "requesterEmail": "admin@example.com",
    "rejectReasonCodes": null,
    "rejectReasonDetail": null,
    "createdAt": "2026-04-20T10:00:00"
  }
]
```

### GET `/api/system-admin/organizations/signups/{organizationId}`

조직 가입 신청 상세를 조회합니다.

Response shape is `OrganizationSignupReviewResponse`.

### PATCH `/api/system-admin/organizations/signups/{organizationId}/approve`

조직 가입 신청을 승인합니다. 조직 상태가 `PENDING`일 때만 가능합니다.

Response:

```json
{
  "success": true
}
```

### PATCH `/api/system-admin/organizations/signups/{organizationId}/reject`

조직 가입 신청을 거절합니다.

Request:

```json
{
  "reasonCodes": ["INVALID_BUSINESS_NUMBER", "MISSING_DOCUMENT"],
  "reasonDetail": "사업자 정보 확인이 필요합니다."
}
```

Response:

```json
{
  "success": true
}
```

## Agent API

### GET `/api/agent/config`

AI 동승자 사용 가능 여부와 모드를 조회합니다.

Response for non-subscribed user:

```json
{
  "enabled": false,
  "mode": "PASSIVE",
  "cooldownSeconds": 0
}
```

Response for subscribed user:

```json
{
  "enabled": true,
  "mode": "PROACTIVE",
  "cooldownSeconds": 30
}
```

### POST `/api/agent/chat`

AI 동승자에게 메시지를 보내고 응답을 받습니다. 사용자 또는 소속 조직이 `FREE`가 아닌 구독 상태여야 합니다.

Request:

```json
{
  "message": "졸음이 오는 것 같아. 지금 어떻게 하면 좋을까?",
  "drivingState": "DROWSY"
}
```

`drivingState` 값:

| Value | Alias |
| --- | --- |
| `NORMAL` | - |
| `AWAKE` | - |
| `DROWSY` | - |
| `SLEEP` | `SLEEPING` |

Response:

```json
{
  "reply": "잠시 안전한 곳에 정차하고 환기를 하세요.",
  "source": "gemini"
}
```
