package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.user.domain.User;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AuthenticationDto {

    private final UserDto user;
    private final String accessToken;
    private final String staticToken;

    private AuthenticationDto(UserDto user, String accessToken, String staticToken) {
        this.user = checkNotNull(user);
        this.accessToken = checkNotNull(accessToken);
        this.staticToken = checkNotNull(staticToken);
    }

    public UserDto getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getStaticToken() {
        return staticToken;
    }

    public static AuthenticationDto of(User user, String accessToken, String staticToken) {
        return new AuthenticationDto(UserDto.of(user), accessToken, staticToken);
    }
}
