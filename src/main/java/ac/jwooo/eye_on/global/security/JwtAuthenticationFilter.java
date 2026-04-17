package ac.jwooo.eye_on.global.security;

import java.io.IOException;
import java.util.List;

import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_EXCEPTION_ATTR = "AUTH_EXCEPTION";

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveBearerToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                TokenType tokenType = jwtTokenProvider.getTokenType(claims);
                if (tokenType != TokenType.ACCESS) {
                    throw new CustomException(ErrorCode.INVALID_TOKEN);
                }

                String jti = claims.getId();
                if (redisTokenStore.isAccessBlacklisted(jti)) {
                    throw new CustomException(ErrorCode.TOKEN_BLACKLISTED);
                }

                AuthenticatedUser authenticatedUser = jwtTokenProvider.toAuthenticatedUser(claims);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (CustomException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_EXCEPTION_ATTR, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}

