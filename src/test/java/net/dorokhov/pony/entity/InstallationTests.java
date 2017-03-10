package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InstallationTests {
    
    @Test
    public void shouldFailOnNotNullViolation() throws Exception {
        assertThatThrownBy(() -> new Installation(null)).isInstanceOf(NullPointerException.class);
        final Installation installation = new Installation("1.0");
        assertThatThrownBy(() -> installation.setVersion(null)).isInstanceOf(NullPointerException.class);
    }
}
