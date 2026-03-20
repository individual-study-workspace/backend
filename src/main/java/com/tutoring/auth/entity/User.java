package com.tutoring.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
// access = AccessLevel.PROTECTED: 기본 생성자(new User())를 외부에서 직접 호출하지 못하게 막는다.
// JPA 는 내부적으로 기본 생성자가 필요하므로 완전히 막을 수는 없고(PRIVATE 불가),
// PROTECTED 로 두어 "Builder 를 통해서만 생성하라"는 의도를 코드로 표현한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// AuditingEntityListener: Entity 가 저장/수정될 때 @CreatedDate, @LastModifiedDate 필드를
// 자동으로 채워주는 JPA 리스너. TutoringApplication 의 @EnableJpaAuditing 과 함께 동작한다.
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 비밀번호 변경, 프로필 수정 등 이력 추적을 위해 updatedAt 추가.
    // @LastModifiedDate: Entity 가 수정될 때마다 자동으로 현재 시각으로 갱신된다.
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // @Builder: 빌더 패턴 적용. new User("a@b.com", ...) 처럼 순서에 의존하는 대신
    // User.builder().email("a@b.com").password("...").name("홍길동").role(Role.STUDENT).build()
    // 형태로 필드명을 명시해서 객체를 생성할 수 있어 가독성과 안전성이 높아진다.
    // 생성자에 붙이면 해당 생성자 파라미터만 빌더에 포함된다. (id, createdAt 등 자동 생성 필드 제외)
    @Builder
    public User(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }
}