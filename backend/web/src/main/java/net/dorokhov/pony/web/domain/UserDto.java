package net.dorokhov.pony.web.domain;

import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.api.user.domain.User;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class UserDto extends BaseDto {

    public enum Role {

        USER, ADMIN;

        public Set<User.Role> convert() {
            if (this == ADMIN) {
                return ImmutableSet.of(User.Role.USER, User.Role.ADMIN);
            } else {
                return ImmutableSet.of(User.Role.USER);
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

    private final String name;
    private final String email;

    private final Role role;

    UserDto(Long id, LocalDateTime creationDate, @Nullable LocalDateTime updateDate,
            String name, String email, Role role) {
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

    public static UserDto of(User user) {
        return new UserDto(user.getId(), user.getCreationDate(), user.getUpdateDate(),
                user.getName(), user.getEmail(), Role.of(user.getRoles()));
    }
}
