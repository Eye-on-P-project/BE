package ac.jwooo.eye_on.domain.auth.ui.spec;

import ac.jwooo.eye_on.domain.auth.application.dto.request.LoginRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.request.LogoutRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.request.RefreshRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.request.SignupRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.response.AuthTokenResponse;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@Tag(name = "Auth", description = "인증/인가 API (회원가입, 로그인, 토큰 갱신, 로그아웃)")
public interface AuthControllerSpec {

    @Operation(
            summary = "회원가입",
            description = """
                    이메일과 비밀번호로 회원가입합니다.
                    
                    - **WEB 클라이언트**: `X-Client-Type: WEB` 헤더 전송 시 refreshToken은 HttpOnly 쿠키로 설정됩니다.
                    - **MOBILE 클라이언트**: refreshToken이 응답 바디에 포함됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "유효성 검증 실패 (이메일 형식 오류, 비밀번호 길이 등)"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    })
    AuthTokenResponse signup(
            @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SignupRequest.class),
                            examples = @ExampleObject(
                                    name = "signupExample",
                                    summary = "회원가입 예시",
                                    value = """
                                            {
                                              "email": "user@example.com",
                                              "password": "password123",
                                              "organizationCode": "ORG001",
                                              "name": "홍길동",
                                              "nickname": "길동이",
                                              "age": 25,
                                              "gender": "MALE"
                                            }
                                            """
                            )
                    )
            )
            @Valid SignupRequest request,

            @Parameter(
                    name = "X-Client-Type",
                    description = "클라이언트 타입 (WEB | MOBILE). 미입력 시 MOBILE로 처리됩니다.",
                    in = ParameterIn.HEADER,
                    schema = @Schema(type = "string", allowableValues = {"WEB", "MOBILE"}),
                    example = "WEB"
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,

            HttpServletResponse response
    );

    @Operation(
            summary = "로그인",
            description = """
                    이메일과 비밀번호로 로그인합니다.
                    
                    - **WEB 클라이언트**: refreshToken이 HttpOnly 쿠키로 설정됩니다.
                    - **MOBILE 클라이언트**: refreshToken이 응답 바디에 포함됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "유효성 검증 실패"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    AuthTokenResponse login(
            @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "loginExample",
                                    summary = "로그인 예시",
                                    value = """
                                            {
                                              "email": "user@example.com",
                                              "password": "password123"
                                            }
                                            """
                            )
                    )
            )
            @Valid LoginRequest request,

            @Parameter(
                    name = "X-Client-Type",
                    description = "클라이언트 타입 (WEB | MOBILE)",
                    in = ParameterIn.HEADER,
                    schema = @Schema(type = "string", allowableValues = {"WEB", "MOBILE"}),
                    example = "WEB"
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,

            HttpServletResponse response
    );

    @Operation(
            summary = "토큰 갱신 (Refresh)",
            description = """
                    refreshToken을 이용해 새로운 accessToken과 refreshToken을 발급합니다.
                    
                    - **WEB 클라이언트**: refreshToken을 쿠키에서 자동으로 읽어옵니다. (본문 불필요)
                    - **MOBILE 클라이언트**: 요청 바디의 `refreshToken` 필드로 전송합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 refreshToken")
    })
    AuthTokenResponse refresh(
            @RequestBody(
                    required = false,
                    content = @Content(
                            schema = @Schema(implementation = RefreshRequest.class),
                            examples = @ExampleObject(
                                    name = "refreshExample",
                                    summary = "MOBILE용 refresh 예시",
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                                            }
                                            """
                            )
                    )
            )
            RefreshRequest requestBody,

            @Parameter(
                    name = "X-Client-Type",
                    description = "클라이언트 타입 (WEB | MOBILE)",
                    in = ParameterIn.HEADER,
                    schema = @Schema(type = "string", allowableValues = {"WEB", "MOBILE"}),
                    example = "MOBILE"
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,

            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "로그아웃",
            description = """
                    accessToken을 무효화하고 refreshToken을 삭제합니다.
                    
                    - **WEB 클라이언트**: refreshToken 쿠키를 자동으로 삭제합니다.
                    - **MOBILE 클라이언트**: 요청 바디의 `refreshToken` 필드로 전송합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(value = "{\"success\": true}")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    Map<String, Object> logout(
            @RequestBody(
                    required = false,
                    content = @Content(schema = @Schema(implementation = LogoutRequest.class))
            )
            LogoutRequest requestBody,

            @Parameter(
                    name = "X-Client-Type",
                    description = "클라이언트 타입 (WEB | MOBILE)",
                    in = ParameterIn.HEADER,
                    schema = @Schema(type = "string", allowableValues = {"WEB", "MOBILE"}),
                    example = "MOBILE"
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,

            HttpServletRequest request,
            HttpServletResponse response
    );
}
