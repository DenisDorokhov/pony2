package net.dorokhov.pony.common;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonAttributeConverterTests {

    private JsonAttributeConverter converter = new JsonAttributeConverter();

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConvertFromJson() throws Exception {
        Object result = converter.convertToEntityAttribute("[\"foo\",\"bar\"]");
        assertThat(result).isInstanceOfSatisfying(List.class, list -> assertThat(list).containsExactly("foo", "bar"));
    }
    
    @Test
    public void shouldConvertToJson() throws Exception {
        String json = converter.convertToDatabaseColumn(ImmutableList.of("foo", "bar"));
        assertThat(json).isEqualTo("[\"foo\",\"bar\"]");
    }

    @Test
    public void shouldHandleNulls() throws Exception {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
