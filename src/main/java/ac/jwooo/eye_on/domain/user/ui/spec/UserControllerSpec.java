package ac.jwooo.eye_on.domain.user.ui.spec;

import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.user.application.dto.request.CreateOrganizationRecordRequest;
import ac.jwooo.eye_on.domain.user.application.dto.response.MeResponse;
import ac.jwooo.eye_on.domain.user.application.dto.response.OrganizationRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "User", description = "사용자/조직 API")
public interface UserControllerSpec {

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인된 사용자의 정보를 반환합니다. Authorization 헤더에 Bearer 토큰이 필요합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = MeResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청 (토큰 없음 또는 만료)")
    })
    MeResponse me(Authentication authentication);

    @Operation(
            summary = "개발용 조직 레코드 생성",
            description = "개발 환경에서 사용할 조직 코드를 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조직 레코드 생성 성공",
                    content = @Content(schema = @Schema(implementation = OrganizationRecordResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 조직 코드")
    })
    OrganizationRecordResponse createOrganizationRecord(
            @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateOrganizationRecordRequest.class),
                            examples = @ExampleObject(
                                    name = "createOrganizationRecordExample",
                                    summary = "조직 레코드 생성 예시",
                                    value = """
                                            {
                                              "code": "ORG001",
                                              "description": "개발용 조직"
                                            }
                                            """
                            )
                    )
            )
            @Valid CreateOrganizationRecordRequest request
    );

    @Operation(
            summary = "개발용 전체 조직 레코드 조회",
            description = "삭제되지 않은 전체 조직 레코드를 생성일 내림차순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조직 레코드 전체 조회 성공",
                    content = @Content(schema = @Schema(implementation = OrganizationRecordResponse.class))
            )
    })
    List<OrganizationRecordResponse> getOrganizationRecords();

    @Operation(
            summary = "개발용 조직 레코드 삭제",
            description = "조직 레코드 ID로 soft delete 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조직 레코드 삭제 성공",
                    content = @Content(
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(value = "{\"success\": true}")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "조직 레코드를 찾을 수 없음")
    })
    Map<String, Object> deleteOrganizationRecord(Long organizationRecordId);
}
