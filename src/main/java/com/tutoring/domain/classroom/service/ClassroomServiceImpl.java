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

/**
 * 강의실 도메인 서비스 구현체.
 *
 * <p>담당 기능:
 * <ul>
 *   <li>초대코드 발급 ({@link #generateInviteCode()}) — FE가 강의실 생성 전에 호출</li>
 *   <li>강의실 + 청구정책 생성 ({@link #create(Long, CreateClassroomRequest)}) — 한 트랜잭션</li>
 * </ul>
 *
 * <p>클래스 기본 트랜잭션은 {@code readOnly = true} 이며, 쓰기가 필요한 {@link #create}에만
 * {@code @Transactional} 을 별도로 붙여 읽기/쓰기를 구분한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomServiceImpl implements ClassroomService {

    /** 초대코드에 사용하는 안전 문자셋. 사람이 헷갈리는 문자(I, O, 0, 1)는 제외했다. */
    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    /** 초대코드 길이(자릿수). */
    private static final int INVITE_CODE_LENGTH = 8;
    /** 초대코드 중복 발생 시 재생성 최대 횟수. 이 횟수를 넘기면 발급 실패로 간주한다. */
    private static final int INVITE_CODE_MAX_RETRY = 10;

    private final ClassroomRepository classroomRepository;
    private final BillingPolicyRepository billingPolicyRepository;
    /** 초대코드 난수 생성기. 예측을 어렵게 하기 위해 {@link SecureRandom} 을 사용한다. */
    private final SecureRandom random = new SecureRandom();

    /**
     * 사용 가능한(DB에 없는) 초대코드를 발급한다.
     *
     * <p>동작 흐름:
     * <ol>
     *   <li>안전 문자셋에서 {@value #INVITE_CODE_LENGTH}자 난수 코드를 생성한다.</li>
     *   <li>{@code existsByInviteCode} 로 DB에 이미 존재하는지 검증한다.</li>
     *   <li>존재하지 않으면(미사용) 그 코드를 즉시 반환한다.</li>
     *   <li>중복이면 최대 {@value #INVITE_CODE_MAX_RETRY}회까지 1~3단계를 반복한다.</li>
     * </ol>
     * 재시도 한도 내에 성공하지 못하면 {@link ErrorCode#INVITE_CODE_GENERATION_FAILED} 예외를 던진다.
     *
     * <p>발급한 코드는 DB에 저장(예약)하지 않는다. FE가 이 값을 강의실 생성 요청의 {@code inviteCode}
     * 로 담아 보내면, 최종 저장·중복 검증은 {@link #create(Long, CreateClassroomRequest)} 에서 수행한다.
     *
     * @return 발급된 초대코드를 담은 응답
     */
    @Override
    public InviteCodeResponse generateInviteCode() {
        for (int i = 0; i < INVITE_CODE_MAX_RETRY; i++) {
            String code = randomCode();
            // DB에 이미 존재하는지 검증 후, 미사용 코드만 발급한다
            if (!classroomRepository.existsByInviteCode(code)) {
                return InviteCodeResponse.of(code);
            }
        }
        // 재시도 한도 초과 — 사실상 발생하기 어려우나(32^8 공간) 무한루프 방지를 위한 안전장치
        throw new ApiException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    /**
     * 강의실 1개와 청구정책 1개(1:1)를 한 트랜잭션으로 생성한다.
     *
     * <p>동작 흐름:
     * <ol>
     *   <li>청구정책 교차검증 — 결제 유형별 필수 필드 확인 ({@link #validateBillingPolicy}).</li>
     *   <li>FE가 전달한 초대코드의 DB 중복 검증 — 이미 사용 중이면
     *       {@link ErrorCode#INVITE_CODE_DUPLICATE}(409).</li>
     *   <li>강의실 저장 — {@code classType} 미지정 시 {@link ClassType#ONLINE} 기본,
     *       {@code created_by} 는 생성자(소유자) ID.</li>
     *   <li>청구정책 저장 — 결제 유형과 무관한 필드는 null 로 정규화하여
     *       방금 저장된 강의실 ID와 1:1 로 연결.</li>
     *   <li>생성 결과를 응답 DTO로 변환하여 반환.</li>
     * </ol>
     * 어느 단계에서든 예외가 발생하면 트랜잭션 전체가 롤백된다(강의실만 저장되고 청구정책이 빠지는 일 없음).
     *
     * @param creatorId 강의실 소유자(생성자)의 사용자 ID — 인증 주체(principal)에서 전달된다
     * @param request   강의실·청구정책 생성 입력 (Bean Validation 통과한 값)
     * @return 생성된 강의실과 청구정책 정보를 담은 응답
     */
    @Override
    @Transactional
    public ClassroomResponse create(Long creatorId, CreateClassroomRequest request) {
        BillingPolicyRequest billing = request.billingPolicy();
        validateBillingPolicy(billing);

        // 초대코드는 FE가 발급받아 전달한 값을 저장한다. 저장 전 DB 중복을 재검증(예약이 없으므로 필수)
        String inviteCode = request.inviteCode();
        if (classroomRepository.existsByInviteCode(inviteCode)) {
            throw new ApiException(ErrorCode.INVITE_CODE_DUPLICATE);
        }

        // classType 은 선택 입력 — 없으면 ONLINE 을 기본값으로 사용한다
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
        // IDENTITY 전략이라 save 시점에 즉시 INSERT 되어 id 가 채워진다 → 아래 청구정책 FK 로 사용
        Classroom savedClassroom = classroomRepository.save(classroom);

        // 결제 유형에 해당하지 않는 청구 필드는 null 로 정규화한다
        // (MONTHLY 는 billingDay 만, PER_SESSION 은 billingUnit 만 의미가 있음)
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

    /**
     * 결제 유형별로 필수인 청구 필드가 채워졌는지 검증한다.
     *
     * <ul>
     *   <li>{@link PaymentType#MONTHLY} → {@code billingDay}(매월 청구일)가 필수</li>
     *   <li>{@link PaymentType#PER_SESSION} → {@code billingUnit}(몇 회차마다 청구할지)이 필수</li>
     * </ul>
     * 필수 값이 없으면 {@link ErrorCode#CLASSROOM_INVALID_BILLING_POLICY}(400) 예외를 던진다.
     *
     * @param billing 검증 대상 청구정책 입력
     */
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

    /**
     * 안전 문자셋({@link #INVITE_CODE_ALPHABET})에서 {@value #INVITE_CODE_LENGTH}자짜리 난수 코드를 만든다.
     *
     * <p>유니크를 보장하지 않는 순수 난수 생성이며, 중복 확인은 호출부에서 DB로 수행한다.
     *
     * @return 생성된 코드 문자열
     */
    private String randomCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_ALPHABET.charAt(random.nextInt(INVITE_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
