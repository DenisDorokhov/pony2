package net.dorokhov.pony.entity;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserTests {

    @Test
    public void shouldFailOnNotNullViolation() throws Exception {
        
        User user = getUserBuilder().setId(1L).build();

        assertThatThrownBy(() -> new User.Builder(user).setName(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User.Builder(user).setEmail(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User.Builder(user).setPassword(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User.Builder(user).setRoles(null).build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void songShouldBeBuilt() throws Exception {
        
        User user = new User.Builder()
                .setId(1L)
                .setName("name")
                .setEmail("email")
                .setPassword("password")
                .setRoles(ImmutableSet.of("r1", "r2"))
                .build();

        assertThat(user.getId()).isEqualTo(Optional.of(1L));
        assertThat(user.getName()).isEqualTo("name");
        assertThat(user.getEmail()).isEqualTo("email");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getRoles()).containsExactly("r1", "r2");
    }

    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        User eqUser1 = getUserBuilder().setId(1L).build();
        User eqUser2 = getUserBuilder().setId(1L).build();
        User diffUser = getUserBuilder().setId(2L).build();

        assertThat(eqUser1.hashCode()).isEqualTo(eqUser2.hashCode());
        assertThat(eqUser1.hashCode()).isNotEqualTo(diffUser.hashCode());

        assertThat(eqUser1).isEqualTo(eqUser1);
        assertThat(eqUser1).isEqualTo(eqUser2);

        assertThat(eqUser1).isNotEqualTo(diffUser);
        assertThat(eqUser1).isNotEqualTo("foo1");
        assertThat(eqUser1).isNotEqualTo(null);
    }

    private User.Builder getUserBuilder() {
        return new User.Builder()
                .setName("name")
                .setEmail("email")
                .setPassword("password");
    }
}
