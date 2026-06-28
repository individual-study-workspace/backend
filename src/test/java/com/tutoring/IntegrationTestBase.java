package com.tutoring;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    // 싱글톤 컨테이너 패턴: 여러 통합 테스트 클래스가 static 컨테이너를 공유한다.
    // @Testcontainers/@Container 를 쓰면 클래스마다 stop 되어, reuse 가 비활성인 CI 에서
    // 두 번째 클래스부터 죽은 컨테이너에 붙어 ConnectException 이 난다.
    // 따라서 수동 start 만 하고 lifecycle 은 JVM 종료 시 Ryuk 에 맡긴다.
    static final MySQLContainer<?> mysql =
        new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("tutoring")
            .withUsername("tutoring")
            .withPassword("tutoring")
            .withReuse(true);

    static final GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    static {
        mysql.start();
        redis.start();
    }

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.data.redis.host",     redis::getHost);
        r.add("spring.data.redis.port",     () -> redis.getMappedPort(6379));
    }
}
