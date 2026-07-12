package com.tutoring.domain.classroom.dto;

/**
 * 초대코드 발급 응답. 서버가 생성한 유니크 코드를 담는다.
 *
 * @param inviteCode 발급된 8자 초대코드
 */
public record InviteCodeResponse(String inviteCode) {

    /** 초대코드 문자열로 응답 객체를 만든다. */
    public static InviteCodeResponse of(String inviteCode) {
        return new InviteCodeResponse(inviteCode);
    }
}
