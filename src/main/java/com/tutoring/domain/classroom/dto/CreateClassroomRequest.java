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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 강의실 생성 요청 본문.
 *
 * <p>강의실 기본 정보 + 초대코드 + 청구정책(중첩)을 함께 받는다.
 * 필드 제약은 Bean Validation 애노테이션으로 표현하며, 결제 유형별 필수 필드 같은
 * 교차검증은 서비스 계층에서 수행한다.
 */
public record CreateClassroomRequest(
    @NotBlank @Size(max = 20) String name,
    ClassType classType,
    RepeatType repeatType,
    List<DayOfWeek> classDays,
    LocalDate fromDate,
    @JsonFormat(pattern = "HH:mm") LocalTime fromTime,
    @Positive Short totalSessions,
    @Size(max = 500) String remark,
    // 초대코드는 FE가 GET /api/v1/classrooms/invite-code 로 발급받아 그대로 전달한다
    @NotBlank
    @Pattern(regexp = "^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$", message = "초대코드 형식이 올바르지 않습니다")
    String inviteCode,
    @NotNull @Valid BillingPolicyRequest billingPolicy
) {
    /**
     * 청구정책 입력 (강의실과 1:1).
     * MONTHLY 는 {@code billingDay}, PER_SESSION 은 {@code billingUnit} 이 필수이며,
     * 이 필수 여부는 서비스에서 교차검증한다.
     */
    public record BillingPolicyRequest(
        @NotNull PaymentType paymentType,
        @Min(1) @Max(28) Short billingDay,
        @Min(1) Short billingUnit,
        @NotNull @Positive Integer amount
    ) {}
}
