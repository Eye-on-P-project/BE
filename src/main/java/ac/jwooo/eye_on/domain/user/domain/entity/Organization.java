package ac.jwooo.eye_on.domain.user.domain.entity;


import ac.jwooo.eye_on.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "organization")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends BaseEntity {

    @Id
    @Tsid
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(name = "businessman_num", nullable = false, length = 20)
    private String businessmanNum;

    @Column(name = "established_at", nullable = false)
    private LocalDate establishedAt;

    @Column(name = "representative_name", nullable = false, length = 100)
    private String representativeName;

    @Column(name = "co_representative_name", length = 100)
    private String coRepresentativeName;

    @Column(name = "corporate_num", nullable = false, length = 20)
    private String corporateNum;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "business_address", length = 255)
    private String businessAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationSubscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationStatus status;

    @Column(name = "reject_reason_codes", length = 255)
    private String rejectReasonCodes;

    @Column(name = "reject_reason_detail", length = 500)
    private String rejectReasonDetail;

    @Builder
    private Organization(
            String name,
            String code,
            String businessmanNum,
            LocalDate establishedAt,
            String representativeName,
            String coRepresentativeName,
            String corporateNum,
            String businessName,
            String businessAddress,
            OrganizationSubscription subscription,
            OrganizationStatus status,
            String rejectReasonCodes,
            String rejectReasonDetail
    ) {
        this.name = normalizeText(name);
        this.code = normalizeCode(code);
        this.businessmanNum = normalizeText(businessmanNum);
        this.establishedAt = establishedAt;
        this.representativeName = normalizeText(representativeName);
        this.coRepresentativeName = normalizeText(coRepresentativeName);
        this.corporateNum = normalizeText(corporateNum);
        this.businessName = normalizeText(businessName);
        this.businessAddress = normalizeText(businessAddress);
        this.subscription = subscription;
        this.status = status;
        this.rejectReasonCodes = normalizeText(rejectReasonCodes);
        this.rejectReasonDetail = normalizeText(rejectReasonDetail);
    }

    public static Organization createPending(
            String name,
            String code,
            String businessmanNum,
            LocalDate establishedAt,
            String representativeName,
            String corporateNum,
            String businessName,
            String coRepresentativeName,
            String businessAddress
    ) {
        return Organization.builder()
                .name(name)
                .code(code)
                .businessmanNum(businessmanNum)
                .establishedAt(establishedAt)
                .representativeName(representativeName)
                .coRepresentativeName(coRepresentativeName)
                .corporateNum(corporateNum)
                .businessName(businessName)
                .businessAddress(businessAddress)
                .subscription(OrganizationSubscription.FREE)
                .status(OrganizationStatus.PENDING)
                .build();
    }

    public void approve() {
        this.status = OrganizationStatus.ACTIVE;
        this.rejectReasonCodes = null;
        this.rejectReasonDetail = null;
    }

    public void reject(String rejectReasonCodes, String rejectReasonDetail) {
        this.status = OrganizationStatus.REJECTED;
        this.rejectReasonCodes = normalizeText(rejectReasonCodes);
        this.rejectReasonDetail = normalizeText(rejectReasonDetail);
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}
