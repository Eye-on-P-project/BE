package ac.jwooo.eye_on.domain.auth.ui;

import java.util.Map;

import ac.jwooo.eye_on.domain.auth.application.dto.response.AuthTokenResponse;
import ac.jwooo.eye_on.domain.auth.application.dto.request.LoginRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.request.LogoutRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.request.RefreshRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.request.SignupRequest;
import ac.jwooo.eye_on.domain.auth.domain.entity.ClientType;
import ac.jwooo.eye_on.domain.auth.domain.service.AuthResult;
import ac.jwooo.eye_on.domain.auth.domain.service.AuthService;
import ac.jwooo.eye_on.domain.auth.ui.spec.AuthControllerSpec;
import ac.jwooo.eye_on.global.config.JwtProperties;
import ac.jwooo.eye_on.global.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerSpec {

    private final AuthService authService;
    private final SecurityProperties securityProperties;
    private final JwtProperties jwtProperties;

    @PostMapping("/signup")
    public AuthTokenResponse signup(
            @Valid @RequestBody SignupRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletResponse response
    ) {
        ClientType clientType = ClientType.fromHeader(clientTypeHeader);
        AuthResult authResult = authService.signup(request, clientType);
        return buildAuthResponse(authResult, clientType, response);
    }

    @PostMapping("/login")
    public AuthTokenResponse login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletResponse response
    ) {
        ClientType clientType = ClientType.fromHeader(clientTypeHeader);
        AuthResult authResult = authService.login(request, clientType);
        return buildAuthResponse(authResult, clientType, response);
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(
            @RequestBody(required = false) RefreshRequest requestBody,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ClientType clientType = ClientType.fromHeader(clientTypeHeader);
        String refreshToken = resolveRefreshToken(requestBody == null ? null : requestBody.refreshToken(), request);
        AuthResult authResult = authService.refresh(refreshToken, clientType);
        return buildAuthResponse(authResult, clientType, response);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(
            @RequestBody(required = false) LogoutRequest requestBody,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ClientType clientType = ClientType.fromHeader(clientTypeHeader);
        String accessToken = resolveAccessToken(request);
        String refreshToken = resolveRefreshToken(requestBody == null ? null : requestBody.refreshToken(), request);

        authService.logout(accessToken, refreshToken, clientType);

        if (clientType == ClientType.WEB) {
            clearRefreshCookie(response);
        }

        return Map.of("success", true);
    }

    private AuthTokenResponse buildAuthResponse(AuthResult authResult, ClientType clientType, HttpServletResponse response) {
        if (clientType == ClientType.WEB) {
            if (StringUtils.hasText(authResult.refreshToken())) {
                setRefreshCookie(response, authResult.refreshToken());
            }
            return AuthTokenResponse.from(authResult, null);
        }
        return AuthTokenResponse.from(authResult, authResult.refreshToken());
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(securityProperties.refreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(securityProperties.cookieSecure())
                .path(securityProperties.cookiePath())
                .sameSite(securityProperties.cookieSameSite())
                .maxAge(jwtProperties.refreshTokenExpirationSeconds());

        if (StringUtils.hasText(securityProperties.cookieDomain())) {
            builder.domain(securityProperties.cookieDomain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(securityProperties.refreshCookieName(), "")
                .httpOnly(true)
                .secure(securityProperties.cookieSecure())
                .path(securityProperties.cookiePath())
                .sameSite(securityProperties.cookieSameSite())
                .maxAge(0);

        if (StringUtils.hasText(securityProperties.cookieDomain())) {
            builder.domain(securityProperties.cookieDomain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private String resolveRefreshToken(String requestBodyToken, HttpServletRequest request) {
        if (StringUtils.hasText(requestBodyToken)) {
            return requestBodyToken;
        }

        Cookie cookie = WebUtils.getCookie(request, securityProperties.refreshCookieName());
        return cookie != null ? cookie.getValue() : null;
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
