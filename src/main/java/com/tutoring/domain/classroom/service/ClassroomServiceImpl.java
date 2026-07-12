package com.tutoring.domain.classroom.service;

import com.tutoring.domain.classroom.dto.ClassroomResponse;
import com.tutoring.domain.classroom.dto.CreateClassroomRequest;
import com.tutoring.domain.classroom.dto.CreateClassroomRequest.BillingPolicyRequest;
import com.tutoring.domain.classroom.dto.InviteCodeResponse;
import com.tutoring.domain.classroom.entity.BillingPolicy;
import com.tutoring.domain.classroom.entity.ClassType;
import com.tutoring.domain.classroom.entity.Classroom;
import com.tutoring.domain.classroom.entity.PaymentType;
import com.tutoring.domain.classroom.repository.BillingPolicyRepository;
import com.tutoring.domain.classroom.repository.ClassroomRepository;
import com.tutoring.global.error.ApiException;
import com.tutoring.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomServiceImpl implements ClassroomService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final int INVITE_CODE_MAX_RETRY = 10;

    private final ClassroomRepository classroomRepository;
    private final BillingPolicyRepository billingPolicyRepository;
    private final SecureRandom random = new SecureRandom();

    @Override
    public InviteCodeResponse generateInviteCode() {
        for (int i = 0; i < INVITE_CODE_MAX_RETRY; i++) {
            String code = randomCode();
            // DB에 이미 존재하는지 검증 후, 미사용 코드만 발급한다
            if (!classroomRepository.existsByInviteCode(code)) {
                return InviteCodeResponse.of(code);
            }
        }
        throw new ApiException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    @Override
    @Transactional
    public ClassroomResponse create(Long creatorId, CreateClassroomRequest request) {
        BillingPolicyRequest billing = request.billingPolicy();
        validateBillingPolicy(billing);

        // 초대코드는 FE가 발급받아 전달한 값을 저장한다 (유니크 검증)
        String inviteCode = request.inviteCode();
        if (classroomRepository.existsByInviteCode(inviteCode)) {
            throw new ApiException(ErrorCode.INVITE_CODE_DUPLICATE);
        }

        ClassType classType = request.classType() != null ? request.classType() : ClassType.ONLINE;

        Classroom classroom = Classroom.create(
            creatorId,
            request.name(),
            classType,
            request.repeatType(),
            request.classDays(),
            request.fromDate(),
            request.fromTime(),
            request.totalSessions(),
            inviteCode,
            request.remark()
        );
        Classroom savedClassroom = classroomRepository.save(classroom);

        // 청구 모델에 따라 사용하지 않는 필드는 null 로 저장
        Short billingDay = billing.paymentType() == PaymentType.MONTHLY ? billing.billingDay() : null;
        Short billingUnit = billing.paymentType() == PaymentType.PER_SESSION ? billing.billingUnit() : null;

        BillingPolicy policy = BillingPolicy.create(
            savedClassroom.getId(),
            billing.paymentType(),
            billingDay,
            billingUnit,
            billing.amount()
        );
        BillingPolicy savedPolicy = billingPolicyRepository.save(policy);

        return ClassroomResponse.of(savedClassroom, savedPolicy);
    }

    private void validateBillingPolicy(BillingPolicyRequest billing) {
        switch (billing.paymentType()) {
            case MONTHLY -> {
                if (billing.billingDay() == null) {
                    throw new ApiException(ErrorCode.CLASSROOM_INVALID_BILLING_POLICY,
                        "월별 청구는 billingDay(청구일)가 필요합니다");
                }
            }
            case PER_SESSION -> {
                if (billing.billingUnit() == null) {
                    throw new ApiException(ErrorCode.CLASSROOM_INVALID_BILLING_POLICY,
                        "회차별 청구는 billingUnit(청구 회차 간격)이 필요합니다");
                }
            }
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_ALPHABET.charAt(random.nextInt(INVITE_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
