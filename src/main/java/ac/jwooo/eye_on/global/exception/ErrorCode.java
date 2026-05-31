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
    ORGANIZATION_ADMIN_ALREADY_EXISTS(HttpStatus.CONFLICT, "ORGANIZATION_ADMIN_ALREADY_EXISTS", "해당 조직에는 이미 관리자 계정이 존재합니다."),
    USER_PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "USER_PROFILE_REQUIRED", "일반 사용자 회원가입 필수 값이 누락되었습니다."),
    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "조직을 찾을 수 없습니다."),
    ORGANIZATION_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "ORGANIZATION_ADMIN_REQUIRED", "조직 관리자만 구성원을 관리할 수 있습니다."),
    ORGANIZATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORGANIZATION_ACCESS_DENIED", "해당 조직에 접근할 수 없습니다."),
    ORGANIZATION_MEMBER_ADMIN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ORGANIZATION_MEMBER_ADMIN_NOT_ALLOWED", "관리자 계정은 구성원으로 추가할 수 없습니다."),
    INVALID_MONITORING_TIME_RANGE(HttpStatus.BAD_REQUEST, "INVALID_MONITORING_TIME_RANGE", "모니터링 시간 범위가 올바르지 않습니다."),
    INVALID_MONITORING_EVENT_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_MONITORING_EVENT_REQUEST", "모니터링 이벤트 요청 형식이 올바르지 않습니다."),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
    ORGANIZATION_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "ORGANIZATION_MEMBER_ALREADY_EXISTS", "이미 해당 조직에 추가된 구성원입니다."),
    MONITORING_SESSION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "MONITORING_SESSION_ALREADY_ACTIVE", "이미 진행 중인 모니터링 세션이 있습니다."),
    MONITORING_SESSION_ALREADY_ENDED(HttpStatus.CONFLICT, "MONITORING_SESSION_ALREADY_ENDED", "이미 종료된 모니터링 세션입니다."),
    MONITORING_EVENT_ALREADY_RESOLVED(HttpStatus.CONFLICT, "MONITORING_EVENT_ALREADY_RESOLVED", "이미 종료 처리된 이벤트입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다."),
    NEW_PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "NEW_PASSWORD_SAME_AS_OLD", "새 비밀번호는 기존 비밀번호와 달라야 합니다."),
    ORGANIZATION_CODE_MISMATCH(HttpStatus.FORBIDDEN, "ORGANIZATION_CODE_MISMATCH", "조직 코드가 일치하지 않습니다."),
    WEB_ADMIN_LOGIN_ONLY(HttpStatus.FORBIDDEN, "WEB_ADMIN_LOGIN_ONLY", "웹 로그인은 조직 관리자 계정만 가능합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    ORGANIZATION_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORGANIZATION_MEMBER_NOT_FOUND", "구성원 정보를 찾을 수 없습니다."),
    MONITORING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MONITORING_SESSION_NOT_FOUND", "모니터링 세션을 찾을 수 없습니다."),
    MONITORING_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "MONITORING_EVENT_NOT_FOUND", "모니터링 이벤트를 찾을 수 없습니다."),

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
