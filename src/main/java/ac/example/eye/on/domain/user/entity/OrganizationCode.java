package ac.example.eye.on.domain.user.entity;

import ac.example.eye.on.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "organization_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(length = 255)
    private String description;

    @Builder
    private OrganizationCode(String code, String description) {
        this.code = normalizeCode(code);
        this.description = description;
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}

