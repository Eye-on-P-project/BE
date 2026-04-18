package ac.jwooo.eye_on.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class ApiRequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String className = resolveClassName(handler);
        String requestUri = buildRequestUri(request);

        log.info("[{}] {} {}", className, request.getMethod(), requestUri);
        return true;
    }

    private String resolveClassName(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            return ClassUtils.getUserClass(handlerMethod.getBeanType()).getSimpleName();
        }
        return "UnknownHandler";
    }

    private String buildRequestUri(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }
}
