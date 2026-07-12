package com.tutoring.domain.classroom.controller;

import com.tutoring.domain.classroom.dto.ClassroomResponse;
import com.tutoring.domain.classroom.dto.CreateClassroomRequest;
import com.tutoring.domain.classroom.dto.InviteCodeResponse;
import com.tutoring.domain.classroom.service.ClassroomService;
import com.tutoring.global.common.ApiResponse;
import com.tutoring.global.security.principal.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomController implements ClassroomApi {

    private final ClassroomService classroomService;

    @Override
    @GetMapping("/invite-code")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ApiResponse<InviteCodeResponse> generateInviteCode() {
        return ApiResponse.success(classroomService.generateInviteCode());
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ApiResponse<ClassroomResponse> create(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @Valid @RequestBody CreateClassroomRequest request
    ) {
        return ApiResponse.success(classroomService.create(principal.userId(), request));
    }
}
