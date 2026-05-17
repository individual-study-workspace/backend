package com.tutoring.global.security.oauth2;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomOAuth2UserServiceTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final CustomOAuth2UserService service = new CustomOAuth2UserService(repo);

    @Test
    void upsert_creates_when_user_not_found() {
        when(repo.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.upsert(AuthProvider.GOOGLE, "sub-1", "e@e.com", "name", "img");

        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        verify(repo).save(any(User.class));
    }

    @Test
    void upsert_updates_profile_when_user_exists() {
        User existing = UserFixture.googleUser();
        when(repo.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-sub-123"))
            .thenReturn(Optional.of(existing));

        User user = service.upsert(AuthProvider.GOOGLE, "google-sub-123",
                                   "user@example.com", "new-name", "new-img");

        assertThat(user.getName()).isEqualTo("new-name");
        assertThat(user.getProfileImageUrl()).isEqualTo("new-img");
        verify(repo, never()).save(any(User.class));
    }
}
