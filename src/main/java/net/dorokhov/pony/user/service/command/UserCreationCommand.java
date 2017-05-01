package net.dorokhov.pony.user.service.command;

import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.user.domain.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class UserCreationCommand {
    
    private final String name;
    private final String email;
    private final String password;
    private final Set<User.Role> roles;

    private UserCreationCommand(Builder builder) {
        name = checkNotNull(builder.name);
        email = checkNotNull(builder.email);
        password = checkNotNull(builder.password);
        roles = builder.roles.build();
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Set<User.Role> getRoles() {
        return roles;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private String name;
        private String email;
        private String password;
        private ImmutableSet.Builder<User.Role> roles = ImmutableSet.builder();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
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
            roles.add(role);
            return this;
        }

        public UserCreationCommand build() {
            return new UserCreationCommand(this);
        }
    }
}
