package ac.jwooo.eye_on.domain.monitoring.ui.spec;

import java.util.List;

import ac.jwooo.eye_on.domain.monitoring.application.dto.request.CreateMonitoringEventRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.EndMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.StartMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringEventResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringHourlyRisk24hResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringNotificationPageResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRecentEndedSessionResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringRealtimeSummaryResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionEndResponse;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringSessionStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Monitoring", description = "모니터링 세션/이벤트 API")
public interface MonitoringControllerSpec {

    @Operation(
            summary = "대시보드 실시간 요약 조회",
            description = """
                    **[ 대시보드 실시간 요약 API ]**
                    관리자 기준 소속 조직의 실시간 위젯 요약 값을 조회합니다.
                    
                    ### 📥 입력 (Input)
                    - `Authorization: Bearer {accessToken}` 헤더 (필수, 관리자 계정)
                    
                    ### 📤 출력 (Output)
                    - `totalMemberCount`: 조직 내 활성 구성원 수
                    - `activeSessionCount`: 현재 종료되지 않은 **ORGANIZATION 모드** 세션 수
                    - `warningSessionCount`: 활성 ORGANIZATION 세션 중 최신 상태가 졸음/수면인 세션 수
                    - `drowsyWarningSessionCount`: 활성 ORGANIZATION 세션 중 최신 상태가 졸음인 세션 수
                    - `sleepWarningSessionCount`: 활성 ORGANIZATION 세션 중 최신 상태가 수면인 세션 수
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "실시간 요약 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = MonitoringRealtimeSummaryResponse.class),
                            examples = @ExampleObject(
                                    name = "monitoringRealtimeSummaryResponseExample",
                                    summary = "실시간 요약 응답 예시",
                                    value = """
                                            {
                                              "totalMemberCount": 25,
                                              "activeSessionCount": 13,
                                              "warningSessionCount": 4,
                                              "drowsyWarningSessionCount": 3,
                                              "sleepWarningSessionCount": 2
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "조직 정보를 찾을 수 없음")
    })
    MonitoringRealtimeSummaryResponse getRealtimeSummary(Authentication authentication);

    @Operation(
            summary = "대시보드 실시간 요약 SSE 구독",
            description = """
                    **[ 대시보드 실시간 요약 SSE API ]**
                    관리자 기준 소속 조직의 실시간 요약 변화를 SSE로 구독합니다.
                    
                    ### 📥 입력 (Input)
                    - `Authorization: Bearer {accessToken}` 헤더 (필수, 관리자 계정)
                    
                    ### 📤 이벤트 (Output)
                    - `connected`: 연결 직후
                    - `summary`: 최신 요약 값 (`MonitoringRealtimeSummaryResponse`)
                    - `alert`: `NORMAL`/`DROWSY`/`SLEEP` 알림 (`MonitoringNotificationResponse`)
                    - `heartbeat`: 연결 유지용 하트비트
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "조직 정보를 찾을 수 없음")
    })
    SseEmitter subscribeRealtimeSummary(Authentication authentication);

    @Operation(
            summary = "대시보드 최근 24시간 시간대별 위험 건수 조회",
            description = """
                    **[ 최근 24시간 위험 시간대 집계 API ]**
                    관리자 기준 소속 조직의 모든 구성원/모든 세션을 대상으로
                    현재 시각 기준 최근 24시간의 시간대(1시간)별 `졸음 + 수면` 발생 건수를 조회합니다.
                    집계 대상은 `ORGANIZATION` 모드 세션만 포함합니다.
                    
                    - 이벤트 시각 기준: `occurredAtApp`
                    - 포함 이벤트: `DROWSY`, `SLEEP`
                    - 진행 중 세션 이벤트 포함
                    - 결과는 24개 버킷(0건 포함)으로 고정 반환
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = MonitoringHourlyRisk24hResponse.class),
                            examples = @ExampleObject(
                                    name = "hourlyRisk24hResponseExample",
                                    summary = "최근 24시간 시간대별 위험 건수",
                                    value = """
                                            {
                                              "rangeStart": "2026-04-18T13:00:00",
                                              "rangeEnd": "2026-04-19T12:59:59",
                                              "buckets": [
                                                {
                                                  "bucketStart": "2026-04-19T11:00:00",
                                                  "bucketEnd": "2026-04-19T11:59:59",
                                                  "totalRiskCount": 4
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    MonitoringHourlyRisk24hResponse getHourlyRisk24h(Authentication authentication);

    @Operation(
            summary = "대시보드 최근 종료 세션 조회",
            description = """
                    **[ 최근 접속 세션 위젯 API ]**
                    관리자 기준 소속 조직의 `ORGANIZATION` 모드 세션 중
                    **현재 활성 세션(미종료)을 제외한 종료 세션만** 최신 순으로 조회합니다.
                    
                    - 정렬: `endedAtServer DESC`, `sessionId DESC`
                    - 기본 조회 개수: `20`
                    - 최대 조회 개수: `100` (요청값이 커도 서버에서 100으로 제한)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = MonitoringRecentEndedSessionResponse.class),
                            examples = @ExampleObject(
                                    name = "recentEndedSessionsExample",
                                    summary = "최근 종료 세션 응답 예시",
                                    value = """
                                            [
                                              {
                                                "sessionId": "123456789012345678",
                                                "userId": "987654321012345678",
                                                "userName": "홍길동",
                                                "startedAtApp": "2026-04-18T08:20:00",
                                                "endedAtApp": "2026-04-18T09:05:20",
                                                "durationMinutes": 45,
                                                "drowsyCount": 3,
                                                "sleepCount": 1,
                                                "totalRiskCount": 4
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "조직 정보를 찾을 수 없음")
    })
    List<MonitoringRecentEndedSessionResponse> getRecentEndedSessions(
            Authentication authentication,
            @RequestParam
            @Parameter(
                    name = "limit",
                    description = "조회 개수 (기본값: 20, 최대 100)",
                    required = false,
                    schema = @Schema(type = "integer", format = "int32", defaultValue = "20", example = "20")
            )
            int limit
    );

    @Operation(
            summary = "대시보드 최근 알림 조회",
            description = """
                    **[ 실시간 알림 기록 API ]**
                    관리자 기준 **소속 조직 전체 알림**을 **커서 기반**으로 조회합니다.
                    알림은 `DROWSY` / `SLEEP` 이벤트 발생 시 자동 저장됩니다.
                    
                    - 정렬: `notificationId DESC`
                    - `cursor` 미지정: 최신 페이지
                    - `cursor` 지정: 해당 ID보다 작은 알림부터 조회
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = MonitoringNotificationPageResponse.class),
                            examples = @ExampleObject(
                                    name = "recentNotificationsExample",
                                    summary = "최근 알림 응답 예시",
                                    value = """
                                            {
                                              "items": [
                                                {
                                                  "notificationId": "123456789012345678",
                                                  "userId": "223456789012345678",
                                                  "targetUserId": "223456789012345678",
                                                  "userName": "홍길동",
                                                  "type": "DROWSY",
                                                  "content": "홍길동 사용자에게 졸음 의심 알림이 감지되었습니다.",
                                                  "occurredAt": "2026-04-19T13:20:30"
                                                }
                                              ],
                                              "nextCursor": "123456789012345678",
                                              "hasNext": true
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    MonitoringNotificationPageResponse getRecentNotifications(
            Authentication authentication,
            @RequestParam
            @Parameter(
                    name = "cursor",
                    description = "다음 페이지 조회 시작 커서(notificationId). 없으면 최신부터 조회",
                    required = false,
                    schema = @Schema(type = "integer", format = "int64", example = "123456789012345678")
            )
            Long cursor,
            @RequestParam
            @Parameter(
                    name = "limit",
                    description = "조회 개수 (기본값: 50, 최대 200)",
                    required = false,
                    schema = @Schema(type = "integer", format = "int32", defaultValue = "50", example = "50")
            )
            int limit
    );

    @Operation(
            summary = "모니터링 시작",
            description = """
                    **[ 모니터링 시작 API ]**
                    로그인한 사용자 기준으로 모니터링 세션을 시작합니다.
                    
                    ### 📥 입력 (Input)
                    - `Authorization: Bearer {accessToken}` 헤더 (필수)
                    - `mode`: 모니터링 모드 (`ORGANIZATION` | `DRIVING` | `STUDY`)
                    - `startedAtApp`: 앱에서 측정한 시작 시각 (`yyyy-MM-dd'T'HH:mm:ss`)
                    
                    ### ⚠️ 동작 규칙
                    - 사용자별로 **동시에 1개의 활성 세션만 허용**됩니다.
                    - 이미 종료되지 않은 세션이 있으면 `409 MONITORING_SESSION_ALREADY_ACTIVE`를 반환합니다.
                    
                    ### 📤 출력 (Output)
                    - `sessionId`: 생성된 모니터링 세션 PK
                    - `userId`: JWT에서 추출한 사용자 PK
                    - `startedAtApp`: 앱 시작 시각
                    - `startedAtServer`: 서버 수신 시각
                    - `drowsyCount`, `sleepCount`: 초기값 `0`
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "모니터링 시작 성공",
                    content = @Content(
                            schema = @Schema(implementation = MonitoringSessionStartResponse.class),
                            examples = @ExampleObject(
                                    name = "startMonitoringResponseExample",
                                    summary = "시작 성공 응답 예시",
                                    value = """
                                            {
                                              "sessionId": 123456789012345678,
                                              "userId": 987654321012345678,
                                              "mode": "DRIVING",
                                              "startedAtApp": "2026-04-18T08:20:00",
                                              "startedAtServer": "2026-04-18T08:20:02",
                                              "drowsyCount": 0,
                                              "sleepCount": 0
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 필드 누락/형식 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "409", description = "이미 진행 중인 세션이 존재함")
    })
    MonitoringSessionStartResponse startMonitoring(
            Authentication authentication,
            @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = StartMonitoringSessionRequest.class),
                            examples = @ExampleObject(
                                    name = "startMonitoringRequestExample",
                                    summary = "모니터링 시작 요청 예시",
                                    value = """
                                            {
                                              "mode": "DRIVING",
                                              "startedAtApp": "2026-04-18T08:20:00"
                                            }
                                            """
                            )
                    )
            )
            @Valid StartMonitoringSessionRequest request
    );

    @Operation(
            summary = "모니터링 종료",
            description = """
                    **[ 모니터링 종료 API ]**
                    지정한 모니터링 세션을 종료합니다.
                    
                    ### 📥 입력 (Input)
                    - `Authorization: Bearer {accessToken}` 헤더 (필수)
                    - `sessionId` (Path): 종료할 세션 PK
                    - `endedAtApp`: 앱에서 측정한 종료 시각 (`yyyy-MM-dd'T'HH:mm:ss`)
                    
                    ### ⚠️ 동작 규칙
                    - 본인 세션만 종료할 수 있습니다.
                    - 이미 종료된 세션이면 `409 MONITORING_SESSION_ALREADY_ENDED`
                    - `endedAtApp < startedAtApp`이면 `400 INVALID_MONITORING_TIME_RANGE`
                    
                    ### 📤 출력 (Output)
                    - `endedAtServer`: 서버 수신 시각
                    - `durationMinutes`: `startedAtApp ~ endedAtApp` 차이를 **분 단위**로 저장
                    - `drowsyCount`, `sleepCount`: 누적 이벤트 카운트
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "모니터링 종료 성공",
                    content = @Content(
                            schema = @Schema(implementation = MonitoringSessionEndResponse.class),
                            examples = @ExampleObject(
                                    name = "endMonitoringResponseExample",
                                    summary = "모니터링 종료 응답 예시",
                                    value = """
                                            {
                                              "sessionId": 123456789012345678,
                                              "userId": 987654321012345678,
                                              "mode": "DRIVING",
                                              "startedAtApp": "2026-04-18T08:20:00",
                                              "startedAtServer": "2026-04-18T08:20:02",
                                              "endedAtApp": "2026-04-18T09:05:20",
                                              "endedAtServer": "2026-04-18T09:05:21",
                                              "durationMinutes": 45,
                                              "drowsyCount": 3,
                                              "sleepCount": 1
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 시간 범위"),
            @ApiResponse(responseCode = "409", description = "이미 종료된 세션"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "세션 없음")
    })
    MonitoringSessionEndResponse endMonitoring(
            Authentication authentication,
            @Parameter(
                    name = "sessionId",
                    description = "종료할 모니터링 세션 PK",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "integer", format = "int64", example = "123456789012345678")
            )
            Long sessionId,
            @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = EndMonitoringSessionRequest.class),
                            examples = @ExampleObject(
                                    name = "endMonitoringRequestExample",
                                    summary = "모니터링 종료 요청 예시",
                                    value = """
                                            {
                                              "endedAtApp": "2026-04-18T09:05:20"
                                            }
                                            """
                            )
                    )
            )
            @Valid EndMonitoringSessionRequest request
    );

    @Operation(
            summary = "졸음/수면 이벤트 기록",
            description = """
                    **[ 상태 변화 이벤트 API ]**
                    모니터링 세션 중 발생한 상태 변화(`DROWSY`, `SLEEP`, `NORMAL`)를 시간순 로그로 기록합니다.
                    
                    ### 동작 규칙
                    - 이벤트는 append-only 방식으로 저장됩니다.
                    - `eventId`를 이용한 기존 로그 수정/종료 처리는 하지 않습니다.
                    - 같은 세션에서 새 이벤트 시각(`occurredAtApp`)은 이전 이벤트보다 빠를 수 없습니다.
                    
                    ### duration 해석 방식
                    - duration은 저장값이 아니라, 같은 `sessionId`의 연속 로그 시각 차이로 해석합니다.
                    - 예: `12:00 DROWSY` -> `12:01 SLEEP`이면 DROWSY 지속시간은 1분입니다.
                    - 예: `12:01 SLEEP` -> `12:02 NORMAL`이면 SLEEP 지속시간은 1분입니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이벤트 저장 성공",
                    content = @Content(
                            schema = @Schema(implementation = MonitoringEventResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "startEventResponseExample",
                                            summary = "DROWSY/SLEEP 시작 응답 예시",
                                            value = """
                                                    {
                                                      "eventId": 223456789012345678,
                                                      "sessionId": 123456789012345678,
                                                      "eventType": "DROWSY",
                                                      "occurredAtApp": "2026-04-18T21:15:10",
                                                      "occurredAtServer": "2026-04-18T21:15:10",
                                                      "drowsyCount": 4,
                                                      "sleepCount": 1
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "normalEventResponseExample",
                                            summary = "NORMAL 상태 변화 응답 예시",
                                            value = """
                                                    {
                                                      "eventId": 223456789012345679,
                                                      "sessionId": 123456789012345678,
                                                      "eventType": "NORMAL",
                                                      "occurredAtApp": "2026-04-18T21:15:16",
                                                      "occurredAtServer": "2026-04-18T21:15:16",
                                                      "drowsyCount": 4,
                                                      "sleepCount": 1
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 유효하지 않은 시간 범위"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "세션 없음")
    })
    MonitoringEventResponse createMonitoringEvent(
            Authentication authentication,
            @Parameter(
                    name = "sessionId",
                    description = "이벤트를 기록할 모니터링 세션 PK",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "integer", format = "int64", example = "123456789012345678")
            )
            Long sessionId,
            @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateMonitoringEventRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "startEventExample",
                                            summary = "졸음/수면 이벤트 시작",
                                            value = """
                                                    {
                                                      "eventType": "DROWSY",
                                                      "occurredAtApp": "2026-04-18T21:15:10"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "normalEventExample",
                                            summary = "정상 상태 이벤트 기록",
                                            value = """
                                                    {
                                                      "eventType": "NORMAL",
                                                      "occurredAtApp": "2026-04-18T21:15:16"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid CreateMonitoringEventRequest request
    );
}
