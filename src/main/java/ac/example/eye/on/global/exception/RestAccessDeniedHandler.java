package ac.example.eye.on.global.exception;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        String body = """
                {"timestamp":"%s","status":%d,"code":"%s","message":"%s","path":"%s","errors":[]}
                """.formatted(
                Instant.now(),
                ErrorCode.FORBIDDEN.getStatus().value(),
                ErrorCode.FORBIDDEN.getCode(),
                escape(ErrorCode.FORBIDDEN.getMessage()),
                escape(request.getRequestURI())
        );

        response.setStatus(ErrorCode.FORBIDDEN.getStatus().value());
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

