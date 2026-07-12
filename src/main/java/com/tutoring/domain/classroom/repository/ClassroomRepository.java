package com.tutoring.domain.classroom.repository;

import com.tutoring.domain.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 강의실 JPA 리포지토리.
 *
 * <p>참고: {@code Classroom} 에 {@code @SQLRestriction("deleted_at IS NULL")} 이 걸려 있어
 * 모든 조회는 소프트 삭제되지 않은 행만 대상으로 한다.
 */
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    /**
     * 해당 초대코드를 가진 강의실이 이미 존재하는지 확인한다. (초대코드 유니크 검증용)
     *
     * @param inviteCode 확인할 초대코드
     * @return 존재하면 true
     */
    boolean existsByInviteCode(String inviteCode);
}
