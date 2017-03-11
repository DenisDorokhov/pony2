package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTests {
    
    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        User eqUser1 = new User();
        eqUser1.setId(1L);
        User eqUser2 = new User();
        eqUser2.setId(1L);
        User diffUser = new User();
        diffUser.setId(2L);

        assertThat(eqUser1.hashCode()).isEqualTo(eqUser2.hashCode());
        assertThat(eqUser1.hashCode()).isNotEqualTo(diffUser.hashCode());

        assertThat(eqUser1).isEqualTo(eqUser1);
        assertThat(eqUser1).isEqualTo(eqUser2);

        assertThat(eqUser1).isNotEqualTo(diffUser);
        assertThat(eqUser1).isNotEqualTo("foo1");
        assertThat(eqUser1).isNotEqualTo(null);
    }
}
