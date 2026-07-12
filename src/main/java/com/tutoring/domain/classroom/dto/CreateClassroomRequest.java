package com.tutoring.domain.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tutoring.domain.classroom.entity.ClassType;
import com.tutoring.domain.classroom.entity.PaymentType;
import com.tutoring.domain.classroom.entity.RepeatType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateClassroomRequest(
    @NotBlank @Size(max = 20) String name,
    ClassType classType,
    RepeatType repeatType,
    List<DayOfWeek> classDays,
    LocalDate fromDate,
    @JsonFormat(pattern = "HH:mm") LocalTime fromTime,
    @Positive Short totalSessions,
    @Size(max = 500) String remark,
    @NotNull @Valid BillingPolicyRequest billingPolicy
) {
    public record BillingPolicyRequest(
        @NotNull PaymentType paymentType,
        @Min(1) @Max(28) Short billingDay,
        @Min(1) Short billingUnit,
        @NotNull @Positive Integer amount
    ) {}
}
