package com.tutoring.domain.classroom.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 청구정책 엔티티. 강의실({@link Classroom})과 1:1 로 연결된다({@code class_id} 는 UNIQUE).
 *
 * <p>결제 유형({@link PaymentType})에 따라 사용하는 필드가 다르다.
 * <ul>
 *   <li>{@link PaymentType#MONTHLY} — {@code billingDay}(매월 청구일) 사용, {@code billingUnit} 은 null</li>
 *   <li>{@link PaymentType#PER_SESSION} — {@code billingUnit}(몇 회차마다 청구) 사용, {@code billingDay} 는 null</li>
 * </ul>
 * 생성 시각만 감사 대상이라 BaseEntity 를 상속하지 않고 {@code @CreatedDate} 한 필드만 둔다.
 */
@Entity
@Table(
    name = "billing_policy",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_billing_policy_class_id",
        columnNames = "class_id"
    )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "billing_day")
    private Short billingDay;

    @Column(name = "billing_unit")
    private Short billingUnit;

    @Column(nullable = false)
    private Integer amount;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    private BillingPolicy(Long classId, PaymentType paymentType, Short billingDay,
                          Short billingUnit, Integer amount) {
        this.classId = classId;
        this.paymentType = paymentType;
        this.billingDay = billingDay;
        this.billingUnit = billingUnit;
        this.amount = amount;
    }

    /**
     * 새 청구정책을 생성한다.
     *
     * <p>호출부에서 결제 유형에 맞지 않는 필드는 미리 null 로 정규화해서 넘긴다
     * (MONTHLY 면 {@code billingUnit}=null, PER_SESSION 이면 {@code billingDay}=null).
     *
     * @param classId     연결할 강의실 ID (1:1)
     * @param paymentType 결제 유형 (MONTHLY/PER_SESSION)
     * @param billingDay  매월 청구일 (MONTHLY 에서만 사용, 그 외 null)
     * @param billingUnit 청구 회차 간격 (PER_SESSION 에서만 사용, 그 외 null)
     * @param amount      단위당 금액(원)
     * @return 생성된(아직 미영속) 청구정책 인스턴스
     */
    public static BillingPolicy create(Long classId, PaymentType paymentType, Short billingDay,
                                       Short billingUnit, Integer amount) {
        return new BillingPolicy(classId, paymentType, billingDay, billingUnit, amount);
    }
}
