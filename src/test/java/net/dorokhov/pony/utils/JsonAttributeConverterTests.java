package net.dorokhov.pony.utils;

import com.google.common.collect.ImmutableMap;
import net.dorokhov.pony.util.JsonAttributeConverter;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

public class JsonAttributeConverterTests {

    private JsonAttributeConverter.ListConverter listConverter;
    private JsonAttributeConverter.MapConverter mapConverter;

    @Before
    public void setUp() throws Exception {
        listConverter = new JsonAttributeConverter.ListConverter();
        mapConverter = new JsonAttributeConverter.MapConverter();
    }

    @Test
    public void convertFromJsonUsingListConverter() throws Exception {
        List<Object> list = listConverter.convertToEntityAttribute("[\"foo\",\"bar\"]");
        assertThat(list).containsExactly("foo", "bar");
    }
    
    @Test
    public void convertToJsonUsingListConverter() throws Exception {
        List<Object> list = Arrays.asList("foo", "bar");
        String json = listConverter.convertToDatabaseColumn(list);
        assertThat(json).isEqualTo("[\"foo\",\"bar\"]");
    }

    @Test
    public void convertFromJsonUsingMapConverter() throws Exception {
        Map<String, Object> map = mapConverter.convertToEntityAttribute("{\"k1\":\"string\",\"k2\":2.1}");
        assertThat(map).containsOnly(entry("k1", "string"), entry("k2", 2.1));
    }
    
    @Test
    public void convertToJsonUsingMapConverter() throws Exception {
        Map<String, Object> map = ImmutableMap.of("k1", "string", "k2", 2.1);
        String json = mapConverter.convertToDatabaseColumn(map);
        assertThat(json).isEqualTo("{\"k1\":\"string\",\"k2\":2.1}");
    }
    
    @Test
    public void failOnInvalidJson() throws Exception {
        assertThatThrownBy(() -> listConverter.convertToEntityAttribute("foo, bar")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> mapConverter.convertToEntityAttribute("foo, bar")).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void handleNulls() throws Exception {
        assertThat(listConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(mapConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(listConverter.convertToEntityAttribute(null)).isNull();
        assertThat(mapConverter.convertToEntityAttribute(null)).isNull();
    }
}
