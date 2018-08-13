package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.user.domain.User;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AuthenticationDto {

    private final UserDto user;

    private final String token;

    private AuthenticationDto(UserDto user, String token) {
        this.user = checkNotNull(user);
        this.token = checkNotNull(token);
    }

    public UserDto getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public static AuthenticationDto of(User user, String token) {
        return new AuthenticationDto(UserDto.of(user), token);
    }
}
