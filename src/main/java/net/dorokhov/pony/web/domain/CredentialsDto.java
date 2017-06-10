package net.dorokhov.pony.web.domain;

import org.hibernate.validator.constraints.NotBlank;

public final class CredentialsDto {

    @NotBlank
    private final String email;
    @NotBlank
    private final String password;

    public CredentialsDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
