package com.learning.outh2_demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final JwtDecoder jwtDecoder;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;
    CustomOidcUserService(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser defaultUser =  super.loadUser(userRequest);

        String accessToken = userRequest.getAccessToken().getTokenValue();

        Set<GrantedAuthority> keycloakRoles = extractKeycloakRoles(accessToken);

        keycloakRoles.addAll(defaultUser.getAuthorities());

        return new DefaultOidcUser(
                keycloakRoles,
                defaultUser.getIdToken(),
                defaultUser.getUserInfo()
        );
    }

    private Set<GrantedAuthority> extractKeycloakRoles(String accessToken) {

        Jwt jwt = jwtDecoder.decode(accessToken);

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess == null) {
            return Collections.emptySet();
        }

        Object clientObj = resourceAccess.get(clientId);

        if (!(clientObj instanceof Map<?, ?> clientResource)) {
            return Collections.emptySet();
        }

        Object rolesObj = clientResource.get("roles");

        if (!(rolesObj instanceof List<?> rolesList)) {
            return Collections.emptySet();
        }

        return rolesList.stream()
                .map(Object::toString)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
    }

}
