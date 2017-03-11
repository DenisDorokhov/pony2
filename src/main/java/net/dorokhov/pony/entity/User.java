package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user")
public class User extends BaseEntity<Long> {

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
    private Set<String> roles;

    public String getName() {
        return name;
    }

    public void setName(String aName) {
        name = aName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String aLogin) {
        email = aLogin;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String aPassword) {
        password = aPassword;
    }

    public Set<String> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public void setRoles(Set<String> aRoles) {
        roles = aRoles;
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
}
