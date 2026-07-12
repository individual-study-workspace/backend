package com.tutoring.domain.classroom.service;

import com.tutoring.domain.classroom.dto.ClassroomResponse;
import com.tutoring.domain.classroom.dto.CreateClassroomRequest;

public interface ClassroomService {

    ClassroomResponse create(Long creatorId, CreateClassroomRequest request);
}
