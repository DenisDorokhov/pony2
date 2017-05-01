package net.dorokhov.pony.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.io.Serializable;

public class JsonAttributeConverter implements AttributeConverter<Serializable, String> {

    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(@Nullable Serializable attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Serializable convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return mapper.readValue(dbData, new TypeReference<Object>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
