package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.api.user.domain.User;

public final class AuthenticationDto {

    private UserDto user;
    private String accessToken;
    private String staticToken;

    public UserDto getUser() {
        return user;
    }

    public AuthenticationDto setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public AuthenticationDto setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getStaticToken() {
        return staticToken;
    }

    public AuthenticationDto setStaticToken(String staticToken) {
        this.staticToken = staticToken;
        return this;
    }

    public static AuthenticationDto of(User user, String accessToken, String staticToken) {
        return new AuthenticationDto()
                .setUser(UserDto.of(user))
                .setAccessToken(accessToken)
                .setStaticToken(staticToken);
    }
}
