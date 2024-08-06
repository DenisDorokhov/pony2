package net.dorokhov.pony2.web.dto;

import com.google.common.collect.Sets;
import net.dorokhov.pony2.api.user.domain.User;

import java.util.Set;

public final class UserDto extends BaseDto<UserDto> {

    public enum Role {

        USER, ADMIN;

        public Set<User.Role> convert() {
            if (this == ADMIN) {
                return Sets.newHashSet(User.Role.USER, User.Role.ADMIN);
            } else {
                return Sets.newHashSet(User.Role.USER);
            }
        }

        public static Role of(Set<User.Role> roles) {
            if (roles.contains(User.Role.ADMIN)) {
                return Role.ADMIN;
            } else {
                return Role.USER;
            }
        }
    }

    private String name;
    private String email;

    private Role role;

    public String getName() {
        return name;
    }

    public UserDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public UserDto setRole(Role role) {
        this.role = role;
        return this;
    }

    public static UserDto of(User user) {
        return new UserDto()
                .setId(user.getId())
                .setCreationDate(user.getCreationDate())
                .setUpdateDate(user.getUpdateDate())
                .setName(user.getName())
                .setEmail(user.getEmail())
                .setRole(Role.of(user.getRoles()));
    }
}
