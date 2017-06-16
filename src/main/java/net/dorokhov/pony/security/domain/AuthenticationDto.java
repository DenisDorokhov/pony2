package net.dorokhov.pony.security.domain;

public final class AuthenticationDto {
    
    private final String token;

    public AuthenticationDto(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
