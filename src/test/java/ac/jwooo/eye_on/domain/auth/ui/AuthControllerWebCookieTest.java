package ac.jwooo.eye_on.domain.auth.ui;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ac.jwooo.eye_on.domain.auth.domain.entity.ClientType;
import ac.jwooo.eye_on.domain.auth.domain.service.AuthResult;
import ac.jwooo.eye_on.domain.auth.domain.service.AuthService;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.global.config.JwtProperties;
import ac.jwooo.eye_on.global.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerWebCookieTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties("refreshToken", false, "Lax", "/", "", false, false);
        JwtProperties jwtProperties = new JwtProperties("test-secret-key-test-secret-key", 900, 1209600);
        AuthController authController = new AuthController(authService, securityProperties, jwtProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("WEB 로그인 시 refreshToken은 HttpOnly 쿠키로 내려가고 응답 바디에는 포함되지 않는다")
    void loginForWebSetsHttpOnlyRefreshCookie() throws Exception {
        when(authService.login(any(), eq(ClientType.WEB)))
                .thenReturn(new AuthResult(1L, "access-token", "refresh-token", UserRole.ADMIN));

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Client-Type", "WEB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@eyeon.com",
                                  "password": "password1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value(nullValue()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
    }

    @Test
    @DisplayName("WEB refresh 시 쿠키의 refreshToken을 사용해 재발급한다")
    void refreshForWebUsesRefreshCookie() throws Exception {
        when(authService.refresh("refresh-old-token", ClientType.WEB))
                .thenReturn(new AuthResult(1L, "new-access-token", "new-refresh-token", UserRole.ADMIN));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Client-Type", "WEB")
                        .cookie(new Cookie("refreshToken", "refresh-old-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value(nullValue()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=new-refresh-token")));

        verify(authService).refresh("refresh-old-token", ClientType.WEB);
    }

    @Test
    @DisplayName("WEB 로그아웃 시 refreshToken 쿠키를 만료 처리한다")
    void logoutForWebClearsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Client-Type", "WEB")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

        verify(authService).logout("access-token", "refresh-token", ClientType.WEB);
    }
}
