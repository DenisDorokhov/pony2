package net.dorokhov.pony2.common;

import tools.jackson.core.type.TypeReference;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

public final class JsonConverter {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .configure(EnumFeature.READ_ENUMS_USING_TO_STRING, false)
            .configure(EnumFeature.WRITE_ENUMS_USING_TO_STRING, false)
            .build();

    private JsonConverter() {
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Object fromJson(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T fromJson(String json, Class<T> ignoredClazz) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> List<T> listFromJson(String json, Class<T> ignoredClazz) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <K, V> Map<K, V> mapFromJson(String json, Class<K> ignoredKeyClass, Class<V> ignoredValueClass) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
