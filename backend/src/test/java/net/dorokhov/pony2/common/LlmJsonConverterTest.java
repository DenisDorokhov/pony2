package net.dorokhov.pony2.common;

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

    public record NamedValue(String name) {}
}
