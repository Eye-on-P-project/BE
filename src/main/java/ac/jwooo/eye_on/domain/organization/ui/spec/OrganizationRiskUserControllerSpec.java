package ac.jwooo.eye_on.domain.organization.ui.spec;

import java.time.LocalDate;
import java.util.List;

import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationRiskStatsResponse;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationRiskUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Organization Risk", description = "조직 위험 사용자 집계 API")
public interface OrganizationRiskUserControllerSpec {

    @Operation(
            summary = "조직 위험 사용자 목록 조회",
            description = """
                    지정한 조직의 구성원을 대상으로,
                    `ORGANIZATION` 모드 모니터링 세션의 졸음/수면 누적 건수를 합산하여 내림차순으로 조회합니다.
                    
                    정렬 기준:
                    1) totalRiskCount (sleep + drowsy) DESC
                    2) sleepCount DESC
                    3) drowsyCount DESC
                    4) totalSessionCount DESC
                    5) userId ASC
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = OrganizationRiskUserResponse.class),
                            examples = @ExampleObject(
                                    name = "organizationRiskUsersResponseExample",
                                    summary = "위험 사용자 목록 예시",
                                    value = """
                                            [
                                              {
                                                "userId": "300010020000000005",
                                                "email": "user1@example.com",
                                                "name": "홍길동",
                                                "nickname": "hong",
                                                "totalSessionCount": 18,
                                                "drowsyCount": 12,
                                                "sleepCount": 6,
                                                "totalRiskCount": 18
                                              },
                                              {
                                                "userId": "300010020000000011",
                                                "email": "user2@example.com",
                                                "name": "김철수",
                                                "nickname": "chulsoo",
                                                "totalSessionCount": 14,
                                                "drowsyCount": 8,
                                                "sleepCount": 3,
                                                "totalRiskCount": 11
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "조직 관리자 권한 없음 또는 해당 조직 접근 불가"),
            @ApiResponse(responseCode = "404", description = "조직 정보를 찾을 수 없음")
    })
    List<OrganizationRiskUserResponse> getRiskUsers(
            Authentication authentication,
            @Parameter(
                    name = "organizationId",
                    description = "조회할 조직의 식별값(PK)",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "integer", format = "int64", example = "400010020000000001")
            )
            Long organizationId
    );

    @Operation(
            summary = "조직 위험 통계 조회 (분석 탭)",
            description = """
                    지정한 기간(`from` ~ `to`)의 조직 위험 통계를 조회합니다.
                    
                    - granularity:
                      - `HOUR`: 시간 단위 버킷
                      - `DAY`: 일 단위 버킷
                      - `WEEK`: from 기준 7일 단위 버킷
                      - `MONTH`: from 기준 1개월 단위 버킷
                      - `YEAR`: from 기준 1년 단위 버킷
                    - 집계 대상: `ORGANIZATION` 모드 세션만 포함
                    - sessionCount 집계 기준: `monitoring_sessions.startedAtApp`
                    - series 항목: `sessionCount`, `drowsyCount`, `sleepCount`, `totalRiskCount`
                    - top5Members: 기간 내 `drowsy + sleep` 기준 상위 5명
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = OrganizationRiskStatsResponse.class),
                            examples = @ExampleObject(
                                    name = "organizationRiskStatsExample",
                                    summary = "분석 통계 응답 예시",
                                    value = """
                                            {
                                              "granularity": "WEEK",
                                              "from": "2026-04-01",
                                              "to": "2026-04-19",
                                              "series": [
                                                {
                                                  "bucketStart": "2026-04-01T00:00:00",
                                                  "bucketEnd": "2026-04-07T23:59:59",
                                                  "sessionCount": 15,
                                                  "drowsyCount": 9,
                                                  "sleepCount": 2,
                                                  "totalRiskCount": 11
                                                }
                                              ],
                                              "top5Members": [
                                                {
                                                  "userId": "300010020000000001",
                                                  "name": "홍길동",
                                                  "totalRiskCount": 8
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "조직 관리자 권한 없음 또는 해당 조직 접근 불가"),
            @ApiResponse(responseCode = "404", description = "조직 정보를 찾을 수 없음")
    })
    OrganizationRiskStatsResponse getRiskStats(
            Authentication authentication,
            @Parameter(
                    name = "organizationId",
                    description = "조회할 조직의 식별값(PK)",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "integer", format = "int64", example = "400010020000000001")
            )
            Long organizationId,
            @RequestParam
            @Parameter(
                    name = "granularity",
                    description = "버킷 단위 (HOUR, DAY, WEEK, MONTH, YEAR)",
                    required = true,
                    schema = @Schema(type = "string", example = "DAY")
            )
            String granularity,
            @RequestParam
            @Parameter(
                    name = "from",
                    description = "시작일 (yyyy-MM-dd)",
                    required = true,
                    schema = @Schema(type = "string", format = "date", example = "2026-04-12")
            )
            LocalDate from,
            @RequestParam
            @Parameter(
                    name = "to",
                    description = "종료일 (yyyy-MM-dd, 포함)",
                    required = true,
                    schema = @Schema(type = "string", format = "date", example = "2026-04-15")
            )
            LocalDate to
    );
}
