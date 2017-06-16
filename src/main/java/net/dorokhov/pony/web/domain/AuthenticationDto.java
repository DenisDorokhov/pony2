package net.dorokhov.pony.web.domain;

public final class AuthenticationDto {
    
    private final UserDto user;
    
    private final String token;

    public AuthenticationDto(UserDto user, String token) {
        this.user = user;
        this.token = token;
    }

    public UserDto getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }
}
