package com.tutoring.domain.classroom.controller;

import com.tutoring.domain.classroom.dto.ClassroomResponse;
import com.tutoring.domain.classroom.dto.CreateClassroomRequest;
import com.tutoring.domain.classroom.dto.InviteCodeResponse;
import com.tutoring.global.security.principal.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 강의실 API 문서 (OpenAPI/Swagger).
 * 컨트롤러는 이 인터페이스를 implements 하여 문서 애노테이션과 구현을 분리한다.
 */
@Tag(name = "Classroom", description = "강의실 API")
public interface ClassroomApi {

    @Operation(
        summary = "초대코드 발급",
        description = "TUTOR 이상(TUTOR·ADMIN) 권한으로 사용 가능한 유니크 초대코드를 발급받는다. 반환된 코드를 강의실 생성 요청의 inviteCode 로 전달한다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "발급 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (TUTOR 미만)")
    })
    com.tutoring.global.common.ApiResponse<InviteCodeResponse> generateInviteCode();

    @Operation(
        summary = "강의실 생성",
        description = """
            TUTOR 이상(TUTOR·ADMIN) 권한으로 강의실과 청구정책을 한 번에 생성한다.
            - 초대코드(inviteCode)는 발급 API(GET /api/v1/classrooms/invite-code)로 받은 값을 요청 본문에 담아 전달한다.
            - 청구정책: MONTHLY는 billingDay(1~28) 필수, PER_SESSION은 billingUnit(≥1) 필수.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 오류 또는 청구정책 검증 실패"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (TUTOR 미만)"),
        @ApiResponse(responseCode = "409", description = "이미 사용 중인 초대코드")
    })
    com.tutoring.global.common.ApiResponse<ClassroomResponse> create(
        CustomUserPrincipal principal,
        CreateClassroomRequest request
    );
}
