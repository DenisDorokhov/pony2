package net.dorokhov.pony3.api.user.service.command;

import net.dorokhov.pony3.api.user.domain.User;

import java.util.HashSet;
import java.util.Set;

public final class UserCreationCommand {
    
    private String name;
    private String email;
    private String password;
    private Set<User.Role> roles;

    public String getName() {
        return name;
    }

    public UserCreationCommand setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserCreationCommand setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserCreationCommand setPassword(String password) {
        this.password = password;
        return this;
    }

    public Set<User.Role> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public UserCreationCommand setRoles(Set<User.Role> roles) {
        this.roles = roles;
        return this;
    }
}
