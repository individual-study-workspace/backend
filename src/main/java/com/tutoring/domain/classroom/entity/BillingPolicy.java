package com.tutoring.domain.classroom.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

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

    public static BillingPolicy create(Long classId, PaymentType paymentType, Short billingDay,
                                       Short billingUnit, Integer amount) {
        return new BillingPolicy(classId, paymentType, billingDay, billingUnit, amount);
    }
}
