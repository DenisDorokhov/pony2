package net.dorokhov.pony.web.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.dorokhov.pony.user.domain.User;

import java.time.LocalDateTime;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class UserDto {
    
    public enum Role {
        USER, ADMIN
    }
    
    private final Long id;
    
    private final LocalDateTime creationDate;
    private final LocalDateTime updateDate;
    
    private final String name;
    private final String email;
    
    private final Role role;

    @JsonCreator
    public UserDto(Long id,
                   LocalDateTime creationDate, LocalDateTime updateDate, 
                   String name, String email, 
                   Role role) {
        this.id = checkNotNull(id);
        this.creationDate = checkNotNull(creationDate);
        this.updateDate = updateDate;
        this.name = checkNotNull(name);
        this.email = checkNotNull(email);
        this.role = checkNotNull(role);
    }
    
    public UserDto(User user) {
        this(user.getId(), user.getCreationDate(), user.getUpdateDate(), 
                user.getName(), user.getEmail(), rolesToDto(user.getRoles()));
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
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
    
    private static Role rolesToDto(Set<User.Role> roles) {
        if (roles.contains(User.Role.ADMIN)) {
            return Role.ADMIN;
        } else {
            return Role.USER;
        }
    }
}
