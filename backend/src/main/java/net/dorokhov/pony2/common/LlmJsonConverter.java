package net.dorokhov.pony2.common;

import io.github.glaforge.jsonspotter.JsonSpotter;
import tools.jackson.core.JacksonException;

public final class LlmJsonConverter {

    private LlmJsonConverter() {
    }

    /**
     * @throws JacksonException if neither the response nor an extracted JSON fragment can be parsed
     */
    public static Object fromJson(String response) throws JacksonException {
        return fromJson(response, Object.class);
    }

    /**
     * @throws JacksonException if neither the response nor an extracted JSON fragment can be deserialized
     *                          into the requested type
     */
    public static <T> T fromJson(String response, Class<T> clazz) throws JacksonException {
        try {
            return parseJson(response, clazz);
        } catch (JacksonException e) {
            String json = JsonSpotter.extractJson(response);
            if (json.isEmpty()) {
                throw e;
            }
            return parseJson(json, clazz);
        }
    }

    private static <T> T parseJson(String json, Class<T> clazz) throws JacksonException {
        try {
            return JsonConverter.fromJson(json, clazz);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof JacksonException jacksonException) {
                throw jacksonException;
            }
            throw e;
        }
    }
}
