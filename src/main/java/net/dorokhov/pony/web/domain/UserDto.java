package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.domain.User.Role;

import java.time.LocalDateTime;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class UserDto extends BaseDto {

    private final String name;
    private final String email;

    private final Role role;

    public UserDto(Long id,
                   LocalDateTime creationDate, LocalDateTime updateDate,
                   String name, String email,
                   Role role) {
        super(id, creationDate, updateDate);
        this.name = checkNotNull(name);
        this.email = checkNotNull(email);
        this.role = checkNotNull(role);
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    private static Role rolesToDto(Set<Role> roles) {
        if (roles.contains(Role.ADMIN)) {
            return Role.ADMIN;
        } else {
            return Role.USER;
        }
    }

    public static UserDto of(User user) {
        return new UserDto(user.getId(), user.getCreationDate(), user.getUpdateDate(),
                user.getName(), user.getEmail(), rolesToDto(user.getRoles()));
    }
}
