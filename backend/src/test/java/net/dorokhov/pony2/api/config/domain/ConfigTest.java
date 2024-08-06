package net.dorokhov.pony2.api.config.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigTest {

    @Test
    public void shouldSupportNullValue() {

        Config config = Config.of("foo", null);

        assertThat(config.getValue()).isNull();
        assertThat(config.getInteger()).isNull();
        assertThat(config.getLong()).isNull();
        assertThat(config.getDouble()).isNull();
        assertThat(config.getBoolean()).isNull();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldSupportStringValue() {

        Config config = Config.of("foo", "bar");

        assertThat(config.getValue()).isEqualTo("bar");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getDouble).isInstanceOf(NumberFormatException.class);
        assertThat(config.getBoolean()).isFalse();
    }

    @Test
    public void shouldSupportIntegerValue() {

        Config config = Config.of("foo", 1);

        assertThat(config.getValue()).isEqualTo("1");
        assertThat(config.getInteger()).isEqualTo(1);
        assertThat(config.getLong()).isEqualTo(1L);
        assertThat(config.getDouble()).isEqualTo(1.0);
        assertThat(config.getBoolean()).isFalse();
    }

    @Test
    public void shouldSupportLongValue() {

        Config config = Config.of("foo", 1L);

        assertThat(config.getValue()).isEqualTo("1");
        assertThat(config.getInteger()).isEqualTo(1);
        assertThat(config.getLong()).isEqualTo(1L);
        assertThat(config.getDouble()).isEqualTo(1.0);
        assertThat(config.getBoolean()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldSupportDoubleValue() {

        Config config = Config.of("foo", 1.2);

        assertThat(config.getValue()).isEqualTo("1.2");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThat(config.getDouble()).isEqualTo(1.2);
        assertThat(config.getBoolean()).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldSupportBooleanValue() {

        Config config = Config.of("foo", true);

        assertThat(config.getValue()).isEqualTo("true");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getDouble).isInstanceOf(NumberFormatException.class);
        assertThat(config.getBoolean()).isTrue();
    }

    @Test
    public void shouldSupportEqualityAndHashCode() {

        Config eqConfig1 = Config.of("1", null);
        Config eqConfig2 = Config.of("1", null);
        Config diffConfig = Config.of("2", null);

        assertThat(eqConfig1.hashCode()).isEqualTo(eqConfig2.hashCode());
        assertThat(eqConfig1.hashCode()).isNotEqualTo(diffConfig.hashCode());

        //noinspection EqualsWithItself
        assertThat(eqConfig1).isEqualTo(eqConfig1);
        assertThat(eqConfig1).isEqualTo(eqConfig2);

        assertThat(eqConfig1).isNotEqualTo(diffConfig);
        //noinspection AssertBetweenInconvertibleTypes
        assertThat(eqConfig1).isNotEqualTo("foo1");
        assertThat(eqConfig1).isNotEqualTo(null);
    }
}
