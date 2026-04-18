package ac.jwooo.eye_on.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "요청 값이 올바르지 않습니다."),
    INVALID_CLIENT_TYPE(HttpStatus.BAD_REQUEST, "INVALID_CLIENT_TYPE", "클라이언트 타입이 올바르지 않습니다."),
    ORGANIZATION_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "ORGANIZATION_CODE_REQUIRED", "관리자 회원가입에는 조직 코드가 필요합니다."),
    ORGANIZATION_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "ORGANIZATION_CODE_NOT_FOUND", "유효한 조직 코드가 아닙니다."),
    USER_PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "USER_PROFILE_REQUIRED", "일반 사용자 회원가입 필수 값이 누락되었습니다."),
    ORGANIZATION_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "ORGANIZATION_RECORD_NOT_FOUND", "조직 레코드를 찾을 수 없습니다."),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
    ORGANIZATION_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "ORGANIZATION_CODE_ALREADY_EXISTS", "이미 존재하는 조직 코드입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),

    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "TOKEN_NOT_FOUND", "토큰이 존재하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "만료된 토큰입니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "TOKEN_BLACKLISTED", "차단된 토큰입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISMATCH", "리프레시 토큰이 화이트리스트와 일치하지 않습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
