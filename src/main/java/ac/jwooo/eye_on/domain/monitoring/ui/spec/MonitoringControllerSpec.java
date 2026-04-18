package ac.jwooo.eye_on.domain.monitoring.ui.spec;

import ac.jwooo.eye_on.domain.monitoring.application.dto.request.CreateMonitoringEventRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.EndMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.request.StartMonitoringSessionRequest;
import ac.jwooo.eye_on.domain.monitoring.application.dto.response.MonitoringEventResponse;
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

@Tag(name = "Monitoring", description = "모니터링 세션/이벤트 API")
public interface MonitoringControllerSpec {

    @Operation(
            summary = "모니터링 시작",
            description = """
                    **[ 모니터링 시작 API ]**
                    로그인한 사용자 기준으로 모니터링 세션을 시작합니다.
                    
                    ### 📥 입력 (Input)
                    - `Authorization: Bearer {accessToken}` 헤더 (필수)
                    - `mode`: 모니터링 모드 (`DRIVING` | `STUDY`)
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
                    모니터링 세션 중 발생한 상태 변화를 기록합니다.
                    
                    ### 케이스 A) 이벤트 시작
                    - `eventType = DROWSY | SLEEP`
                    - 요청: `occurredAtApp`만 전달 (`eventId`는 보내지 않음)
                    - 응답: 생성된 이벤트 로그 PK(`eventId`) 반환
                    
                    ### 케이스 B) 이벤트 종료(정상 복귀)
                    - `eventType = NORMAL`
                    - 요청: `occurredAtApp + eventId` 전달
                    - 서버는 해당 `eventId`를 종료 처리하고
                      `resolvedAtApp`, `resolvedAtServer`, `durationSeconds`를 업데이트
                    
                    ### duration 계산
                    - `durationSeconds = NORMAL.occurredAtApp - (기존 이벤트 발생시각)`
                    - 단위: 초(second)
                    - 소수점 둘째 자리까지 저장 (예: `6.20`)
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
                                                      "resolvedAtApp": null,
                                                      "resolvedAtServer": null,
                                                      "durationSeconds": null,
                                                      "drowsyCount": 4,
                                                      "sleepCount": 1
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "resolveEventResponseExample",
                                            summary = "NORMAL 종료 응답 예시",
                                            value = """
                                                    {
                                                      "eventId": 223456789012345678,
                                                      "sessionId": 123456789012345678,
                                                      "eventType": "DROWSY",
                                                      "occurredAtApp": "2026-04-18T21:15:10",
                                                      "occurredAtServer": "2026-04-18T21:15:10",
                                                      "resolvedAtApp": "2026-04-18T21:15:16",
                                                      "resolvedAtServer": "2026-04-18T21:15:16",
                                                      "durationSeconds": 6.00,
                                                      "drowsyCount": 4,
                                                      "sleepCount": 1
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 유효하지 않은 시간 범위"),
            @ApiResponse(responseCode = "409", description = "이미 종료된 이벤트를 다시 종료 요청함"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "세션 또는 이벤트 없음")
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
                                                      "occurredAtApp": "2026-04-18T21:15:10",
                                                      "eventId": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "resolveEventExample",
                                            summary = "정상 복귀로 이벤트 종료",
                                            value = """
                                                    {
                                                      "eventType": "NORMAL",
                                                      "occurredAtApp": "2026-04-18T21:15:16",
                                                      "eventId": 223456789012345678
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid CreateMonitoringEventRequest request
    );
}
