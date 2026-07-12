package com.tutoring.domain.classroom.repository;

import com.tutoring.domain.classroom.entity.BillingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingPolicyRepository extends JpaRepository<BillingPolicy, Long> {
}
