package net.dorokhov.pony.user.domain;

import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.entity.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserCreationDraft {
    
    private final String name;
    private final String email;
    private final String password;
    private final Set<User.Role> roles;

    private UserCreationDraft(Builder builder) {
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

    public static class Builder {
        
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

        public UserCreationDraft build() {
            return new UserCreationDraft(this);
        }
    }
}
