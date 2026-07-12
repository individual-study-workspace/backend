package com.tutoring.domain.classroom.repository;

import com.tutoring.domain.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    boolean existsByInviteCode(String inviteCode);
}
