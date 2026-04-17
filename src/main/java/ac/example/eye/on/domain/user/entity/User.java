package ac.example.eye.on.domain.user.entity;

import ac.example.eye.on.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 191)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "organization_code", length = 100)
    private String organizationCode;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String nickname;

    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Builder(access = AccessLevel.PRIVATE)
    private User(
            String email,
            String passwordHash,
            UserRole role,
            String organizationCode,
            String name,
            String nickname,
            Integer age,
            Gender gender
    ) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.organizationCode = organizationCode;
        this.name = name;
        this.nickname = nickname;
        this.age = age;
        this.gender = gender;
    }

    public static User createAdmin(String email, String passwordHash, String organizationCode) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .role(UserRole.ADMIN)
                .organizationCode(organizationCode)
                .build();
    }

    public static User createGeneralUser(
            String email,
            String passwordHash,
            String name,
            String nickname,
            Integer age,
            Gender gender
    ) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .role(UserRole.USER)
                .name(name)
                .nickname(nickname)
                .age(age)
                .gender(gender)
                .build();
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

