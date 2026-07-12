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

    public record BillingPolicyResponse(
        PaymentType paymentType,
        Short billingDay,
        Short billingUnit,
        Integer amount
    ) {
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
