package com.tutoring.domain.classroom.service;

import com.tutoring.domain.classroom.dto.ClassroomResponse;
import com.tutoring.domain.classroom.dto.CreateClassroomRequest;
import com.tutoring.domain.classroom.dto.InviteCodeResponse;

/**
 * 강의실 도메인 서비스.
 *
 * <p>강의실 생성과, 생성 전에 필요한 초대코드 발급을 담당한다.
 * 구현은 {@link ClassroomServiceImpl}.
 */
public interface ClassroomService {

    /**
     * 사용 가능한(유니크한) 초대코드를 발급한다.
     *
     * <p>FE는 강의실 생성 화면에서 이 API로 코드를 먼저 발급받고,
     * 받은 코드를 {@link #create} 요청의 {@code inviteCode} 로 담아 전달한다.
     * 발급된 코드는 DB에 저장(예약)하지 않으며, 최종 저장은 {@link #create} 에서 이뤄진다.
     *
     * @return 발급된 초대코드
     */
    InviteCodeResponse generateInviteCode();

    /**
     * 강의실 1개와 청구정책 1개(1:1)를 한 트랜잭션으로 생성한다.
     *
     * @param creatorId 강의실 소유자(생성자)의 사용자 ID
     * @param request   강의실·청구정책 생성 입력
     * @return 생성된 강의실 + 청구정책 응답
     */
    ClassroomResponse create(Long creatorId, CreateClassroomRequest request);
}
