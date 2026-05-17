package com.tutoring.global.common;

import java.util.List;

public record PageResponse<T>(List<T> content, Pagination pagination) {}
