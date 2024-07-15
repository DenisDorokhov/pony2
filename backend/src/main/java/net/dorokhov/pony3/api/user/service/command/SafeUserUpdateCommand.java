package net.dorokhov.pony3.api.user.service.command;

import jakarta.annotation.Nullable;

public final class SafeUserUpdateCommand {

    private String id;
    private String name;
    private String email;
    private String oldPassword;
    private String newPassword;

    public String getId() {
        return id;
    }

    public SafeUserUpdateCommand setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public SafeUserUpdateCommand setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public SafeUserUpdateCommand setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public SafeUserUpdateCommand setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
        return this;
    }

    public @Nullable String getNewPassword() {
        return newPassword;
    }

    public SafeUserUpdateCommand setNewPassword(@Nullable String newPassword) {
        this.newPassword = newPassword;
        return this;
    }
}
