package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTests {
    
    @Test
    public void supportEqualityAndHashCode() throws Exception {

        User eqUser1 = buildUser().id(1L).build();
        User eqUser2 = buildUser().id(1L).build();
        User diffUser = buildUser().id(2L).build();

        assertThat(eqUser1.hashCode()).isEqualTo(eqUser2.hashCode());
        assertThat(eqUser1.hashCode()).isNotEqualTo(diffUser.hashCode());

        assertThat(eqUser1).isEqualTo(eqUser1);
        assertThat(eqUser1).isEqualTo(eqUser2);

        assertThat(eqUser1).isNotEqualTo(diffUser);
        assertThat(eqUser1).isNotEqualTo("foo1");
        assertThat(eqUser1).isNotEqualTo(null);
    }

    @Test
    public void stringify() throws Exception {
        assertThat(new User().toString()).startsWith("User{");
    }

    private User.Builder buildUser() {
        return User.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword");
    }
}
