package com.tutoring.domain.user.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    private static class TestEntity extends BaseEntity {}

    @Test
    void initial_state_is_not_deleted() {
        TestEntity e = new TestEntity();
        assertThat(e.isDeleted()).isFalse();
        assertThat(e.getDeletedAt()).isNull();
    }

    @Test
    void markDeleted_sets_deletedAt_timestamp() {
        TestEntity e = new TestEntity();
        Instant before = Instant.now();
        e.markDeleted();
        Instant after = Instant.now();

        assertThat(e.isDeleted()).isTrue();
        assertThat(e.getDeletedAt()).isBetween(before, after);
    }
}
