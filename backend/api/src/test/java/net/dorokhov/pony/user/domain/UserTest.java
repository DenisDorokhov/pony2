package net.dorokhov.pony.user.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {
    
    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        User eqUser1 = userBuilder().id(1L).build();
        User eqUser2 = userBuilder().id(1L).build();
        User diffUser = userBuilder().id(2L).build();

        assertThat(eqUser1.hashCode()).isEqualTo(eqUser2.hashCode());
        assertThat(eqUser1.hashCode()).isNotEqualTo(diffUser.hashCode());

        assertThat(eqUser1).isEqualTo(eqUser1);
        assertThat(eqUser1).isEqualTo(eqUser2);

        assertThat(eqUser1).isNotEqualTo(diffUser);
        assertThat(eqUser1).isNotEqualTo("foo1");
        assertThat(eqUser1).isNotEqualTo(null);
    }

    private User.Builder userBuilder() {
        return User.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword");
    }
}
