package ac.jwooo.eye_on.domain.organization.ui.spec;

import java.util.List;

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

@Tag(name = "Organization Risk", description = "조직 위험 사용자 집계 API")
public interface OrganizationRiskUserControllerSpec {

    @Operation(
            summary = "조직 위험 사용자 목록 조회",
            description = """
                    지정한 조직의 구성원을 대상으로,
                    모든 모니터링 세션의 졸음/수면 누적 건수를 합산하여 내림차순으로 조회합니다.
                    
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
}
