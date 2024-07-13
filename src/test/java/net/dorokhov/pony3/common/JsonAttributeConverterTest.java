package net.dorokhov.pony3.common;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonAttributeConverterTest {

    private final JsonAttributeConverter converter = new JsonAttributeConverter();

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConvertFromJson() {

        Object result = converter.convertToEntityAttribute("[\"foo\",\"bar\"]");

        assertThat(result).isInstanceOfSatisfying(List.class, list -> assertThat(list).containsExactly("foo", "bar"));
    }
    
    @Test
    public void shouldConvertToJson() {

        String json = converter.convertToDatabaseColumn(ImmutableList.of("foo", "bar"));

        assertThat(json).isEqualTo("[\"foo\",\"bar\"]");
    }

    @Test
    public void shouldHandleNulls() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
