package com.tutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
// JPA Auditing 활성화: @CreatedDate, @LastModifiedDate 어노테이션이 붙은 필드를
// Entity 저장/수정 시 자동으로 현재 시각을 채워준다.
// 이 어노테이션이 없으면 @CreatedDate 가 동작하지 않아 createdAt 이 null 로 저장됨.
@EnableJpaAuditing
public class TutoringApplication {

	public static void main(String[] args) {
		SpringApplication.run(TutoringApplication.class, args);
	}
}
