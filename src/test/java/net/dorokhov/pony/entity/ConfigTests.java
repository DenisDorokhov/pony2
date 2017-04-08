package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigTests {

    @Test
    public void supportNullValue() throws Exception {
        Config config = new Config("foo");
        assertThat(config.getValue()).isEmpty();
        assertThat(config.getInteger()).isEmpty();
        assertThat(config.getLong()).isEmpty();
        assertThat(config.getDouble()).isEmpty();
        assertThat(config.getBoolean()).isEmpty();
    }

    @Test
    public void supportStringValue() throws Exception {
        Config config = new Config("foo", "bar");
        assertThat(config.getValue()).hasValue("bar");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getDouble).isInstanceOf(NumberFormatException.class);
        assertThat(config.getBoolean()).hasValue(false);
    }

    @Test
    public void supportIntegerValue() throws Exception {
        Config config = new Config("foo", 1);
        assertThat(config.getValue()).hasValue("1");
        assertThat(config.getInteger()).hasValue(1);
        assertThat(config.getLong()).hasValue(1L);
        assertThat(config.getDouble()).hasValue(1.0);
        assertThat(config.getBoolean()).hasValue(false);
    }

    @Test
    public void supportLongValue() throws Exception {
        Config config = new Config("foo", 1L);
        assertThat(config.getValue()).hasValue("1");
        assertThat(config.getInteger()).hasValue(1);
        assertThat(config.getLong()).hasValue(1L);
        assertThat(config.getDouble()).hasValue(1.0);
        assertThat(config.getBoolean()).hasValue(false);
    }

    @Test
    public void supportDoubleValue() throws Exception {
        Config config = new Config("foo", 1.2);
        assertThat(config.getValue()).hasValue("1.2");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThat(config.getDouble()).hasValue(1.2);
        assertThat(config.getBoolean()).hasValue(false);
    }

    @Test
    public void supportBooleanValue() throws Exception {
        Config config = new Config("foo", true);
        assertThat(config.getValue()).hasValue("true");
        assertThatThrownBy(config::getInteger).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getLong).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(config::getDouble).isInstanceOf(NumberFormatException.class);
        assertThat(config.getBoolean()).hasValue(true);
    }

    @Test
    public void supportEqualityAndHashCode() throws Exception {

        Config eqConfig1 = new Config("1");
        Config eqConfig2 = new Config("1");
        Config diffConfig = new Config("2");

        assertThat(eqConfig1.hashCode()).isEqualTo(eqConfig2.hashCode());
        assertThat(eqConfig1.hashCode()).isNotEqualTo(diffConfig.hashCode());

        assertThat(eqConfig1).isEqualTo(eqConfig1);
        assertThat(eqConfig1).isEqualTo(eqConfig2);

        assertThat(eqConfig1).isNotEqualTo(diffConfig);
        assertThat(eqConfig1).isNotEqualTo("foo1");
        assertThat(eqConfig1).isNotEqualTo(null);
    }

    @Test
    public void stringify() throws Exception {
        assertThat(new Config().toString()).startsWith("Config{");
    }
}
