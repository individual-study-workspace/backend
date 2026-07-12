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

    public static Classroom create(Long createdBy, String name, ClassType classType, RepeatType repeatType,
                                   List<DayOfWeek> classDays, LocalDate fromDate, LocalTime fromTime,
                                   Short totalSessions, String inviteCode, String remark) {
        return new Classroom(createdBy, name, classType, repeatType, classDays, fromDate, fromTime,
            totalSessions, inviteCode, remark);
    }
}
