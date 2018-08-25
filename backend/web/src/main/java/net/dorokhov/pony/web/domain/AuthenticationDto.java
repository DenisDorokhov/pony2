package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.user.domain.User;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AuthenticationDto {

    private final UserDto user;
    private final String accessToken;

    private AuthenticationDto(UserDto user, String accessToken) {
        this.user = checkNotNull(user);
        this.accessToken = checkNotNull(accessToken);
    }

    public UserDto getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public static AuthenticationDto of(User user, String accessToken) {
        return new AuthenticationDto(UserDto.of(user), accessToken);
    }
}
