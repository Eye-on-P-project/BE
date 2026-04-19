package ac.jwooo.eye_on.domain.organization.ui.spec;

import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.organization.application.dto.request.AddOrganizationMemberRequest;
import ac.jwooo.eye_on.domain.organization.application.dto.response.OrganizationMemberResponse;
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

@Tag(name = "Organization Member", description = "조직 구성원 관리 API")
public interface OrganizationMemberControllerSpec {

    @Operation(
            summary = "조직 구성원 추가",
            description = """
                    조직 관리자가 이메일 기준으로 기존 사용자를 조직 구성원(member 테이블)으로 추가합니다.
                    
                    **요청 데이터**:
                    - `email`: 추가할 리소스 사용자의 이메일
                    
                    **동작 방식**:
                    - 요청받은 `email`로 시스템 내 사용자를 검색합니다.
                    - 조직 ID는 클라이언트로부터 직접 받지 않고, 현재 로그인한 관리자 계정의 `organization_code`를 기준으로 조직 PK를 찾아 매핑합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "구성원 추가 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationMemberResponse.class),
                            examples = @ExampleObject(
                                    name = "addOrganizationMemberResponseExample",
                                    summary = "구성원 추가 성공 응답",
                                    value = """
                                            {
                                              "memberId": "500010020000000001",
                                              "organizationId": "400010020000000001",
                                              "userId": "300010020000000001",
                                              "email": "user@example.com",
                                              "name": "홍길동",
                                              "nickname": "hong",
                                              "role": "USER",
                                              "createdAt": "2026-04-18T12:00:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않음 (ex: 유효하지 않은 이메일 형식)"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (조직의 관리자가 아님)"),
            @ApiResponse(responseCode = "404", description = "해당 이메일을 가진 계정이 존재하지 않거나, 진행 중인 조직 정보가 존재하지 않음"),
            @ApiResponse(responseCode = "409", description = "이미 해당 조직에 포함된 구성원임")
    })
    OrganizationMemberResponse addMember(
            Authentication authentication,
            @RequestBody(
                    description = "조직에 추가할 사용자의 이메일 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddOrganizationMemberRequest.class),
                            examples = @ExampleObject(
                                    name = "addOrganizationMemberExample",
                                    summary = "구성원 추가 요청 예시",
                                    value = """
                                            {
                                              "email": "user@example.com"
                                            }
                                            """
                            )
                    )
            )
            @Valid AddOrganizationMemberRequest request
    );

    @Operation(
            summary = "조직 구성원 목록 조회",
            description = """
                    현재 로그인한 관리자에게 속한 조직의 모든 구성원 목록을 조회합니다.
                    
                    **동작 방식**:
                    - 관리자(Admin) 계정의 `organization_code`를 기준으로 조직 PK를 찾습니다.
                    - 해당 조직에 속해있으며 **삭제되지 않은 구성원(소프트 딜리트 필터링)** 들을 생성일(가입일) 내림차순으로 반환합니다.
                    - 각 구성원별 응답에서는 회원 PK, 이름, 역할, 이메일 등의 상세 정보가 포함됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "구성원 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationMemberResponse.class),
                            examples = @ExampleObject(
                                    name = "getOrganizationMembersResponseExample",
                                    summary = "구성원 목록 조회 성공 응답",
                                    value = """
                                            [
                                              {
                                                "memberId": "500010020000000001",
                                                "organizationId": "400010020000000001",
                                                "userId": "300010020000000001",
                                                "email": "admin@example.com",
                                                "name": "김관리",
                                                "nickname": "admin_kim",
                                                "role": "ADMIN",
                                                "createdAt": "2026-04-10T10:00:00"
                                              },
                                              {
                                                "memberId": "500010020000000002",
                                                "organizationId": "400010020000000001",
                                                "userId": "300010020000000005",
                                                "email": "user@example.com",
                                                "name": "홍길동",
                                                "nickname": "hong",
                                                "role": "USER",
                                                "createdAt": "2026-04-18T12:00:00"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (조직의 관리자가 아님)"),
            @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    List<OrganizationMemberResponse> getMembers(Authentication authentication);

    @Operation(
            summary = "조직 구성원 삭제",
            description = """
                    조직 내의 특정 구성원을 삭제(Soft Delete)합니다.
                    
                    **동작 방식**:
                    - 요청 시 제시된 `memberId` (구성원의 PK)를 기준으로 삭제 작업을 수행합니다.
                    - 조직 ID는 관리자의 `organization_code`로부터 알아내며, 다른 조직의 구성원은 지울 수 없습니다.
                    - 성공 시 `success: true` 형태의 JSON을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "구성원 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(
                                    name = "removeOrganizationMemberResponseExample",
                                    summary = "구성원 삭제 성공 응답",
                                    value = """
                                            {
                                              "success": true
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (조직의 관리자가 아님)"),
            @ApiResponse(responseCode = "404", description = "조직 정보, 또는 삭제하려는 구성원을 찾을 수 없음")
    })
    Map<String, Object> removeMember(
            Authentication authentication,
            @Parameter(
                    name = "memberId",
                    description = "삭제할 구성원의 식별값(PK). 목록 조회 API 등에서 얻은 memberId 값을 사용하세요.",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "integer", format = "int64", example = "500010020000000001")
            )
            Long memberId
    );
}
