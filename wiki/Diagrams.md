# Diagrams

이 문서는 Eye:on Backend의 주요 Usecase와 핵심 Sequence를 Mermaid 다이어그램으로 정리합니다. GitHub Markdown과 대부분의 문서 도구에서 바로 렌더링할 수 있습니다.

## Usecase Diagram

```mermaid
flowchart LR
  AppUser["일반 사용자\nAndroid App"]:::actor
  OrgAdmin["조직 관리자\nAdmin Web"]:::actor
  SysAdmin["시스템 관리자\nAdmin Web"]:::actor
  Gemini["Gemini API"]:::external

  subgraph Backend["Eye:on Backend"]
    UCSignupApp(("APP 회원가입"))
    UCLoginApp(("APP 로그인"))
    UCRefresh(("토큰 재발급"))
    UCLogout(("로그아웃"))
    UCMe(("내 정보 조회"))
    UCPassword(("비밀번호 변경"))

    UCStartSession(("모니터링 세션 시작"))
    UCCreateEvent(("상태 이벤트 전송"))
    UCEndSession(("모니터링 세션 종료"))
    UCAgentConfig(("AI Agent 설정 조회"))
    UCAgentChat(("AI Agent 대화"))

    UCSignupOrg(("조직 관리자 가입 신청"))
    UCLoginWeb(("WEB 관리자 로그인"))
    UCMemberAdd(("조직 구성원 추가"))
    UCMemberList(("조직 구성원 조회"))
    UCMemberRemove(("조직 구성원 삭제"))
    UCRiskUsers(("위험 사용자 조회"))
    UCRiskStats(("기간별 위험 통계 조회"))
    UCDashboard(("대시보드 요약 조회"))
    UCSse(("실시간 SSE 구독"))
    UCNotifications(("알림 피드 조회"))
    UCRecentSessions(("최근 종료 세션 조회"))

    UCReviewList(("조직 가입 신청 목록 조회"))
    UCReviewDetail(("조직 가입 신청 상세 조회"))
    UCApprove(("조직 가입 승인"))
    UCReject(("조직 가입 거절"))
  end

  AppUser --> UCSignupApp
  AppUser --> UCLoginApp
  AppUser --> UCRefresh
  AppUser --> UCLogout
  AppUser --> UCMe
  AppUser --> UCPassword
  AppUser --> UCStartSession
  AppUser --> UCCreateEvent
  AppUser --> UCEndSession
  AppUser --> UCAgentConfig
  AppUser --> UCAgentChat

  OrgAdmin --> UCSignupOrg
  OrgAdmin --> UCLoginWeb
  OrgAdmin --> UCRefresh
  OrgAdmin --> UCLogout
  OrgAdmin --> UCMe
  OrgAdmin --> UCMemberAdd
  OrgAdmin --> UCMemberList
  OrgAdmin --> UCMemberRemove
  OrgAdmin --> UCRiskUsers
  OrgAdmin --> UCRiskStats
  OrgAdmin --> UCDashboard
  OrgAdmin --> UCSse
  OrgAdmin --> UCNotifications
  OrgAdmin --> UCRecentSessions

  SysAdmin --> UCLoginWeb
  SysAdmin --> UCReviewList
  SysAdmin --> UCReviewDetail
  SysAdmin --> UCApprove
  SysAdmin --> UCReject

  UCAgentChat -. "LLM 응답 생성" .-> Gemini
  UCCreateEvent -. "위험 이벤트 발생 시" .-> UCNotifications
  UCCreateEvent -. "ORGANIZATION 모드일 때" .-> UCSse
  UCApprove -. "승인 후" .-> UCLoginWeb
  UCMemberAdd -. "구성원 등록 후" .-> UCStartSession

  classDef actor fill:#f8fafc,stroke:#334155,stroke-width:1.5px,color:#0f172a;
  classDef external fill:#fff7ed,stroke:#f97316,stroke-width:1.5px,color:#9a3412;
```

## Sequence Diagram 1. APP 회원가입/로그인

```mermaid
sequenceDiagram
  participant App as Android App
  participant AuthController as AuthController
  participant AuthService as AuthService
  participant UserRepo as UserRepository
  participant Jwt as JwtTokenProvider
  participant Redis as RedisTokenStore
  participant DB as MySQL

  App->>AuthController: POST /api/auth/signup<br/>X-Client-Type: APP
  AuthController->>AuthService: signup(request, APP)
  AuthService->>UserRepo: existsByEmailAndDeletedAtIsNull(email)
  UserRepo->>DB: select user by email
  DB-->>UserRepo: exists false
  AuthService->>UserRepo: save(User.createGeneralUser)
  UserRepo->>DB: insert users
  DB-->>UserRepo: saved user
  AuthService->>Jwt: createAccessToken(user, APP)
  AuthService->>Jwt: createRefreshToken(user, APP)
  AuthService->>Redis: putRefreshWhitelist(userId, APP, refreshToken, ttl)
  AuthService-->>AuthController: AuthResult
  AuthController-->>App: userId, accessToken, refreshToken, role
```

