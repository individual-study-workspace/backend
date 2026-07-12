package com.tutoring.domain.user.entity;

public enum Role {
    USER,
    TUTOR,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
