package ac.jwooo.eye_on.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import ac.jwooo.eye_on.domain.auth.domain.entity.ClientType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RedisTokenStore {

    private final StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void putRefreshWhitelist(Long userId, ClientType clientType, String refreshToken, long ttlSeconds) {
        String key = whitelistKey(userId, clientType);
        redisTemplate.opsForValue().set(key, hash(refreshToken), Duration.ofSeconds(ttlSeconds));
    }

    public boolean matchesRefreshWhitelist(Long userId, ClientType clientType, String refreshToken) {
        String key = whitelistKey(userId, clientType);
        String value = redisTemplate.opsForValue().get(key);
        return value != null && value.equals(hash(refreshToken));
    }

    public void removeRefreshWhitelist(Long userId, ClientType clientType) {
        redisTemplate.delete(whitelistKey(userId, clientType));
    }

    public void blacklistAccessJti(String jti, long ttlSeconds) {
        if (!StringUtils.hasText(jti) || ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(accessBlacklistKey(jti), "1", Duration.ofSeconds(ttlSeconds));
    }

    public void blacklistRefreshJti(String jti, long ttlSeconds) {
        if (!StringUtils.hasText(jti) || ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(refreshBlacklistKey(jti), "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isAccessBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessBlacklistKey(jti)));
    }

    public boolean isRefreshBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshBlacklistKey(jti)));
    }

    private String whitelistKey(Long userId, ClientType clientType) {
        return "auth:refresh:white:%d:%s".formatted(userId, clientType.name());
    }

    private String accessBlacklistKey(String jti) {
        return "auth:access:black:" + jti;
    }

    private String refreshBlacklistKey(String jti) {
        return "auth:refresh:black:" + jti;
    }

    private String hash(String token) {
        MessageDigest digest = createDigest();
        byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }

    private MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
