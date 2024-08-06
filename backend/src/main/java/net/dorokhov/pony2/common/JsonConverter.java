package net.dorokhov.pony2.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class JsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonConverter() {
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Object fromJson(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T fromJson(String json, Class<T> ignoredClazz) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> List<T> listFromJson(String json, Class<T> ignoredClazz) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <K, V> Map<K, V> mapFromJson(String json, Class<K> ignoredKeyClass, Class<V> ignoredValueClass) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
