package ac.jwooo.eye_on.domain.auth.domain.entity;

import java.util.Locale;

import ac.jwooo.eye_on.global.exception.CustomException;
import ac.jwooo.eye_on.global.exception.ErrorCode;

public enum ClientType {
    WEB,
    APP;

    public static ClientType fromHeader(String value) {
        if (value == null || value.isBlank()) {
            return APP;
        }
        return parse(value);
    }

    public static ClientType fromTokenClaim(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        return parse(value);
    }

    private static ClientType parse(String value) {
        try {
            return ClientType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CLIENT_TYPE);
        }
    }
}
