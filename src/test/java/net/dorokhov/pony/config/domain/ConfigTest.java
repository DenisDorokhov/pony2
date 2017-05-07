package net.dorokhov.pony.config.domain;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigTest {

    @Test
    public void shouldSupportNullValue() throws Exception {
        Config config = new Config("foo", null);
        assertThat(config.getValue()).isNull();
        assertThat(config.getInteger()).isNull();
        assertThat(config.getLong()).isNull();
        assertThat(config.getDouble()).isNull();
        assertThat(config.getBoolean()).isNull();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldSupportStringValue() throws Exception {
        Config config = new Config("foo", "bar");
        assertThat(config.getValue()).isEqualTo("bar");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getDouble).isInstanceOf(NumberFormatException.class);
        assertThat(config.getBoolean()).isFalse();
    }

    @Test
    public void shouldSupportIntegerValue() throws Exception {
        Config config = new Config("foo", 1);
        assertThat(config.getValue()).isEqualTo("1");
        assertThat(config.getInteger()).isEqualTo(1);
        assertThat(config.getLong()).isEqualTo(1L);
        assertThat(config.getDouble()).isEqualTo(1.0);
        assertThat(config.getBoolean()).isFalse();
    }

    @Test
    public void shouldSupportLongValue() throws Exception {
        Config config = new Config("foo", 1L);
        assertThat(config.getValue()).isEqualTo("1");
        assertThat(config.getInteger()).isEqualTo(1);
        assertThat(config.getLong()).isEqualTo(1L);
        assertThat(config.getDouble()).isEqualTo(1.0);
        assertThat(config.getBoolean()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldSupportDoubleValue() throws Exception {
        Config config = new Config("foo", 1.2);
        assertThat(config.getValue()).isEqualTo("1.2");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThat(config.getDouble()).isEqualTo(1.2);
        assertThat(config.getBoolean()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldSupportBooleanValue() throws Exception {
        Config config = new Config("foo", true);
        assertThat(config.getValue()).isEqualTo("true");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getDouble).isInstanceOf(NumberFormatException.class);
        assertThat(config.getBoolean()).isTrue();
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

    @Test
    public void shouldImplementToString() throws Exception {
        assertThat(Config.builder()
                .id("foo")
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .value(false)
                .build().toString()).startsWith("Config{");
    }
}
