package net.dorokhov.pony.entity;

import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigTests {

    @Test
    public void shouldSupportNullValue() throws Exception {
        Config config = new Config("foo", null);
        assertThat(config.getValue()).isEqualTo(Optional.empty());
        assertThat(config.getInteger()).isEqualTo(Optional.empty());
        assertThat(config.getLong()).isEqualTo(Optional.empty());
        assertThat(config.getDouble()).isEqualTo(Optional.empty());
        assertThat(config.getBoolean()).isEqualTo(Optional.empty());
    }

    @Test
    public void shouldSupportStringValue() throws Exception {
        Config config = new Config("foo", "bar");
        assertThat(config.getValue()).isEqualTo(Optional.of("bar"));
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getDouble).isInstanceOf(NumberFormatException.class);
        assertThat(config.getBoolean()).isEqualTo(Optional.of(false));
    }

    @Test
    public void shouldSupportIntegerValue() throws Exception {
        Config config = new Config("foo", 1);
        assertThat(config.getValue()).isEqualTo(Optional.of("1"));
        assertThat(config.getInteger()).isEqualTo(Optional.of(1));
        assertThat(config.getLong()).isEqualTo(Optional.of(1L));
        assertThat(config.getDouble()).isEqualTo(Optional.of(1.0));
        assertThat(config.getBoolean()).isEqualTo(Optional.of(false));
    }

    @Test
    public void shouldSupportLongValue() throws Exception {
        Config config = new Config("foo", 1L);
        assertThat(config.getValue()).isEqualTo(Optional.of("1"));
        assertThat(config.getInteger()).isEqualTo(Optional.of(1));
        assertThat(config.getLong()).isEqualTo(Optional.of(1L));
        assertThat(config.getDouble()).isEqualTo(Optional.of(1.0));
        assertThat(config.getBoolean()).isEqualTo(Optional.of(false));
    }

    @Test
    public void shouldSupportDoubleValue() throws Exception {
        Config config = new Config("foo", 1.2);
        assertThat(config.getValue()).isEqualTo(Optional.of("1.2"));
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThat(config.getDouble()).isEqualTo(Optional.of(1.2));
        assertThat(config.getBoolean()).isEqualTo(Optional.of(false));
    }

    @Test
    public void shouldSupportBooleanValue() throws Exception {
        Config config = new Config("foo", true);
        assertThat(config.getValue()).isEqualTo(Optional.of("true"));
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getDouble).isInstanceOf(NumberFormatException.class);
        assertThat(config.getBoolean()).isEqualTo(Optional.of(true));
    }

    @Test
    public void shouldFailOnNotNullViolation() throws Exception {
        
        assertThatThrownBy(() -> new Config(null, "foo")).isInstanceOf(NullPointerException.class);
        
        final Config config = new Config("foo", true);
        assertThatThrownBy(() -> config.setId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        Config eqConfig1 = new Config("1", null);
        Config eqConfig2 = new Config("1", null);
        Config diffConfig = new Config("2", null);

        assertThat(eqConfig1.hashCode()).isEqualTo(eqConfig2.hashCode());
        assertThat(eqConfig1.hashCode()).isNotEqualTo(diffConfig.hashCode());

        assertThat(eqConfig1).isEqualTo(eqConfig1);
        assertThat(eqConfig1).isEqualTo(eqConfig2);

        assertThat(eqConfig1).isNotEqualTo(diffConfig);
        assertThat(eqConfig1).isNotEqualTo("foo1");
        assertThat(eqConfig1).isNotEqualTo(null);
    }
}