## Sequence Diagram 2. WEB 조직 관리자 가입 신청과 승인

```mermaid
sequenceDiagram
  participant Admin as Organization Admin Web
  participant AuthController as AuthController
  participant AuthService as AuthService
  participant OrgRepo as OrganizationRepository
  participant UserRepo as UserRepository
  participant SysAdmin as System Admin Web
  participant ReviewController as SystemAdminOrganizationController
  participant ReviewService as OrganizationSignupReviewService
  participant DB as MySQL

  Admin->>AuthController: POST /api/auth/signup<br/>X-Client-Type: WEB
  AuthController->>AuthService: signup(request, WEB)
  AuthService->>OrgRepo: existsByCorporateNumAndStatusIn(PENDING, ACTIVE)
  OrgRepo->>DB: select organization
  DB-->>OrgRepo: no duplicate
  AuthService->>OrgRepo: save(Organization.createPending)
  OrgRepo->>DB: insert organization status PENDING
  AuthService->>UserRepo: save(User.createAdmin)
  UserRepo->>DB: insert admin user
  AuthService-->>AuthController: AuthResult without tokens
  AuthController-->>Admin: userId, accessToken null, refreshToken null, role ADMIN

  SysAdmin->>ReviewController: PATCH /api/system-admin/organizations/signups/{organizationId}/approve
  ReviewController->>ReviewService: approve(systemAdminUserId, organizationId)
  ReviewService->>UserRepo: find reviewer
  UserRepo->>DB: select system admin
  DB-->>UserRepo: SYSTEM_ADMIN user
  ReviewService->>OrgRepo: find organization
  OrgRepo->>DB: select pending organization
  DB-->>OrgRepo: organization PENDING
  ReviewService->>OrgRepo: organization.approve()
  OrgRepo->>DB: update status ACTIVE
  ReviewController-->>SysAdmin: success true

  Admin->>AuthController: POST /api/auth/login<br/>X-Client-Type: WEB
  AuthController->>AuthService: login(request, WEB)
  AuthService->>OrgRepo: validate organization ACTIVE
  AuthService-->>AuthController: AuthResult
  AuthController-->>Admin: accessToken body + refreshToken HttpOnly cookie
```

## Sequence Diagram 3. 모니터링 세션/이벤트 수집과 SSE Push

```mermaid
sequenceDiagram
  participant App as Android App
  participant JwtFilter as JwtAuthenticationFilter
  participant Controller as MonitoringController
  participant Service as MonitoringService
  participant SessionRepo as MonitoringSessionRepository
  participant EventRepo as MonitoringEventLogRepository
  participant NotiRepo as NotificationRepository
  participant Broker as MonitoringRealtimeSseBroker
  participant Web as Admin Web SSE
  participant DB as MySQL

  App->>JwtFilter: Authorization Bearer accessToken
  JwtFilter-->>Controller: AuthenticatedUser(userId)

  App->>Controller: POST /api/monitoring/sessions/start
  Controller->>Service: startSession(userId, request)
  Service->>SessionRepo: find active sessions
  SessionRepo->>DB: select active sessions
  DB-->>SessionRepo: active sessions
  alt active session exists
    Service->>SessionRepo: end old active sessions
    SessionRepo->>DB: update endedAtApp, endedAtServer
  end
  Service->>SessionRepo: save new monitoring session
  SessionRepo->>DB: insert monitoring_sessions
  Service-->>Controller: MonitoringSessionStartResponse
  Controller-->>App: sessionId, startedAt, counts

  Web->>Controller: GET /api/monitoring/dashboard/realtime-summary/stream
  Controller->>Service: subscribeRealtimeSummary(adminUserId)
  Service->>Broker: connect(organizationId)
  Broker-->>Web: event connected
  Service->>Broker: sendSummary(current summary)
  Broker-->>Web: event summary

  App->>Controller: POST /api/monitoring/sessions/{sessionId}/events
  Controller->>Service: createEvent(userId, sessionId, request)
  Service->>SessionRepo: find owned session
  SessionRepo->>DB: select monitoring session
  Service->>EventRepo: save MonitoringEventLog
  EventRepo->>DB: insert monitoring_event_logs
  alt eventType is DROWSY or SLEEP and mode is ORGANIZATION
    Service->>SessionRepo: increase risk count
    SessionRepo->>DB: update drowsyCount or sleepCount
    Service->>NotiRepo: save Notification
    NotiRepo->>DB: insert notification
  end
  Service-->>Controller: MonitoringEventResponse
  Controller-->>App: eventId, sessionId, counts
  Service->>Broker: afterCommit sendSummary
  Broker-->>Web: event summary
  alt risk notification created
    Service->>Broker: afterCommit sendAlert
    Broker-->>Web: event alert
  end
```

## Sequence Diagram 4. 관리자 대시보드 조회

