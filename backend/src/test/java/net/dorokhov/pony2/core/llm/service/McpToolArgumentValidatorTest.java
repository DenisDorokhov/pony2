package net.dorokhov.pony2.core.llm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolArgumentValidatorTest {

    @Mock private ToolCallback delegate;

    private final McpToolArgumentValidator validator = new McpToolArgumentValidator(3);

    @BeforeEach
    void setUp() {
        lenient().when(delegate.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name("selenium_execute_script").description("Execute a script").inputSchema("{}").build());
    }

    @Test
    void shouldPreserveToolDefinitionAndMetadata() {
        ToolMetadata metadata = ToolMetadata.builder().returnDirect(true).build();
        when(delegate.getToolMetadata()).thenReturn(metadata);
        ToolCallback callback = callback();

        assertThat(callback.getToolDefinition()).isSameAs(delegate.getToolDefinition());
        assertThat(callback.getToolMetadata()).isSameAs(metadata);
    }

    private ToolCallback callback() {
        return validator.validate(Stream.of(() -> new ToolCallback[]{delegate})).getFirst().getToolCallbacks()[0];
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPassOriginalArgumentsAndContext(boolean withContext) {
        ToolCallback callback = callback();
        String input = " \n{\"function\":\"() => { return document.title; }\",\"values\":[1,true,null,{\"key\":\"value\"}]}\n";
        ToolContext context = new ToolContext(Map.of("key", "value"));
        if (withContext) {
            when(delegate.call(input, context)).thenReturn("result");

            assertThat(callback.call(input, context)).isEqualTo("result");

            verify(delegate).call(input, context);
        } else {
            when(delegate.call(input)).thenReturn("result");

            assertThat(callback.call(input)).isEqualTo("result");

            verify(delegate).call(input);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void shouldPreserveEmptyArgumentNormalization(String input) {
        ToolCallback callback = callback();
        when(delegate.call("{}")).thenReturn("result");

        assertThat(callback.call(input)).isEqualTo("result");

        verify(delegate).call("{}");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"function\":\"() => { return document.title;",
            "{", "[]", "null", "true", "123", "\"text\"", "{} {}", "{} trailing text"
    })
    void shouldReportInvalidArgumentsWithoutExecutingTool(String input) {
        ToolCallback callback = callback();

        assertThatThrownBy(() -> callback.call(input))
                .isInstanceOf(ToolExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("selenium_execute_script", "The tool was not executed", "Correction 1 of 3");

        verify(delegate, never()).call(anyString());
        verify(delegate, never()).call(anyString(), any());
    }

    @Test
    void shouldValidateArgumentsWithContext() {
        ToolCallback callback = callback();

        assertThatThrownBy(() -> callback.call("{", new ToolContext(Map.of())))
                .isInstanceOf(ToolExecutionException.class);

        verify(delegate, never()).call(anyString(), any());
    }

    @Test
    void shouldShareCorrectionBudgetAcrossProvidersAndCallbackInstances() {
        ToolCallback otherTool = mock(ToolCallback.class);
        when(otherTool.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name("browser_evaluate").description("Evaluate JavaScript").inputSchema("{}").build());
        List<ToolCallbackProvider> providers = validator.validate(Stream.of(
                () -> new ToolCallback[]{delegate}, () -> new ToolCallback[]{otherTool}));

        assertThatThrownBy(() -> providers.getFirst().getToolCallbacks()[0].call("{"))
                .isInstanceOf(ToolExecutionException.class).hasMessageContaining("Correction 1 of 3");
        assertThatThrownBy(() -> providers.getLast().getToolCallbacks()[0].call("{"))
                .isInstanceOf(ToolExecutionException.class).hasMessageContaining("Correction 2 of 3");
        assertThatThrownBy(() -> providers.getFirst().getToolCallbacks()[0].call("{"))
                .isInstanceOf(ToolExecutionException.class).hasMessageContaining("Correction 3 of 3");
        assertThatThrownBy(() -> providers.getLast().getToolCallbacks()[0].call("{"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll("correction limit (3) exceeded", "browser_evaluate");

        verify(delegate, never()).call(anyString());
        verify(otherTool, never()).call(anyString());
    }

    @Test
    void shouldNotResetBudgetAfterSuccessfulCall() {
        ToolCallback callback = callback();
        when(delegate.call("{}")).thenReturn("result");
        for (int correction = 1; correction <= 3; correction++) {
            assertThatThrownBy(() -> callback.call("{"))
                    .isInstanceOf(ToolExecutionException.class)
                    .hasMessageContaining("Correction " + correction + " of 3");
        }

        assertThat(callback.call("{}")).isEqualTo("result");
        assertThatThrownBy(() -> callback.call("{"))
                .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldFailImmediatelyWhenCorrectionsAreDisabled() {
        ToolCallback callback = new McpToolArgumentValidator(0)
                .validate(Stream.of(() -> new ToolCallback[]{delegate})).getFirst().getToolCallbacks()[0];

        assertThatThrownBy(() -> callback.call("{"))
                .isExactlyInstanceOf(IllegalStateException.class).hasMessageContaining("correction limit (0) exceeded");

        verify(delegate, never()).call(anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPropagateExecutionFailuresUnchanged(boolean withContext) {
        ToolCallback callback = callback();
        IllegalStateException failure = new IllegalStateException("MCP server failed");
        ToolContext context = new ToolContext(Map.of());
        if (withContext) {
            when(delegate.call("{}", context)).thenThrow(failure);

            assertThatThrownBy(() -> callback.call("{}", context)).isSameAs(failure);
        } else {
            when(delegate.call("{}")).thenThrow(failure);

            assertThatThrownBy(() -> callback.call("{}")).isSameAs(failure);
        }
    }
}
