package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony2.web.validation.RepeatPassword;
import net.dorokhov.pony2.web.validation.RepeatPasswordValue;
import net.dorokhov.pony2.web.validation.UniqueEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RepeatPassword
public final class UserCreationCommandDto {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Email
    @UniqueEmail
    private String email;

    @Size(min = 6, max = 255)
    @RepeatPasswordValue
    private String password;

    @RepeatPasswordValue(constraintViolationField = true)
    private String repeatPassword;

    @NotNull
    private UserDto.Role role;

    public String getName() {
        return name;
    }

    public UserCreationCommandDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserCreationCommandDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserCreationCommandDto setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getRepeatPassword() {
        return repeatPassword;
    }

    public UserCreationCommandDto setRepeatPassword(String repeatPassword) {
        this.repeatPassword = repeatPassword;
        return this;
    }

    public UserDto.Role getRole() {
        return role;
    }

    public UserCreationCommandDto setRole(UserDto.Role role) {
        this.role = role;
        return this;
    }

    public UserCreationCommand convert() {
        return new UserCreationCommand()
                .setName(name)
                .setEmail(email)
                .setPassword(password)
                .setRoles(role.convert());
    }
}
