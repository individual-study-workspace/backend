package com.tutoring.domain.classroom.controller;

import com.tutoring.domain.classroom.dto.ClassroomResponse;
import com.tutoring.domain.classroom.dto.CreateClassroomRequest;
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
        summary = "강의실 생성",
        description = """
            TUTOR 이상(TUTOR·ADMIN) 권한으로 강의실과 청구정책을 한 번에 생성한다.
            - 초대코드(invite_code)는 서버에서 자동 발급된다.
            - 청구정책: MONTHLY는 billingDay(1~28) 필수, PER_SESSION은 billingUnit(≥1) 필수.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 오류 또는 청구정책 검증 실패"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (TUTOR 미만)")
    })
    com.tutoring.global.common.ApiResponse<ClassroomResponse> create(
        CustomUserPrincipal principal,
        CreateClassroomRequest request
    );
}
