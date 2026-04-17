package ac.example.eye.on.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender {
    MALE,
    FEMALE;

    @JsonCreator
    public static Gender from(String value) {
        if (value == null) {
            return null;
        }

        return switch (value.trim().toUpperCase()) {
            case "MALE", "MAN", "M", "남", "남자" -> MALE;
            case "FEMALE", "WOMAN", "F", "여", "여자" -> FEMALE;
            default -> throw new IllegalArgumentException("지원하지 않는 성별 값입니다: " + value);
        };
    }
}

