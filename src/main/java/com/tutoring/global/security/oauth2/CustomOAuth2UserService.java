package com.tutoring.global.security.oauth2;

import com.tutoring.domain.user.entity.AuthProvider;
import com.tutoring.domain.user.entity.User;
import com.tutoring.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.from(registrationId);
        OAuth2UserInfo info = OAuth2UserInfoFactory.from(registrationId, oauth2User.getAttributes());

        User user = upsert(provider, info.getProviderId(), info.getEmail(),
                           info.getName(), info.getProfileImageUrl());

        Map<String, Object> attributes = oauth2User.getAttributes();
        return new OAuth2UserAdapter(user, attributes);
    }

    @Transactional
    public User upsert(AuthProvider provider, String providerId,
                       String email, String name, String profileImageUrl) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
            .map(u -> { u.updateProfile(name, profileImageUrl); return u; })
            .orElseGet(() -> userRepository.save(
                User.create(provider, providerId, email, name, profileImageUrl)));
    }
}
