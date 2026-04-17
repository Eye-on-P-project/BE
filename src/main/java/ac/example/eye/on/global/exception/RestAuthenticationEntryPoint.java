package ac.example.eye.on.global.exception;

import java.io.IOException;
import java.time.Instant;

import ac.example.eye.on.global.security.JwtAuthenticationFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        CustomException customException = (CustomException) request.getAttribute(JwtAuthenticationFilter.AUTH_EXCEPTION_ATTR);
        ErrorCode errorCode = customException != null ? customException.getErrorCode() : ErrorCode.UNAUTHORIZED;
        String message = customException != null ? customException.getMessage() : errorCode.getMessage();

        writeJsonError(response, request, errorCode, message);
    }

    private void writeJsonError(
            HttpServletResponse response,
            HttpServletRequest request,
            ErrorCode errorCode,
            String message
    ) throws IOException {
        String body = """
                {"timestamp":"%s","status":%d,"code":"%s","message":"%s","path":"%s","errors":[]}
                """.formatted(
                Instant.now(),
                errorCode.getStatus().value(),
                escape(errorCode.getCode()),
                escape(message),
                escape(request.getRequestURI())
        );

        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

