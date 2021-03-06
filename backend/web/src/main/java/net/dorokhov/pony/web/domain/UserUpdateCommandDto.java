package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony.web.validation.UpdateUniqueEmail;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@UpdateUniqueEmail
public final class UserUpdateCommandDto {

    @NotNull
    private final String id;

    @NotBlank
    @Size(max = 255)
    private final String name;

    @NotBlank
    @Size(max = 255)
    @Email
    private final String email;

    @Size(min = 6, max = 255)
    private final String newPassword;

    @NotNull
    private final UserDto.Role role;

    public UserUpdateCommandDto(String id, String name, String email, @Nullable String newPassword, UserDto.Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.newPassword = newPassword;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Nullable
    public String getNewPassword() {
        return newPassword;
    }

    public UserDto.Role getRole() {
        return role;
    }

    public UnsafeUserUpdateCommand convert() {
        return UnsafeUserUpdateCommand.builder()
                .id(id)
                .name(name)
                .email(email)
                .newPassword(newPassword)
                .roles(role.convert())
                .build();
    }
}
