package net.dorokhov.pony3.common;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;

import java.io.Serializable;

public class JsonAttributeConverter implements AttributeConverter<Serializable, String> {

    @Override
    public String convertToDatabaseColumn(@Nullable Serializable attribute) {
        if (attribute == null) {
            return null;
        }
        return JsonConverter.toJson(attribute);
    }

    @Override
    public Serializable convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return null;
        }
        return (Serializable) JsonConverter.fromJson(dbData);
    }
}
