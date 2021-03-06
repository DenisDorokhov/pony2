package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony.web.validation.UniqueEmail;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class UserCreationCommandDto {

    @NotBlank
    @Size(max = 255)
    private final String name;

    @NotBlank
    @Size(max = 255)
    @Email
    @UniqueEmail
    private final String email;

    @NotBlank
    @Size(min = 6, max = 255)
    private final String password;

    @NotNull
    private final UserDto.Role role;

    public UserCreationCommandDto(String name, String email, String password, UserDto.Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserDto.Role getRole() {
        return role;
    }

    public UserCreationCommand convert() {
        return UserCreationCommand.builder()
                .name(name)
                .email(email)
                .password(password)
                .roles(role.convert())
                .build();
    }
}
