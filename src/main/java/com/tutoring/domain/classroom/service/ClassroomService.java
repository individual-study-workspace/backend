package com.tutoring.domain.classroom.service;

import com.tutoring.domain.classroom.dto.ClassroomResponse;
import com.tutoring.domain.classroom.dto.CreateClassroomRequest;
import com.tutoring.domain.classroom.dto.InviteCodeResponse;

public interface ClassroomService {

    /** 사용 가능한(유니크) 초대코드를 발급한다. FE가 발급받아 강의실 생성 요청에 담아 전달한다. */
    InviteCodeResponse generateInviteCode();

    ClassroomResponse create(Long creatorId, CreateClassroomRequest request);
}
