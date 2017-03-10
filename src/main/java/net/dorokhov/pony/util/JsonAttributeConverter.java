package net.dorokhov.pony.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonAttributeConverter<T> implements AttributeConverter<T, String> {

    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(T attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, new TypeReference<T>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ListConverter extends JsonAttributeConverter<List<Object>> {
    }

    public static class MapConverter extends JsonAttributeConverter<Map<String, Object>> {
    }
}
