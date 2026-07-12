package com.tutoring.domain.common.repository;

import com.tutoring.domain.common.entity.CodeManagement;
import com.tutoring.domain.common.entity.CodeManagementId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonCodeRepository extends JpaRepository<CodeManagement, CodeManagementId> {

    List<CodeManagement> findByCategoryOrderBySortOrder(String category);
}