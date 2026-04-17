package ac.example.eye.on.domain.auth.model;

import java.util.Locale;

import ac.example.eye.on.global.exception.CustomException;
import ac.example.eye.on.global.exception.ErrorCode;

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
