package net.dorokhov.pony.user.domain;

import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.common.BaseEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "user")
public class User extends BaseEntity<Long> implements Serializable {

    public enum Role {
        USER, ADMIN
    }

    @Column(name = "name", nullable = false)
    @NotNull
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    @NotNull
    private String email;

    @Column(name = "password", nullable = false)
    @NotNull
    private String password;

    @Column(name = "value")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = ImmutableSet.of();

    protected User() {
    }

    private User(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
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

    public Set<Role> getRoles() {
        return roles != null ? roles : ImmutableSet.of();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(User user) {
        return new Builder(user);
    }

    public static final class Builder {
        
        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String name;
        private String email;
        private String password;
        private ImmutableSet.Builder<Role> roles = ImmutableSet.builder();

        public Builder() {
        }

        public Builder(User user) {
            id = user.id;
            creationDate = user.creationDate;
            updateDate = user.updateDate;
            name = user.name;
            email = user.email;
            password = user.password;
            roles = ImmutableSet.<Role>builder().addAll(user.roles);
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(LocalDateTime updateDate) {
            this.updateDate = updateDate;
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

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder roles(Set<Role> roles) {
            if (roles != null) {
                this.roles = ImmutableSet.<Role>builder().addAll(roles);
            } else {
                this.roles = ImmutableSet.builder();
            }
            return this;
        }

        public Builder addRoles(Role... roles) {
            this.roles.addAll(Arrays.asList(roles));
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
