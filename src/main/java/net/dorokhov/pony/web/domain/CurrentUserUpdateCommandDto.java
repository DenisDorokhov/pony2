package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.user.service.command.SafeUserUpdateCommand;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;

public final class CurrentUserUpdateCommandDto {

    @NotBlank
    @Size(max = 255)
    private final String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private final String email;
    
    @NotBlank
    private final String oldPassword;

    @NotBlank
    @Size(min = 6, max = 255)
    private final String newPassword;

    public CurrentUserUpdateCommandDto(String name, String email, String oldPassword, String newPassword) {
        this.name = name;
        this.email = email;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public SafeUserUpdateCommand convert(Long userId) {
        return SafeUserUpdateCommand.builder()
                .id(userId)
                .name(name)
                .email(email)
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .build();
    }
}
