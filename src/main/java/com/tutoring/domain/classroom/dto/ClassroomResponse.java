package com.tutoring.domain.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tutoring.domain.classroom.entity.BillingPolicy;
import com.tutoring.domain.classroom.entity.ClassType;
import com.tutoring.domain.classroom.entity.Classroom;
import com.tutoring.domain.classroom.entity.PaymentType;
import com.tutoring.domain.classroom.entity.RepeatType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 강의실 생성 결과 응답. 강의실 정보와 청구정책(중첩)을 함께 담는다.
 * 엔티티를 직접 노출하지 않고 {@link #of} 로 변환해 반환한다.
 */
public record ClassroomResponse(
    Long id,
    String name,
    ClassType classType,
    RepeatType repeatType,
    List<DayOfWeek> classDays,
    LocalDate fromDate,
    @JsonFormat(pattern = "HH:mm") LocalTime fromTime,
    Short totalSessions,
    String inviteCode,
    String remark,
    Long createdBy,
    Instant createdAt,
    BillingPolicyResponse billingPolicy
) {
    /**
     * 강의실·청구정책 엔티티를 응답 DTO로 변환한다.
     *
     * @param classroom     저장된 강의실 엔티티
     * @param billingPolicy 저장된 청구정책 엔티티
     * @return 변환된 응답
     */
    public static ClassroomResponse of(Classroom classroom, BillingPolicy billingPolicy) {
        return new ClassroomResponse(
            classroom.getId(),
            classroom.getName(),
            classroom.getClassType(),
            classroom.getRepeatType(),
            classroom.getClassDays(),
            classroom.getFromDate(),
            classroom.getFromTime(),
            classroom.getTotalSessions(),
            classroom.getInviteCode(),
            classroom.getRemark(),
            classroom.getCreatedBy(),
            classroom.getCreatedAt(),
            BillingPolicyResponse.from(billingPolicy)
        );
    }

    /** 응답에 포함되는 청구정책 정보. */
    public record BillingPolicyResponse(
        PaymentType paymentType,
        Short billingDay,
        Short billingUnit,
        Integer amount
    ) {
        /** 청구정책 엔티티를 응답 DTO로 변환한다. */
        public static BillingPolicyResponse from(BillingPolicy policy) {
            return new BillingPolicyResponse(
                policy.getPaymentType(),
                policy.getBillingDay(),
                policy.getBillingUnit(),
                policy.getAmount()
            );
        }
    }
}
