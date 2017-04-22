package net.dorokhov.pony.user.domain;

import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.entity.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserUpdateDraft {
    
    private final long id;
    private final String name;
    private final String email;
    private final String newPassword;
    private final Set<User.Role> roles;

    private UserUpdateDraft(Builder builder) {
        id = builder.id;
        name = checkNotNull(builder.name);
        email = checkNotNull(builder.email);
        newPassword = builder.newPassword;
        roles = builder.roles.build();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Optional<String> getNewPassword() {
        return Optional.ofNullable(newPassword);
    }

    public Set<User.Role> getRoles() {
        return roles;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        
        private long id;
        private String name;
        private String email;
        private String newPassword;
        private ImmutableSet.Builder<User.Role> roles = ImmutableSet.builder();

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }
        
        public Builder roles(Collection<User.Role> roles) {
            this.roles = ImmutableSet.<User.Role>builder().addAll(roles);
            return this;
        }

        public Builder roles(User.Role... roles) {
            return roles(Arrays.asList(roles));
        }
        
        public Builder addRole(User.Role role) {
            this.roles.add(role);
            return this;
        }

        public UserUpdateDraft build() {
            return new UserUpdateDraft(this);
        }
    }
}