```mermaid
sequenceDiagram
  participant Web as Admin Web
  participant JwtFilter as JwtAuthenticationFilter
  participant Controller as MonitoringController
  participant Service as MonitoringService
  participant AccessService as OrganizationAccessService
  participant SessionRepo as MonitoringSessionRepository
  participant EventRepo as MonitoringEventLogRepository
  participant NotiRepo as NotificationRepository
  participant DB as MySQL

  Web->>JwtFilter: Authorization Bearer accessToken
  JwtFilter-->>Controller: AuthenticatedUser(adminUserId)

  Web->>Controller: GET /api/monitoring/dashboard/realtime-summary
  Controller->>Service: getRealtimeSummary(adminUserId)
  Service->>AccessService: resolveOwnedOrganization(adminUserId)
  AccessService->>DB: select admin and organization
  DB-->>AccessService: organization
  Service->>SessionRepo: findRealtimeSummaryByOrganizationId
  SessionRepo->>DB: aggregate active and warning sessions
  DB-->>SessionRepo: summary projection
  Service-->>Controller: MonitoringRealtimeSummaryResponse
  Controller-->>Web: summary JSON

  Web->>Controller: GET /api/monitoring/dashboard/notifications?cursor=&limit=50
  Controller->>Service: getRecentNotifications(adminUserId, cursor, limit)
  Service->>AccessService: resolveOwnedOrganization(adminUserId)
  Service->>NotiRepo: findRecentByOrganizationIdWithCursor
  NotiRepo->>DB: select notifications with cursor
  DB-->>NotiRepo: notification projection list
  Service-->>Controller: MonitoringNotificationPageResponse
  Controller-->>Web: items, nextCursor, hasNext

  Web->>Controller: GET /api/monitoring/dashboard/hourly-risk-24h
  Controller->>Service: getHourlyRisk24h(adminUserId)
  Service->>EventRepo: findHourlyRiskCountsByOrganizationAndRange
  EventRepo->>DB: group risk events by hour
  DB-->>EventRepo: hourly buckets
  Service-->>Controller: MonitoringHourlyRisk24hResponse
  Controller-->>Web: 24h risk buckets
```

## Sequence Diagram 5. 조직 구성원 추가 후 조직 모니터링 시작

```mermaid
sequenceDiagram
  participant Admin as Organization Admin Web
  participant MemberController as OrganizationMemberController
  participant AddMemberUseCase as AddOrganizationMemberUseCase
  participant AccessService as OrganizationAccessService
  participant MemberService as OrganizationMemberService
  participant UserService as OrganizationMemberUserService
  participant MemberRepo as OrganizationMemberRepository
  participant User as Android App User
  participant MonitoringController as MonitoringController
  participant MonitoringService as MonitoringService
  participant DB as MySQL

  Admin->>MemberController: POST /api/organizations/members
  MemberController->>AddMemberUseCase: execute(adminUserId, email)
  AddMemberUseCase->>AccessService: resolveOwnedOrganization(adminUserId)
  AccessService->>DB: validate ADMIN and organization
  AddMemberUseCase->>UserService: find user by email
  UserService->>DB: select USER by email
  DB-->>UserService: target user
  AddMemberUseCase->>MemberService: add member
  MemberService->>MemberRepo: save organization member
  MemberRepo->>DB: insert member
  AddMemberUseCase-->>MemberController: OrganizationMemberResponse
  MemberController-->>Admin: member info

  User->>MonitoringController: POST /api/monitoring/sessions/start<br/>mode ORGANIZATION
  MonitoringController->>MonitoringService: startSession(userId, request)
  MonitoringService->>MemberRepo: findFirstByUserIdAndDeletedAtIsNull
  MemberRepo->>DB: select member by userId
  DB-->>MemberRepo: organizationId
  MonitoringService->>DB: insert monitoring_sessions with organizationId
  MonitoringService-->>MonitoringController: MonitoringSessionStartResponse
  MonitoringController-->>User: sessionId
```

## Sequence Diagram 6. AI Agent 대화

```mermaid
sequenceDiagram
  participant App as Android App
  participant Controller as AgentController
  participant UseCase as ChatWithAgentUseCase
  participant SubscriptionService as AgentSubscriptionService
  participant UserRepo as UserRepository
  participant OrgRepo as OrganizationRepository
  participant GeminiClient as GeminiAgentClient
  participant Gemini as Gemini API
  participant DB as MySQL

  App->>Controller: POST /api/agent/chat
  Controller->>UseCase: execute(userId, request)
  UseCase->>SubscriptionService: canUseAgent(userId)
  SubscriptionService->>UserRepo: find user
  UserRepo->>DB: select user
  DB-->>UserRepo: user subscription
  alt user subscription is FREE and organization exists
    SubscriptionService->>OrgRepo: find organization
    OrgRepo->>DB: select organization
    DB-->>OrgRepo: organization subscription
  end
  alt can use agent
    UseCase->>GeminiClient: generateReply(drivingState, message)
    GeminiClient->>Gemini: request content generation
    Gemini-->>GeminiClient: reply text
    GeminiClient-->>UseCase: GeminiAgentReply
    UseCase-->>Controller: AgentChatResponse
    Controller-->>App: reply, source
  else subscription required
    UseCase-->>Controller: AGENT_SUBSCRIPTION_REQUIRED
    Controller-->>App: 403 error response
  end
```
