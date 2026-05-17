package com.tutoring.domain.user.repository;

import com.tutoring.IntegrationTestBase;
import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UserRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;

    @Test
    void save_and_find_by_provider_and_provider_id() {
        User saved = userRepository.save(UserFixture.googleUser());

        Optional<User> found = userRepository.findByProviderAndProviderId(
            AuthProvider.GOOGLE, "google-sub-123");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void soft_deleted_user_is_excluded_from_default_query() {
        User user = userRepository.save(UserFixture.googleUser());
        user.markDeleted();
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByProviderAndProviderId(
            AuthProvider.GOOGLE, "google-sub-123");

        assertThat(found).isEmpty();
    }
}
