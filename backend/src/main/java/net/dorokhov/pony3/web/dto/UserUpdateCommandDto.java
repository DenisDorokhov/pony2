package net.dorokhov.pony3.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.dorokhov.pony3.api.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony3.web.validation.RepeatPassword;
import net.dorokhov.pony3.web.validation.RepeatPasswordValue;
import net.dorokhov.pony3.web.validation.UpdateUniqueEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.annotation.Nullable;

@UpdateUniqueEmail
@RepeatPassword
public final class UserUpdateCommandDto {

    @NotNull
    private String id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Email
    private String email;

    @Size(min = 6, max = 255)
    @RepeatPasswordValue
    private String newPassword;

    @RepeatPasswordValue(constraintViolationField = true)
    private String repeatNewPassword;

    @NotNull
    private UserDto.Role role;

    public String getId() {
        return id;
    }

    public UserUpdateCommandDto setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public UserUpdateCommandDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserUpdateCommandDto setEmail(String email) {
        this.email = email;
        return this;
    }

    @Nullable
    public String getNewPassword() {
        return newPassword;
    }

    public UserUpdateCommandDto setNewPassword(@Nullable String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public UserDto.Role getRole() {
        return role;
    }

    public UserUpdateCommandDto setRole(UserDto.Role role) {
        this.role = role;
        return this;
    }

    public String getRepeatNewPassword() {
        return repeatNewPassword;
    }

    public UserUpdateCommandDto setRepeatNewPassword(String repeatNewPassword) {
        this.repeatNewPassword = repeatNewPassword;
        return this;
    }

    public UnsafeUserUpdateCommand convert() {
        return new UnsafeUserUpdateCommand()
                .setId(id)
                .setName(name)
                .setEmail(email)
                .setNewPassword(newPassword)
                .setRoles(role.convert());
    }
}
