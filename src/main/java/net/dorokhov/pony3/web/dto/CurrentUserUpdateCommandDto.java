package net.dorokhov.pony3.web.dto;

import jakarta.validation.constraints.Size;
import net.dorokhov.pony3.api.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony3.web.validation.CurrentUserPasswordMatch;
import net.dorokhov.pony3.web.validation.UniqueCurrentUserEmail;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import jakarta.annotation.Nullable;

public final class CurrentUserUpdateCommandDto {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Email
    @UniqueCurrentUserEmail
    private String email;

    @NotBlank
    @CurrentUserPasswordMatch
    private String oldPassword;

    @Size(min = 6, max = 255)
    private String newPassword;

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

    public SafeUserUpdateCommand convert(String userId) {
        return new SafeUserUpdateCommand()
                .setId(userId)
                .setName(name)
                .setEmail(email)
                .setOldPassword(oldPassword)
                .setNewPassword(newPassword);
    }
}
