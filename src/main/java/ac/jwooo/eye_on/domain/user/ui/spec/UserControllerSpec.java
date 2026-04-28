package ac.jwooo.eye_on.domain.user.ui.spec;

import java.util.List;
import java.util.Map;

import ac.jwooo.eye_on.domain.user.application.dto.request.ChangePasswordRequest;
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
            description = """
                    **[ 내 정보 조회 API ]**
                    현재 접속 유저의 프로필, 소속 조직, 역할 등의 상세 정보를 조회합니다.
                    
                    ### 📥 **입력 (Input)**
                    - `Authorization` 헤더: `Bearer <accessToken>` (필수)
                    
                    ### 📤 **출력 (Output)**
                    - `userId`: 사용자의 고유 ID
                    - `email`: 가입된 이메일 주소
                    - `role`: 사용자 권한 (예: ROLE_USER)
                    - `organizationCode`: 속한 조직 코드 (없는 경우 null)
                    - `name`: 사용자의 본명
                    - `nickname`: 서비스 내 표시 닉네임
                    - `age`: 나이
                    - `gender`: 성별 (MALE, FEMALE 등)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = MeResponse.class),
                            examples = @ExampleObject(
                                    name = "meSuccessExample",
                                    summary = "내 정보 조회 응답 성공 예시",
                                    value = """
                                            {
                                              "userId": "123456789012345678",
                                              "email": "user@example.com",
                                              "role": "ROLE_USER",
                                              "organizationCode": "ORG001",
                                              "name": "홍길동",
                                              "nickname": "길동이",
                                              "age": 25,
                                              "gender": "MALE"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 헤더가 유효하지 않거나 만료됨")
    })
    MeResponse me(Authentication authentication);

    @Operation(
            summary = "내 비밀번호 변경",
            description = """
                    **[ 내 비밀번호 변경 API ]**
                    로그인한 사용자의 비밀번호를 변경합니다.
                    
                    ### 📥 **입력 (Input)**
                    - `Authorization` 헤더: `Bearer <accessToken>` (필수)
                    - `currentPassword` (필수): 현재 비밀번호
                    - `newPassword` (필수): 새 비밀번호 (4~72자)
                    
                    ### 📤 **출력 (Output)**
                    - 성공 시 `{ "success": true }`를 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 변경 성공",
                    content = @Content(
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(
                                    name = "changePasswordSuccessExample",
                                    summary = "비밀번호 변경 성공 응답",
                                    value = "{\"success\": true}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 새 비밀번호가 기존 비밀번호와 동일함"),
            @ApiResponse(responseCode = "401", description = "인증 실패 또는 현재 비밀번호 불일치")
    })
    Map<String, Object> changePassword(
            Authentication authentication,
            @RequestBody(
                    required = true,
                    description = "현재/새 비밀번호를 포함한 변경 요청",
                    content = @Content(
                            schema = @Schema(implementation = ChangePasswordRequest.class),
                            examples = @ExampleObject(
                                    name = "changePasswordRequestExample",
                                    summary = "비밀번호 변경 요청 예시",
                                    value = """
                                            {
                                              "currentPassword": "password123",
                                              "newPassword": "newPassword456"
                                            }
                                            """
                            )
                    )
            )
            @Valid ChangePasswordRequest request
    );

    @Operation(
            summary = "개발용 조직 레코드 생성",
            description = """
                    **[ 조직 레코드 생성 API (개발용) ]**
                    새로운 조직 코드를 시스템에 추가합니다. 주로 개발/테스트용입니다.
                    
                    ### 📥 **입력 (Input)**
                    - `code` (필수): 고유한 조직 코드 식별자 (예: ORG999)
                    - `description` (선택): 해당 조직의 설명 또는 이름
                    
                    ### 📤 **출력 (Output)**
                    - 생성된 조직 레코드의 기본 정보를 반환합니다.
                    - `id`: 조직 레코드 ID
                    - `code`: 생성된 코드
                    - `description`: 생성 시 입력한 설명
                    - `createdAt`: 생성 일자
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조직 레코드 생성 성공",
                    content = @Content(
                            schema = @Schema(implementation = OrganizationRecordResponse.class),
                            examples = @ExampleObject(
                                    name = "createOrgSuccessExample",
                                    summary = "조직 레코드 생성 응답 예시",
                                    value = """
                                            {
                                              "id": "987654321098765432",
                                              "code": "ORG999",
                                              "description": "테스트용 조직입니다.",
                                              "createdAt": "2023-11-20T14:30:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "필수 값 누락 등 요청 페이로드 오류"),
            @ApiResponse(responseCode = "409", description = "입력한 조직 코드가 이미 존재함")
    })
    OrganizationRecordResponse createOrganizationRecord(
            Authentication authentication,
            @RequestBody(
                    required = true,
                    description = "조직 코드를 생성하기 위한 정보",
                    content = @Content(
                            schema = @Schema(implementation = CreateOrganizationRecordRequest.class),
                            examples = @ExampleObject(
                                    name = "createOrganizationRecordExample",
                                    summary = "조직 레코드 생성 요청 모델",
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
            description = """
                    **[ 전체 조직 조회 API (개발용) ]**
                    시스템 내에 등록된 삭제되지 않은 조직 코드를 최신순으로 조회합니다.
                    
                    ### 📤 **출력 (Output)**
                    - 조직 레코드 객체의 배열(List)을 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조직 레코드 전체 목록",
                    content = @Content(
                            schema = @Schema(implementation = OrganizationRecordResponse.class),
                            examples = @ExampleObject(
                                    name = "getOrgRecordsSuccessExample",
                                    summary = "전체 조회 응답 예시",
                                    value = """
                                            [
                                              {
                                                "id": "987654321098765432",
                                                "code": "ORG999",
                                                "description": "테스트용 조직입니다.",
                                                "createdAt": "2023-11-20T14:30:00Z"
                                              },
                                              {
                                                "id": "123456789012345678",
                                                "code": "ORG001",
                                                "description": "개발용 조직",
                                                "createdAt": "2023-11-19T10:00:00Z"
                                              }
                                            ]
                                            """
                            )
                    )
            )
    })
    List<OrganizationRecordResponse> getOrganizationRecords(Authentication authentication);

    @Operation(
            summary = "개발용 조직 레코드 삭제",
            description = """
                    **[ 조직 레코드 논리 삭제 API (개발용) ]**
                    조직 레코드의 ID(TSID)를 이용하여 논리적 삭제(Soft Delete) 처리를 합니다.
                    
                    ### 📥 **입력 (Input)**
                    - `organizationRecordId`: 삭제할 조직의 고유 ID (경로 혹은 쿼리 타입에 맞게 매핑)
                    
                    ### 📤 **출력 (Output)**
                    - 성공적으로 삭제 시 `success: true`가 포함된 JSON 반환.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조직 레코드 삭제 완료",
                    content = @Content(
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(
                                    name = "deleteOrgSuccessExample",
                                    summary = "삭제 성공 응답",
                                    value = "{\"success\": true}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 이미 삭제된 조직 레코드 ID")
    })
    Map<String, Object> deleteOrganizationRecord(Authentication authentication, Long organizationRecordId);
}
