package com.tutoring.domain.classroom.entity;

import com.tutoring.domain.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 강의실 엔티티.
 *
 * <p>선생님(소유자)이 만드는 수업 단위이며, 청구정책({@link BillingPolicy})과 1:1 로 연결된다.
 * 생성/수정/삭제 시각은 {@link BaseEntity} 가 관리하고, 삭제는 물리 삭제 대신
 * {@code deleted_at} 을 채우는 소프트 삭제({@code @SQLRestriction} 로 조회 시 자동 제외)를 사용한다.
 *
 * <p>인스턴스 생성은 정적 팩토리 {@link #create} 로만 하도록 기본 생성자를 protected 로 제한한다.
 */
@Entity
@Table(
    name = "classroom",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_classroom_invite_code",
        columnNames = "invite_code"
    )
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classroom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", nullable = false, length = 10)
    private ClassType classType;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", length = 20)
    private RepeatType repeatType;

    @Convert(converter = ClassDaysConverter.class)
    @Column(name = "class_days", length = 30)
    private List<DayOfWeek> classDays;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "from_time")
    private LocalTime fromTime;

    @Column(name = "total_sessions")
    private Short totalSessions;

    @Column(name = "invite_code", length = 8)
    private String inviteCode;

    @Column(length = 500)
    private String remark;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deleted_by")
    private Long deletedBy;

    private Classroom(Long createdBy, String name, ClassType classType, RepeatType repeatType,
                      List<DayOfWeek> classDays, LocalDate fromDate, LocalTime fromTime,
                      Short totalSessions, String inviteCode, String remark) {
        this.createdBy = createdBy;
        this.name = name;
        this.classType = classType;
        this.repeatType = repeatType;
        this.classDays = classDays;
        this.fromDate = fromDate;
        this.fromTime = fromTime;
        this.totalSessions = totalSessions;
        this.inviteCode = inviteCode;
        this.remark = remark;
    }

    /**
     * 새 강의실을 생성한다. id·생성시각 등 감사 필드는 영속화 시점에 자동으로 채워진다.
     *
     * @param createdBy     강의실 소유자(생성자)의 사용자 ID
     * @param name          강의실 이름 (최대 20자)
     * @param classType     수업 형태 (ONLINE/OFFLINE/HYBRID)
     * @param repeatType    반복 유형 (매주/격주/1회성), 없을 수 있음
     * @param classDays     수업 요일 목록 (CSV 로 저장됨), 없을 수 있음
     * @param fromDate      시작일, 없을 수 있음
     * @param fromTime      시작 시각, 없을 수 있음
     * @param totalSessions 총 회차 수, 없을 수 있음
     * @param inviteCode    초대코드 (발급 API로 받은 8자 코드)
     * @param remark        비고 (최대 500자), 없을 수 있음
     * @return 생성된(아직 미영속) 강의실 인스턴스
     */
    public static Classroom create(Long createdBy, String name, ClassType classType, RepeatType repeatType,
                                   List<DayOfWeek> classDays, LocalDate fromDate, LocalTime fromTime,
                                   Short totalSessions, String inviteCode, String remark) {
        return new Classroom(createdBy, name, classType, repeatType, classDays, fromDate, fromTime,
            totalSessions, inviteCode, remark);
    }
}
