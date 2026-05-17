package com.tutoring.domain.user.repository;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
