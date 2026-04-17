package ac.example.eye.on.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import ac.example.eye.on.domain.auth.model.ClientType;
import ac.example.eye.on.domain.user.entity.User;
import ac.example.eye.on.domain.user.entity.UserRole;
import ac.example.eye.on.global.config.JwtProperties;
import ac.example.eye.on.global.exception.CustomException;
import ac.example.eye.on.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_CLIENT_TYPE = "clientType";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user, ClientType clientType) {
        return createToken(user, clientType, TokenType.ACCESS, jwtProperties.accessTokenExpirationSeconds());
    }

    public String createRefreshToken(User user, ClientType clientType) {
        return createToken(user, clientType, TokenType.REFRESH, jwtProperties.refreshTokenExpirationSeconds());
    }

    public Claims parseClaims(String token) {
        if (!StringUtils.hasText(token)) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Claims parseClaimsLenient(String token) {
        if (!StringUtils.hasText(token)) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public TokenType getTokenType(Claims claims) {
        String value = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!StringUtils.hasText(value)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        try {
            return TokenType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public ClientType getClientType(Claims claims) {
        String value = claims.get(CLAIM_CLIENT_TYPE, String.class);
        return ClientType.fromTokenClaim(value);
    }

    public AuthenticatedUser toAuthenticatedUser(Claims claims) {
        Long userId = parseUserId(claims);
        String email = claims.get(CLAIM_EMAIL, String.class);
        String roleValue = claims.get(CLAIM_ROLE, String.class);
        UserRole role;
        try {
            role = UserRole.valueOf(roleValue);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        ClientType clientType = getClientType(claims);
        return new AuthenticatedUser(userId, email, role, clientType);
    }

    public long remainingSeconds(Claims claims) {
        Instant now = Instant.now();
        Instant exp = claims.getExpiration().toInstant();
        return Math.max(0, exp.getEpochSecond() - now.getEpochSecond());
    }

    private String createToken(User user, ClientType clientType, TokenType tokenType, long expirationSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_CLIENT_TYPE, clientType.name())
                .claim(CLAIM_TOKEN_TYPE, tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    private Long parseUserId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
