package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user")
public class User extends BaseEntity<Long> {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "value")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    private Set<String> roles = new HashSet<>();

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String aName) {
        name = Preconditions.checkNotNull(aName);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String aLogin) {
        email = Preconditions.checkNotNull(aLogin);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String aPassword) {
        password = Preconditions.checkNotNull(aPassword);
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> aRoles) {
        roles = Preconditions.checkNotNull(aRoles);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("name", name)
                .add("email", email)
                .add("password", password)
                .add("roles", roles)
                .toString();
    }

    public static class Builder {

        private Long id;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private String name;
        private String email;
        private String password;
        private Set<String> roles = new HashSet<>();

        public Builder() {
        }

        public Builder(User user) {
            this.id = user.id;
            this.creationDate = user.creationDate;
            this.updateDate = user.updateDate;
            this.name = user.name;
            this.email = user.email;
            this.password = user.password;
            this.roles = user.roles;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setRoles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public User build() {
            User user = new User();
            user.setId(id);
            user.setCreationDate(creationDate);
            user.setUpdateDate(updateDate);
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setRoles(roles);
            return user;
        }
    }
}
