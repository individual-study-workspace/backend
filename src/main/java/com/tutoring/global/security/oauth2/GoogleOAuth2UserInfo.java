package com.tutoring.global.security.oauth2;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    @Override public String getProviderId()      { return (String) attributes.get("sub"); }
    @Override public String getEmail()           { return (String) attributes.get("email"); }
    @Override public String getName()            { return (String) attributes.get("name"); }
    @Override public String getProfileImageUrl() { return (String) attributes.get("picture"); }
}
