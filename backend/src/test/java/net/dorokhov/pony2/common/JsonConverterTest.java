package net.dorokhov.pony2.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

public class JsonConverterTest {

    @Test
    public void shouldConvertStringToJson() {

        String json = JsonConverter.toJson("Foobar");

        assertThat(json).isEqualTo("\"Foobar\"");
    }

    @Test
    public void shouldConvertListToJson() {

        String json = JsonConverter.toJson(ImmutableList.of("foo", "bar"));

        assertThat(json).isEqualTo("[\"foo\",\"bar\"]");
    }

    @Test
    public void shouldConvertMapToJson() {

        String json = JsonConverter.toJson(ImmutableMap.of("k1", "string", "k2", 2.1));

        assertThat(json).isEqualTo("{\"k1\":\"string\",\"k2\":2.1}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConvertUnknownFromJson() {

        Object result = JsonConverter.fromJson("[\"foo\",\"bar\"]");

        assertThat(result).isInstanceOfSatisfying(List.class, list -> assertThat(list).containsExactly("foo", "bar"));
    }

    @Test
    public void shouldConvertStringFromJson() {

        Object result = JsonConverter.fromJson("\"Foobar\"", String.class);

        assertThat(result).isInstanceOfSatisfying(String.class, str -> assertThat(str).isEqualTo("Foobar"));
    }

    @Test
    public void shouldConvertListFromJson() {

        List<String> result = JsonConverter.listFromJson("[\"foo\",\"bar\"]", String.class);

        assertThat(result).containsExactly("foo", "bar");
    }

    @Test
    public void shouldConvertMapFromJson() {

        Map<String, Object> result = JsonConverter.mapFromJson("{\"k1\":\"string\",\"k2\":2.1}", String.class, Object.class);

        assertThat(result).containsOnly(entry("k1", "string"), entry("k2", 2.1));
    }

    @Test
    public void shouldFailOnInvalidJson() {
        assertThatThrownBy(() -> JsonConverter.fromJson("foo, bar")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> JsonConverter.fromJson("foo, bar", String.class)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> JsonConverter.listFromJson("foo, bar", String.class)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> JsonConverter.mapFromJson("foo, bar", String.class, String.class)).isInstanceOf(RuntimeException.class);
    }
}
