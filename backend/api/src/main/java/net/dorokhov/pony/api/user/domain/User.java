package net.dorokhov.pony.api.user.domain;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.api.common.BaseEntity;
import org.hibernate.annotations.Cache;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;
import static net.dorokhov.pony.api.user.domain.User.CACHE_REGION;
import static org.hibernate.annotations.CacheConcurrencyStrategy.NONSTRICT_READ_WRITE;

@Entity
@Table(name = "user")
@Cacheable
@Cache(usage = NONSTRICT_READ_WRITE, region = CACHE_REGION)
public class User extends BaseEntity<Long> implements Serializable {
    
    public static final String CACHE_REGION = "pony.user";

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
    private Set<Role> roles = emptySet();

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
        return roles != null ? roles : emptySet();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("email", email)
                .add("roles", roles)
                .toString();
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

        private Builder() {
        }

        private Builder(User user) {
            id = user.getId();
            creationDate = user.getCreationDate();
            updateDate = user.getUpdateDate();
            name = user.getName();
            email = user.getEmail();
            password = user.getPassword();
            roles = ImmutableSet.<Role>builder().addAll(user.getRoles());
        }

        public Builder id(@Nullable Long id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(@Nullable LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(@Nullable LocalDateTime updateDate) {
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

        public Builder roles(@Nullable Set<Role> roles) {
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
