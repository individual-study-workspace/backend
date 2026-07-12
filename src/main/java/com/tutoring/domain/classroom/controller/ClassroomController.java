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

/**
 * 강의실 REST 컨트롤러.
 *
 * <p>HTTP 매핑·인가·요청 바인딩만 담당하고 실제 로직은 {@link ClassroomService} 에 위임한다.
 * API 문서(OpenAPI/Swagger) 애노테이션은 {@link ClassroomApi} 인터페이스로 분리되어 있으며,
 * 이 컨트롤러는 해당 인터페이스를 구현한다.
 */
@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomController implements ClassroomApi {

    private final ClassroomService classroomService;

    /**
     * 초대코드 발급. {@code GET /api/v1/classrooms/invite-code} (TUTOR·ADMIN)
     *
     * <p>서버가 유니크한 초대코드를 생성해 반환한다. FE는 이 값을 {@link #create} 요청에 담아 보낸다.
     *
     * @return 발급된 초대코드 응답 (HTTP 200)
     */
    @Override
    @GetMapping("/invite-code")
    @PreAuthorize("hasAnyRole('TUTOR', 'ADMIN')")
    public ApiResponse<InviteCodeResponse> generateInviteCode() {
        return ApiResponse.success(classroomService.generateInviteCode());
    }

    /**
     * 강의실 생성. {@code POST /api/v1/classrooms} (TUTOR·ADMIN)
     *
     * <p>인증 주체(principal)를 강의실 소유자로 삼아, 강의실과 청구정책을 함께 생성한다.
     * 요청 본문은 {@code @Valid} 로 검증되며 성공 시 HTTP 201 을 반환한다.
     *
     * @param principal 인증된 사용자(생성자). {@code @AuthenticationPrincipal} 로 주입된다
     * @param request   강의실·청구정책 생성 입력
     * @return 생성된 강의실 + 청구정책 응답 (HTTP 201)
     */
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
