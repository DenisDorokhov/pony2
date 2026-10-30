package net.dorokhov.pony2.common;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;

import java.util.List;

public class ListOfObjectsJsonAttributeConverter implements AttributeConverter<List<Object>, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable List<Object> attribute) {
        return attribute != null ? JsonConverter.toJson(attribute) : null;
    }

    @Override
    public @Nullable List<Object> convertToEntityAttribute(@Nullable String dbData) {
        return dbData != null ? JsonConverter.listFromJson(dbData, Object.class) : null;
    }
}
