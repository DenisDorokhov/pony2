package net.dorokhov.pony3.common;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;

import java.util.List;

public class ListOfStringsJsonAttributeConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(@Nullable List<String> attribute) {
        if (attribute == null) {
            return null;
        }
        return JsonConverter.toJson(attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return null;
        }
        return JsonConverter.listFromJson(dbData, String.class);
    }
}
