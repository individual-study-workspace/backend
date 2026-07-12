package com.tutoring.domain.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "code_management")
@IdClass(CodeManagementId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeManagement {

    @Id
    @Column(name = "category", length = 50)
    private String category;

    @Id
    @Column(name = "code_id", length = 50)
    private String codeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    public boolean isActive() {
        LocalDate today = LocalDate.now();
        if (today.isBefore(fromDate)) return false;
        return toDate == null || !today.isAfter(toDate);
    }
}