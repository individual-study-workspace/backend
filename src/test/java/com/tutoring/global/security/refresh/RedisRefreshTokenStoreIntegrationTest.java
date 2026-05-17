package com.tutoring.global.security.refresh;

import com.tutoring.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RedisRefreshTokenStoreIntegrationTest extends IntegrationTestBase {

    @Autowired RefreshTokenStore store;

    @Test
    void save_and_find_token() {
        store.save(1L, "refresh-token-abc", Duration.ofMinutes(5));

        Optional<String> found = store.find(1L);

        assertThat(found).contains("refresh-token-abc");
    }

    @Test
    void delete_removes_token() {
        store.save(2L, "refresh-token-xyz", Duration.ofMinutes(5));
        store.delete(2L);

        assertThat(store.find(2L)).isEmpty();
    }

    @Test
    void overwrite_replaces_previous_token() {
        store.save(3L, "old", Duration.ofMinutes(5));
        store.save(3L, "new", Duration.ofMinutes(5));

        assertThat(store.find(3L)).contains("new");
    }
}
