package net.dorokhov.pony3.common;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
            return MAPPER.readValue(json, new TypeReference<Object>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, new TypeReference<T>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> List<T> listFromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, new TypeReference<List<T>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <K, V> Map<K, V> mapFromJson(String json, Class<K> keyClass, Class<V> keyValue) {
        try {
            return MAPPER.readValue(json, new TypeReference<Map<K, V>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
