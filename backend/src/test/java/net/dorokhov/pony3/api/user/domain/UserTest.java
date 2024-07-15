package net.dorokhov.pony3.api.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {
    
    @Test
    public void shouldSupportEqualityAndHashCode() {

        User eqUser1 = user().setId("1");
        User eqUser2 = user().setId("1");
        User diffUser = user().setId("2");

        assertThat(eqUser1.hashCode()).isEqualTo(eqUser2.hashCode());
        assertThat(eqUser1.hashCode()).isNotEqualTo(diffUser.hashCode());

        assertThat(eqUser1).isEqualTo(eqUser1);
        assertThat(eqUser1).isEqualTo(eqUser2);

        assertThat(eqUser1).isNotEqualTo(diffUser);
        assertThat(eqUser1).isNotEqualTo("foo1");
        assertThat(eqUser1).isNotEqualTo(null);
    }

    private User user() {
        return new User()
                .setName("someName")
                .setEmail("someEmail")
                .setPassword("somePassword");
    }
}
