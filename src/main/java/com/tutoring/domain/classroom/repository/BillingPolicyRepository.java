package com.tutoring.domain.classroom.repository;

import com.tutoring.domain.classroom.entity.BillingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

/** 청구정책 JPA 리포지토리. (강의실과 1:1) */
public interface BillingPolicyRepository extends JpaRepository<BillingPolicy, Long> {
}
