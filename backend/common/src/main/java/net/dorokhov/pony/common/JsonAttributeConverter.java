package net.dorokhov.pony.common;

import java.io.Serializable;
import javax.annotation.Nullable;
import javax.persistence.AttributeConverter;

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
