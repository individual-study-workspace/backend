package com.tutoring.domain.classroom.entity;

/** 청구(결제) 유형. */
public enum PaymentType {
    /** 월별 청구 — 매월 billingDay 에 amount 를 청구. */
    MONTHLY,
    /** 회차별 청구 — billingUnit 회차마다 amount 를 청구. */
    PER_SESSION
}
