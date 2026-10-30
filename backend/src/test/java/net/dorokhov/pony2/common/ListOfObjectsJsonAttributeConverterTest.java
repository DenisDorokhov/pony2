package net.dorokhov.pony2.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListOfObjectsJsonAttributeConverterTest {

    private final ListOfObjectsJsonAttributeConverter converter = new ListOfObjectsJsonAttributeConverter();

    @Test
    void shouldRoundTripMixedJsonValues() {
        List<Object> values = Arrays.asList(Map.of("prompt", "Artist\n\"Album\""),
                "https://musicbrainz.org/artist", null, List.of(true, 42));

        String json = converter.convertToDatabaseColumn(values);

        assertThat(converter.convertToEntityAttribute(json)).isEqualTo(values);
        assertThat(json).startsWith("[{\"prompt\":");
    }

    @Test
    void shouldPreserveNullAndEmptyLists() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(List.of())).isEqualTo("[]");
        assertThat(converter.convertToEntityAttribute("[]")).isEmpty();
    }
}
