package ac.jwooo.eye_on.domain.user.ui.spec;

import java.util.Map;

import ac.jwooo.eye_on.domain.user.application.dto.request.ChangePasswordRequest;
import ac.jwooo.eye_on.domain.user.application.dto.response.MeResponse;
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

@Tag(name = "User", description = "사용자 API")
public interface UserControllerSpec {

    @Operation(
            summary = "내 정보 조회",
            description = """
                    **[ 내 정보 조회 API ]**
                    현재 접속 유저의 프로필, 소속 조직, 역할 등의 상세 정보를 조회합니다.
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
                                              "organization": "ORG001",
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
            @ApiResponse(responseCode = "400", description = "요청 형식 오류, 현재 비밀번호 불일치, 또는 새 비밀번호가 기존 비밀번호와 동일함"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "조직 코드 불일치")
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
                                              "newPassword": "newPassword456",
                                              "organization": "ORG001"
                                            }
                                            """
                            )
                    )
            )
            @Valid ChangePasswordRequest request
    );
}
