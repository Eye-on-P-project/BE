package ac.jwooo.eye_on.domain.auth.domain.service;

import ac.jwooo.eye_on.domain.auth.application.dto.request.LoginRequest;
import ac.jwooo.eye_on.domain.auth.application.dto.request.SignupRequest;
import ac.jwooo.eye_on.domain.auth.domain.entity.ClientType;
import ac.jwooo.eye_on.domain.user.domain.entity.Organization;
import ac.jwooo.eye_on.domain.user.domain.entity.OrganizationStatus;
import ac.jwooo.eye_on.domain.user.domain.entity.User;
import ac.jwooo.eye_on.domain.user.domain.entity.UserRole;
import ac.jwooo.eye_on.domain.user.domain.repository.OrganizationRepository;
import ac.jwooo.eye_on.domain.user.domain.repository.UserRepository;
import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;
import ac.jwooo.eye_on.global.security.JwtTokenProvider;
import ac.jwooo.eye_on.global.security.RedisTokenStore;
import ac.jwooo.eye_on.global.security.TokenType;
import io.jsonwebtoken.Claims;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ORGANIZATION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ORGANIZATION_CODE_LENGTH = 6;
    private static final int ORGANIZATION_CODE_MAX_ATTEMPTS = 20;

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore redisTokenStore;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public AuthResult signup(SignupRequest request, ClientType clientType) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User newUser = switch (clientType) {
            case WEB -> createPendingAdminForWebSignup(request, email);
            case APP -> createGeneralUserForAppSignup(request, email);
        };

        User savedUser = userRepository.save(newUser);
        if (clientType == ClientType.WEB) {
            return new AuthResult(savedUser.getId(), null, null, savedUser.getRole());
        }
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

        if (clientType == ClientType.WEB) {
            validateWebLoginPolicy(user);
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

        if (clientType == ClientType.WEB) {
            validateWebLoginPolicy(user);
        }

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

    private User createPendingAdminForWebSignup(SignupRequest request, String email) {
        validateWebOrganizationSignupRequest(request);

        if (organizationRepository.existsByCorporateNumAndStatusInAndDeletedAtIsNull(
                request.corporateNum().trim(),
                List.of(OrganizationStatus.PENDING, OrganizationStatus.ACTIVE)
        )) {
            throw new CustomException(ErrorCode.ORGANIZATION_SIGNUP_ALREADY_EXISTS);
        }

        String organizationCode = generateOrganizationCode();
        Organization organization = Organization.createPending(
                request.organizationName(),
                organizationCode,
                request.businessmanNum(),
                request.establishedAt(),
                request.representativeName(),
                request.corporateNum(),
                request.businessName(),
                request.coRepresentativeName(),
                request.businessAddress()
        );
        Organization savedOrganization = organizationRepository.save(organization);

        return User.createAdmin(
                email,
                passwordEncoder.encode(request.password()),
                savedOrganization.getId()
        );
    }

    private User createGeneralUserForAppSignup(SignupRequest request, String email) {
        validateGeneralUserProfile(request);
        return User.createGeneralUser(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                request.nickname().trim(),
                request.age(),
                request.gender()
        );
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

    private void validateWebOrganizationSignupRequest(SignupRequest request) {
        if (!StringUtils.hasText(request.organizationName())) {
            throw new CustomException(ErrorCode.ORGANIZATION_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.businessmanNum())) {
            throw new CustomException(ErrorCode.BUSINESSMAN_NUM_REQUIRED);
        }
        if (request.establishedAt() == null) {
            throw new CustomException(ErrorCode.ESTABLISHED_AT_REQUIRED);
        }
        if (!StringUtils.hasText(request.representativeName())) {
            throw new CustomException(ErrorCode.REPRESENTATIVE_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.corporateNum())) {
            throw new CustomException(ErrorCode.CORPORATE_NUM_REQUIRED);
        }
        if (!StringUtils.hasText(request.businessName())) {
            throw new CustomException(ErrorCode.BUSINESS_NAME_REQUIRED);
        }
    }

    private void validateWebLoginPolicy(User user) {
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new CustomException(ErrorCode.WEB_ADMIN_LOGIN_ONLY);
        }
        if (user.getRole() == UserRole.SYSTEM_ADMIN) {
            return;
        }

        Long organizationId = user.getOrganization();
        if (organizationId == null) {
            throw new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }

        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORGANIZATION_NOT_FOUND));

        if (organization.getStatus() == OrganizationStatus.PENDING) {
            throw new CustomException(ErrorCode.ORGANIZATION_SIGNUP_PENDING);
        }
        if (organization.getStatus() == OrganizationStatus.REJECTED) {
            throw new CustomException(ErrorCode.ORGANIZATION_SIGNUP_REJECTED);
        }
    }

    private String generateOrganizationCode() {
        for (int attempt = 0; attempt < ORGANIZATION_CODE_MAX_ATTEMPTS; attempt += 1) {
            String code = randomOrganizationCode();
            if (!organizationRepository.existsByCodeAndDeletedAtIsNull(code)) {
                return code;
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "조직 코드를 생성하지 못했습니다.");
    }

    private String randomOrganizationCode() {
        StringBuilder builder = new StringBuilder(ORGANIZATION_CODE_LENGTH);
        for (int i = 0; i < ORGANIZATION_CODE_LENGTH; i += 1) {
            int randomIndex = secureRandom.nextInt(ORGANIZATION_CODE_CHARS.length());
            builder.append(ORGANIZATION_CODE_CHARS.charAt(randomIndex));
        }
        return builder.toString();
    }

}
