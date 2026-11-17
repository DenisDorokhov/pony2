package net.dorokhov.pony2.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmPromptTemplateTest {

    @Test
    void shouldRenderWithoutInterpretingSourceData() throws IOException {
        LlmPromptTemplate prompt = template("Inspect <dialog> and {\"content\":\"100%\"}.\nData: ${data}\nEnd.");
        String data = "{\"text\":\"$value #if($enabled) <dialog> 50% 😀\"}\nLine two\r\n#end\n";

        assertThat(prompt.render("data", data))
                .isEqualTo("Inspect <dialog> and {\"content\":\"100%\"}.\nData: " + data + "\nEnd.");
    }

    private LlmPromptTemplate template(String source) throws IOException {
        return new LlmPromptTemplate(new ByteArrayResource(source.getBytes(UTF_8)));
    }

    @Test
    void shouldPreserveIndentationAndMultilineParameterValues() throws IOException {
        LlmPromptTemplate prompt = template("Text:\n  Static line.\n${empty}${data}\nEnd.");

        assertThat(prompt.render(Map.of("data", "First\r\nSecond\n", "empty", "")))
                .isEqualTo("Text:\n  Static line.\nFirst\r\nSecond\n\nEnd.");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void shouldPreserveLeadingAndTrailingNewlines(int trailingNewlines) throws IOException {
        String source = "\nFirst\nLast" + "\n".repeat(trailingNewlines);

        assertThat(template(source).render()).isEqualTo(source);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPreserveWhitespaceAroundConditionalText(boolean selected) throws IOException {
        LlmPromptTemplate prompt = template("Before #if($selected)selected#{else}normal#end after.\n\n");

        assertThat(prompt.render("selected", selected))
                .isEqualTo("Before " + (selected ? "selected" : "normal") + " after.\n\n");
    }

    @Test
    void shouldOnlyRequireParametersInTheSelectedBranch() throws IOException {
        LlmPromptTemplate prompt = template("Before#if($enabled) ${details}#end after.");

        assertThat(prompt.render("enabled", false)).isEqualTo("Before after.");
        assertThat(prompt.render(Map.of("enabled", true, "details", "Optional")))
                .isEqualTo("Before Optional after.");
    }

    @Test
    void shouldRenderLoopWithConditionalYearsIncludingZero() throws IOException {
        LlmPromptTemplate prompt = template("Albums:\n#foreach($album in $albums)##\n- ${album.title}"
                + "#if($album.hasYear) (year: ${album.year})#end#if($foreach.hasNext)\n#end##\n#end");
        List<Map<String, Object>> albums = List.of(
                Map.of("title", "First $album", "hasYear", true, "year", 0),
                Map.of("title", "Second <dialog>", "hasYear", false, "year", ""));

        assertThat(prompt.render("albums", albums))
                .isEqualTo("Albums:\n- First $album (year: 0)\n- Second <dialog>");
        assertThat(prompt.render("albums", List.of())).isEqualTo("Albums:\n");
    }

    @Test
    void shouldRejectMissingParameters() throws IOException {
        LlmPromptTemplate prompt = template("Value: ${value}");

        assertThatThrownBy(prompt::render)
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("value");
    }

    @Test
    void shouldRejectMissingObjectProperties() throws IOException {
        LlmPromptTemplate prompt = template("${item.missing}");

        assertThatThrownBy(() -> prompt.render("item", Map.of("value", "present")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing");
    }

    @Test
    void shouldRejectMalformedConditions() throws IOException {
        LlmPromptTemplate prompt = template("#if($selected)Selected");

        assertThatThrownBy(() -> prompt.render("selected", true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid prompt template");
    }

    @Test
    void shouldIsolateParametersAcrossConcurrentRenders() throws Exception {
        LlmPromptTemplate prompt = template("Value: ${value}");
        try (ExecutorService workers = Executors.newFixedThreadPool(4)) {
            List<Future<String>> results = new ArrayList<>();
            for (int index = 0; index < 16; index++) {
                String value = Integer.toString(index);
                results.add(workers.submit(() -> prompt.render("value", value)));
            }
            for (int index = 0; index < results.size(); index++) {
                assertThat(results.get(index).get()).isEqualTo("Value: " + index);
            }
        }
    }
}
