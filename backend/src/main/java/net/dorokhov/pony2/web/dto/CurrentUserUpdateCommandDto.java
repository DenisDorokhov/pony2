package net.dorokhov.pony2.web.dto;

import jakarta.validation.constraints.Size;
import net.dorokhov.pony2.api.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony2.web.validation.CurrentUserPasswordMatch;
import net.dorokhov.pony2.web.validation.RepeatPassword;
import net.dorokhov.pony2.web.validation.RepeatPasswordValue;
import net.dorokhov.pony2.web.validation.UniqueCurrentUserEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.annotation.Nullable;

@RepeatPassword
public final class CurrentUserUpdateCommandDto {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Email
    @UniqueCurrentUserEmail
    private String email;

    @CurrentUserPasswordMatch
    private String oldPassword;

    @Size(min = 6, max = 255)
    @RepeatPasswordValue
    private String newPassword;

    @RepeatPasswordValue(constraintViolationField = true)
    private String repeatNewPassword;

    public String getName() {
        return name;
    }

    public CurrentUserUpdateCommandDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public CurrentUserUpdateCommandDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public CurrentUserUpdateCommandDto setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
        return this;
    }

    @Nullable
    public String getNewPassword() {
        return newPassword;
    }

    public CurrentUserUpdateCommandDto setNewPassword(@Nullable String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public String getRepeatNewPassword() {
        return repeatNewPassword;
    }

    public CurrentUserUpdateCommandDto setRepeatNewPassword(String repeatNewPassword) {
        this.repeatNewPassword = repeatNewPassword;
        return this;
    }

    public SafeUserUpdateCommand convert(String userId) {
        return new SafeUserUpdateCommand()
                .setId(userId)
                .setName(name)
                .setEmail(email)
                .setOldPassword(oldPassword)
                .setNewPassword(newPassword);
    }
}
