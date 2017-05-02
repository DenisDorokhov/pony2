package net.dorokhov.pony.user.service.command;

import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.user.domain.User;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserUpdateCommand {
    
    private final long id;
    private final String name;
    private final String email;
    private final String newPassword;
    private final Set<User.Role> roles;

    private UserUpdateCommand(Builder builder) {
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

    @Nullable
    public String getNewPassword() {
        return newPassword;
    }

    public Set<User.Role> getRoles() {
        return roles;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
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

        public Builder newPassword(@Nullable String newPassword) {
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

        public UserUpdateCommand build() {
            return new UserUpdateCommand(this);
        }
    }
}
