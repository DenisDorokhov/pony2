package net.dorokhov.pony2.api.user.service.command;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.user.domain.User;

import java.util.HashSet;
import java.util.Set;

public final class UnsafeUserUpdateCommand {
    
    private String id;
    private String name;
    private String email;
    private String newPassword;
    private Set<User.Role> roles;

    public String getId() {
        return id;
    }

    public UnsafeUserUpdateCommand setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public UnsafeUserUpdateCommand setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UnsafeUserUpdateCommand setEmail(String email) {
        this.email = email;
        return this;
    }

    public @Nullable String getNewPassword() {
        return newPassword;
    }

    public UnsafeUserUpdateCommand setNewPassword(@Nullable String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public Set<User.Role> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public UnsafeUserUpdateCommand setRoles(Set<User.Role> roles) {
        this.roles = roles;
        return this;
    }
}
