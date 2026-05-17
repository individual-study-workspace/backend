package com.tutoring.global.security.principal;

import com.tutoring.domain.user.entity.Role;

public record CustomUserPrincipal(Long userId, Role role) {}
