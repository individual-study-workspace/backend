package com.tutoring.domain.common.entity;

import java.io.Serializable;
import java.util.Objects;

public class CodeManagementId implements Serializable {

    private String category;
    private String codeId;

    protected CodeManagementId() {}

    public CodeManagementId(String category, String codeId) {
        this.category = category;
        this.codeId = codeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CodeManagementId that)) return false;
        return Objects.equals(category, that.category)
            && Objects.equals(codeId, that.codeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, codeId);
    }
}