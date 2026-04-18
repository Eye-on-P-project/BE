package ac.jwooo.eye_on.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("로컬 개발 서버")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Access Token을 입력하세요. (Bearer 접두사 제외)")
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Eye-On API")
                .description("""
                        Eye-On 서비스 REST API 문서입니다.
                        
                        ## 인증 방식
                        - **JWT Bearer Token**: `Authorization: Bearer {accessToken}` 헤더로 전달
                        - **WEB 클라이언트**: refreshToken은 HttpOnly 쿠키로 관리
                        - **MOBILE 클라이언트**: refreshToken은 응답 바디에 포함
                        
                        ## 클라이언트 구분
                        - `X-Client-Type: WEB` — 웹 브라우저 클라이언트
                        - `X-Client-Type: MOBILE` (또는 미입력) — 모바일 앱 클라이언트
                        """)
                .version("v1.0.0")
                .contact(new Contact()
                        .name("Eye-On Team")
                );
    }
}
