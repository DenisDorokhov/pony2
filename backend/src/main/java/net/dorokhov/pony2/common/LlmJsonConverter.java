package net.dorokhov.pony2.common;

import io.github.glaforge.jsonspotter.JsonSpotter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

public final class LlmJsonConverter {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

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
        return MAPPER.readValue(json, clazz);
    }
}
