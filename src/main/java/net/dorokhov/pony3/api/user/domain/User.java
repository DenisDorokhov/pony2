package net.dorokhov.pony3.api.user.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony3.api.common.BaseEntity;
import org.hibernate.annotations.Cache;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static net.dorokhov.pony3.api.user.domain.User.CACHE_REGION;
import static org.hibernate.annotations.CacheConcurrencyStrategy.NONSTRICT_READ_WRITE;

@Entity
@Table(name = "pony_user")
@Cacheable
@Cache(usage = NONSTRICT_READ_WRITE, region = CACHE_REGION)
public class User extends BaseEntity implements Serializable {
    
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

    @Column(name = "name")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pony_user_role", joinColumns = @JoinColumn(name = "pony_user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = emptySet();

    public String getName() {
        return name;
    }

    public User setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }

    public Set<Role> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public User setRoles(Set<Role> roles) {
        this.roles = roles;
        return this;
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
}
