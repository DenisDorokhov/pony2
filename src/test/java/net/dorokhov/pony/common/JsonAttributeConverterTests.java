package net.dorokhov.pony.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

public class JsonAttributeConverterTests {

    private JsonAttributeConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new JsonAttributeConverter();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertFromJsonUsingListConverter() throws Exception {
        Object result = converter.convertToEntityAttribute("[\"foo\",\"bar\"]");
        assertThat(result).isInstanceOfSatisfying(List.class, list -> assertThat(list).containsExactly("foo", "bar"));
    }
    
    @Test
    public void convertToJsonUsingListConverter() throws Exception {
        String json = converter.convertToDatabaseColumn(ImmutableList.of("foo", "bar"));
        assertThat(json).isEqualTo("[\"foo\",\"bar\"]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertFromJsonUsingMapConverter() throws Exception {
        Object result = converter.convertToEntityAttribute("{\"k1\":\"string\",\"k2\":2.1}");
        assertThat(result).isInstanceOfSatisfying(Map.class, map -> assertThat(map).containsOnly(
                entry("k1", "string"), entry("k2", 2.1)));
    }
    
    @Test
    public void convertToJsonUsingMapConverter() throws Exception {
        String json = converter.convertToDatabaseColumn(ImmutableMap.of("k1", "string", "k2", 2.1));
        assertThat(json).isEqualTo("{\"k1\":\"string\",\"k2\":2.1}");
    }
    
    @Test
    public void failOnInvalidJson() throws Exception {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("foo, bar")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> converter.convertToEntityAttribute("foo, bar")).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void handleNulls() throws Exception {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
