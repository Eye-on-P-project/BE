package ac.example.eye.on.domain.auth.service;

import ac.example.eye.on.domain.auth.dto.LoginRequest;
import ac.example.eye.on.domain.auth.dto.SignupRequest;
import ac.example.eye.on.domain.auth.model.ClientType;
import ac.example.eye.on.domain.user.entity.User;
import ac.example.eye.on.domain.user.repository.OrganizationCodeRepository;
import ac.example.eye.on.domain.user.repository.UserRepository;
import ac.example.eye.on.global.exception.CustomException;
import ac.example.eye.on.global.exception.ErrorCode;
import ac.example.eye.on.global.security.JwtTokenProvider;
import ac.example.eye.on.global.security.RedisTokenStore;
import ac.example.eye.on.global.security.TokenType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OrganizationCodeRepository organizationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;

    @Override
    @Transactional
    public AuthResult signup(SignupRequest request, ClientType clientType) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User newUser;
        if (StringUtils.hasText(request.organizationCode())) {
            String organizationCode = normalizeOrganizationCode(request.organizationCode());
            if (!organizationCodeRepository.existsByCodeAndDeletedAtIsNull(organizationCode)) {
                throw new CustomException(ErrorCode.ORGANIZATION_CODE_NOT_FOUND);
            }

            newUser = User.createAdmin(
                    email,
                    passwordEncoder.encode(request.password()),
                    organizationCode
            );
        } else {
            validateGeneralUserProfile(request);
            newUser = User.createGeneralUser(
                    email,
                    passwordEncoder.encode(request.password()),
                    request.name().trim(),
                    request.nickname().trim(),
                    request.age(),
                    request.gender()
            );
        }

        User savedUser = userRepository.save(newUser);
        return issueTokens(savedUser, clientType);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request, ClientType clientType) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(request.email()))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user, clientType);
    }

    @Override
    @Transactional
    public AuthResult refresh(String refreshToken, ClientType clientType) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        TokenType tokenType = jwtTokenProvider.getTokenType(claims);
        if (tokenType != TokenType.REFRESH) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        ClientType tokenClientType = jwtTokenProvider.getClientType(claims);
        if (tokenClientType != clientType) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String refreshJti = claims.getId();
        if (redisTokenStore.isRefreshBlacklisted(refreshJti)) {
            throw new CustomException(ErrorCode.TOKEN_BLACKLISTED);
        }

        Long userId = parseUserId(claims);
        if (!redisTokenStore.matchesRefreshWhitelist(userId, clientType, refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long oldRefreshTtl = jwtTokenProvider.remainingSeconds(claims);
        redisTokenStore.blacklistRefreshJti(refreshJti, oldRefreshTtl);

        return issueTokens(user, clientType);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken, ClientType clientType) {
        Claims accessClaims = safeParseLenient(accessToken);
        if (accessClaims != null && safeTokenType(accessClaims) == TokenType.ACCESS) {
            String accessJti = accessClaims.getId();
            long accessTtl = jwtTokenProvider.remainingSeconds(accessClaims);
            redisTokenStore.blacklistAccessJti(accessJti, accessTtl);
        }

        Claims refreshClaims = safeParseLenient(refreshToken);
        if (refreshClaims != null && safeTokenType(refreshClaims) == TokenType.REFRESH) {
            String refreshJti = refreshClaims.getId();
            long refreshTtl = jwtTokenProvider.remainingSeconds(refreshClaims);
            redisTokenStore.blacklistRefreshJti(refreshJti, refreshTtl);

            try {
                Long userId = parseUserId(refreshClaims);
                ClientType refreshClientType = jwtTokenProvider.getClientType(refreshClaims);
                redisTokenStore.removeRefreshWhitelist(userId, refreshClientType);
            } catch (CustomException ignored) {
                // 로그아웃은 항상 성공 응답을 주기 위해 토큰 파싱 실패를 무시한다.
            }
            return;
        }

        if (accessClaims != null) {
            try {
                Long userId = parseUserId(accessClaims);
                redisTokenStore.removeRefreshWhitelist(userId, clientType);
            } catch (CustomException ignored) {
                // 로그아웃은 항상 성공 응답을 주기 위해 토큰 파싱 실패를 무시한다.
            }
        }
    }

    private AuthResult issueTokens(User user, ClientType clientType) {
        String accessToken = jwtTokenProvider.createAccessToken(user, clientType);
        String refreshToken = jwtTokenProvider.createRefreshToken(user, clientType);

        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);
        long refreshTtl = jwtTokenProvider.remainingSeconds(refreshClaims);
        redisTokenStore.putRefreshWhitelist(user.getId(), clientType, refreshToken, refreshTtl);

        return new AuthResult(
                user.getId(),
                accessToken,
                refreshToken,
                user.getRole()
        );
    }

    private Claims safeParseLenient(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        try {
            return jwtTokenProvider.parseClaimsLenient(token);
        } catch (CustomException e) {
            return null;
        }
    }

    private TokenType safeTokenType(Claims claims) {
        try {
            return jwtTokenProvider.getTokenType(claims);
        } catch (CustomException e) {
            return null;
        }
    }

    private void validateGeneralUserProfile(SignupRequest request) {
        if (!StringUtils.hasText(request.name())
                || !StringUtils.hasText(request.nickname())
                || request.age() == null
                || request.gender() == null) {
            throw new CustomException(
                    ErrorCode.USER_PROFILE_REQUIRED,
                    "일반 사용자 회원가입에는 name, nickname, age, gender가 필요합니다."
            );
        }
    }

    private Long parseUserId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeOrganizationCode(String organizationCode) {
        return organizationCode.trim().toUpperCase();
    }
}
