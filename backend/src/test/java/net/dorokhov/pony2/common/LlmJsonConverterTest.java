package net.dorokhov.pony2.common;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmJsonConverterTest {

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":\"Foobar\"}", "[\"foo\",null,\"bar\"]", "[]", "{}",
            "\"text with {} and []\"", "42", "true", "null"})
    void shouldParseEntireJsonResponse(String response) {
        assertThat(LlmJsonConverter.fromJson(response)).isEqualTo(JsonConverter.fromJson(response));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%s", "Here is the result:\n%s", "%s\nDone.", "```json\n%s\n```",
            "Here is the result:\n```json\n%s\n```\nDone."})
    void shouldParseTypedObjectWithSurroundingText(String template) {
        String response = template.formatted("{\"name\":\"Foobar\"}");

        assertThat(LlmJsonConverter.fromJson(response, NamedValue.class)).isEqualTo(new NamedValue("Foobar"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Here is the result:\n%s", "%s\nDone.", "```json\n%s\n```"})
    void shouldParseArrayWithSurroundingText(String template) {
        String response = template.formatted("[\"foo\",null,\"bar\"]");

        assertThat(LlmJsonConverter.fromJson(response)).isEqualTo(JsonConverter.fromJson("[\"foo\",null,\"bar\"]"));
        assertThat(LlmJsonConverter.fromJson(response, String[].class)).containsExactly("foo", null, "bar");
    }

    @Test
    void shouldExtractLongestJsonFragment() {
        String response = "Example: {}\nResult: {\"name\":\"Foobar\"}\nDone.";

        assertThat(LlmJsonConverter.fromJson(response, NamedValue.class)).isEqualTo(new NamedValue("Foobar"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not JSON", "Result: {\"name\":\"Foobar\"",
            "Result: {name: 'Foobar'}", "Result: {\"name\":\"Foobar\",}"})
    void shouldFailWithJacksonExceptionWhenJsonCannotBeParsed(String response) {
        assertThatThrownBy(() -> LlmJsonConverter.fromJson(response))
                .isInstanceOf(JacksonException.class);
        assertThatThrownBy(() -> LlmJsonConverter.fromJson(response, NamedValue.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void shouldFailWithJacksonExceptionWhenExtractedJsonDoesNotMatchRequestedType() {
        assertThatThrownBy(() -> LlmJsonConverter.fromJson("Result: [\"Foobar\"]", NamedValue.class))
                .isInstanceOf(JacksonException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "Result: {}", "```json\n{}\n```"})
    void shouldRejectOmittedNullableField(String response) {
        assertThatThrownBy(() -> LlmJsonConverter.fromJson(response, NullableValue.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void shouldAcceptExplicitNullForNullableFields() {
        assertThat(LlmJsonConverter.fromJson("{\"value\":null}", NullableValue.class))
                .isEqualTo(new NullableValue(null));
        assertThat(LlmJsonConverter.fromJson("{\"nested\":{\"value\":null}}", NestedValue.class))
                .isEqualTo(new NestedValue(new NullableValue(null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"nested\":{}}", "Result: {\"nested\":{}}"})
    void shouldRejectOmittedNullableFieldInNestedObject(String response) {
        assertThatThrownBy(() -> LlmJsonConverter.fromJson(response, NestedValue.class))
                .isInstanceOf(JacksonException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":\"Foobar\",\"extra\":true}", "{\"name\":42}",
            "{\"name\":1.5}", "{\"name\":true}"})
    void shouldRejectUnknownFieldsAndCoercionToText(String response) {
        assertThatThrownBy(() -> LlmJsonConverter.fromJson(response, NamedValue.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void shouldKeepStoredJsonParsingPermissive() {
        assertThat(JsonConverter.fromJson("{}", NullableValue.class)).isEqualTo(new NullableValue(null));
        assertThat(JsonConverter.fromJson("{\"name\":\"Foobar\",\"extra\":true}", NamedValue.class))
                .isEqualTo(new NamedValue("Foobar"));
    }

    public record NamedValue(String name) {}

    public record NullableValue(@Nullable String value) {}

    public record NestedValue(NullableValue nested) {}
}
