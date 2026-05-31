package ac.jwooo.eye_on.domain.user.domain.entity;

import ac.jwooo.eye_on.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    @Tsid
    private Long id;

    @Column(nullable = false, unique = true, length = 191)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "organization", length = 100)
    private String organization;

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
            String organization,
            String name,
            String nickname,
            Integer age,
            Gender gender
    ) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.organization = organization;
        this.name = name;
        this.nickname = nickname;
        this.age = age;
        this.gender = gender;
    }

    public static User createAdmin(String email, String passwordHash, String organization) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .role(UserRole.ADMIN)
                .organization(organization)
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

    public void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
    }
}
