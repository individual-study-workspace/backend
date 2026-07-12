package com.tutoring.domain.classroom.dto;

public record InviteCodeResponse(String inviteCode) {

    public static InviteCodeResponse of(String inviteCode) {
        return new InviteCodeResponse(inviteCode);
    }
}
