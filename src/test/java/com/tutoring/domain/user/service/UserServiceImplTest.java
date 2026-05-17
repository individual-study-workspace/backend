package com.tutoring.domain.user.service;

import com.tutoring.domain.user.dto.UserResponse;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import com.tutoring.global.error.ApiException;
import com.tutoring.global.error.ErrorCode;
import com.tutoring.support.UserFixture;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final UserService service = new UserServiceImpl(repo);

    @Test
    void getMe_returns_response_for_existing_user() {
        User user = UserFixture.googleUser();
        when(repo.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = service.getMe(1L);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
    }

    @Test
    void getMe_throws_USER_NOT_FOUND_when_missing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMe(99L))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}
