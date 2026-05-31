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
                    **[ 회원가입 API ]**
                    이메일과 비밀번호, 사용자 부가 정보를 입력받아 회원가입을 진행합니다.
                    
                    ### 📥 **입력 (Input)**
                    - `email` (필수): 사용자 이메일 (예: user@example.com)
                    - `password` (필수): 4~72자의 비밀번호
                    - `organization` (조건부): **WEB 가입 시 필수** (예: ORG001), **APP 가입 시 무시됨**
                    - `name` (조건부): APP 가입 시 필수
                    - `nickname` (조건부): APP 가입 시 필수
                    - `age` (조건부): APP 가입 시 필수 (1~120)
                    - `gender` (조건부): APP 가입 시 필수 (MALE, FEMALE)
                    
                    ### 📤 **출력 (Output)**
                    - `userId`: 생성된 사용자의 고유 ID (문자열 형태의 TSID)
                    - `accessToken`: API 요청 인증에 사용될 토큰
                    - `refreshToken`: AccessToken 갱신을 위한 토큰
                    - `role`: 사용자 권한 (ROLE_USER 등)
                    
                    **[ 클라이언트 타입에 따른 처리 ]**
                    - **WEB 클라이언트 (`X-Client-Type: WEB`)**: 관리자 계정만 가입 가능, refreshToken은 `HttpOnly` 쿠키로 설정됩니다.
                    - **APP 클라이언트 (`X-Client-Type: APP` 또는 미입력)**: 일반 사용자로만 가입되며 organization은 사용되지 않습니다.
                    - **조직 관리자 정책**: 조직 코드당 관리자 계정은 1개만 생성할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(
                            schema = @Schema(implementation = AuthTokenResponse.class),
                            examples = @ExampleObject(
                                    name = "signupSuccessExample",
                                    summary = "회원가입 완료 응답",
                                    value = """
                                            {
                                              "userId": "123456789012345678",
                                              "accessToken": "eyJhbGciOi...",
                                              "refreshToken": "eyJhbGciOi...",
                                              "role": "ROLE_USER"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "유효성 검증 실패 (이메일 누락, 비밀번호 길이 미달 등)"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    })
    AuthTokenResponse signup(
            @RequestBody(
                    required = true,
                    description = "회원가입에 필요한 정보 (이메일 및 비밀번호 필수)",
                    content = @Content(
                            schema = @Schema(implementation = SignupRequest.class),
                            examples = @ExampleObject(
                                    name = "signupRequestExample",
                                    summary = "회원가입 요청 모델",
                                    value = """
                                            {
                                              "email": "user@example.com",
                                              "password": "password123",
                                              "organization": "ORG001",
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
                    description = "클라이언트 환경을 구분하는 헤더 (WEB | APP). 기본값: APP",
                    in = ParameterIn.HEADER,
                    schema = @Schema(type = "string", allowableValues = {"WEB", "APP"}),
                    example = "WEB"
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,

            HttpServletResponse response
    );

    @Operation(
            summary = "로그인",
            description = """
                    **[ 로그인 API ]**
                    기존 회원의 이메일과 비밀번호로 인증하여 토큰을 발급받습니다.
                    
                    ### 📥 **입력 (Input)**
                    - `email` (필수): 가입 시 사용한 이메일
                    - `password` (필수): 사용자 비밀번호
                    
                    ### 📤 **출력 (Output)**
                    - `userId`: 로그인한 사용자의 ID (문자열 형태)
                    - `accessToken`: API 요청 인증용 JWT 접근 토큰
                    - `refreshToken`: 토큰 갱신용 Refresh 토큰
                    - `role`: 사용자 권한
                    
                    **[ 클라이언트 타입에 따른 처리 ]**
                    - **WEB 클라이언트**: 조직 관리자(ADMIN) 계정만 로그인 가능하며, refreshToken이 `HttpOnly` 쿠키에 담겨 반환됩니다.
                    - **APP 클라이언트**: refreshToken이 응답 바디의 JSON에 포함됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 및 토큰 반환",
                    content = @Content(
                            schema = @Schema(implementation = AuthTokenResponse.class),
                            examples = @ExampleObject(
                                    name = "loginSuccessExample",
                                    summary = "로그인 성공 응답 예시",
                                    value = """
                                            {
                                              "userId": "123456789012345678",
                                              "accessToken": "eyJhbGciOi...",
                                              "refreshToken": "eyJhbGciOi...",
                                              "role": "ROLE_USER"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 유효성 검증 실패"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호가 일치하지 않음")
    })
    AuthTokenResponse login(
            @RequestBody(
                    required = true,
                    description = "로그인 자격 증명(Credentials)",
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "loginRequestExample",
                                    summary = "로그인 요청 모델 예시",
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
                    description = "동작 환경 설정을 위한 클라이언트 타입 (WEB | APP)",
                    in = ParameterIn.HEADER,
                    schema = @Schema(type = "string", allowableValues = {"WEB", "APP"}),
                    example = "WEB"
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,

            HttpServletResponse response
    );

    @Operation(
            summary = "토큰 갱신 (Refresh)",
            description = """
                    **[ 토큰 갱신 API ]**
                    기존의 유효한 refreshToken을 이용해 새로운 accessToken과 refreshToken 쌍을 발급받습니다.
                    만료된 accessToken 대신 이 API를 호출해야 합니다.
                    
                    ### 📥 **입력 (Input)**
                    - **WEB 클라이언트**: 별도의 Request Body 없이, 자동으로 전송되는 `refreshToken` 쿠키를 이용합니다.
                    - **APP 클라이언트**: 바디의 `refreshToken` 필드에 토큰 값을 명시적으로 포함하여 전송해야 합니다.
                    
                    ### 📤 **출력 (Output)**
                    - 로그인과 동일하게 `accessToken` 및 `refreshToken`이 갱신되어 반환됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공 (새로운 접근/갱신 토큰 발급)",
                    content = @Content(
                            schema = @Schema(implementation = AuthTokenResponse.class),
                            examples = @ExampleObject(
                                    name = "refreshSuccessExample",
                                    summary = "토큰 갱신 성공 응답",
                                    value = """
                                            {
                                              "userId": "123456789012345678",
                                              "accessToken": "eyJhbGciOi...new",
                                              "refreshToken": "eyJhbGciOi...new",
                                              "role": "ROLE_USER"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 이미 만료/삭제된 refreshToken. 다시 로그인해야 합니다.")
    })
    AuthTokenResponse refresh(
            @RequestBody(
                    required = false,
                    description = "토큰 갱신 요청 모델 (APP 환경에서는 필수)",
                    content = @Content(
                            schema = @Schema(implementation = RefreshRequest.class),
                            examples = @ExampleObject(
                                    name = "refreshRequestExample",
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
                    description = "토큰 획득/반환 방식을 구분하는 클라이언트 타입 (WEB | APP)",
                    in = ParameterIn.HEADER,
                    schema = @Schema(type = "string", allowableValues = {"WEB", "APP"}),
                    example = "APP"
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,

            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "로그아웃",
            description = """
                    **[ 로그아웃 API ]**
                    현재 사용자의 세션을 종료하고 토큰을 무효화합니다.
                    (Redis 등의 스토리지에 저장된 refreshToken이 삭제됩니다.)
                    
                    ### 📥 **입력 (Input)**
                    - `Authorization` 헤더: `Bearer <accessToken>` 필수.
                    - **WEB 클라이언트**: `refreshToken` 쿠키가 자동으로 전송되므로 본문 불필요.
                    - **APP 클라이언트**: 요청 바디에 `refreshToken`을 담아 전송해야 삭제 처리 가능.
                    
                    ### 📤 **출력 (Output)**
                    - 성공 시 `success: true` 응답 반환.
                    - **WEB 클라이언트**: 추가적으로 `refreshToken` 쿠키를 만료(삭제)시키는 Set-Cookie 헤더가 포함됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 처리가 완료되었습니다.",
                    content = @Content(
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(
                                    name = "logoutSuccessExample",
                                    summary = "로그아웃 성공 반환 값",
                                    value = "{\"success\": true}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 헤더가 유효하지 않은 요청")
    })
    Map<String, Object> logout(
            @RequestBody(
                    required = false,
                    description = "APP 클라이언트인 경우 로그아웃할 refreshToken 필수",
                    content = @Content(
                            schema = @Schema(implementation = LogoutRequest.class),
                            examples = @ExampleObject(
                                    name = "logoutRequestExample",
                                    summary = "MOBILE용 로그아웃 요청",
                                    value = "{\n  \"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9...\"\n}"
                            )
                    )
            )
            LogoutRequest requestBody,

            @Parameter(
                    name = "X-Client-Type",
                    description = "쿠키 삭제 여부를 결정하는 클라이언트 타입 (WEB | APP)",
                    in = ParameterIn.HEADER,
                    schema = @Schema(type = "string", allowableValues = {"WEB", "APP"}),
                    example = "APP"
            )
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,

            HttpServletRequest request,
            HttpServletResponse response
    );
}
