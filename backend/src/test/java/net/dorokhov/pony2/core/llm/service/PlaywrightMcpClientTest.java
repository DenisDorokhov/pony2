package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.core.llm.service.PlaywrightMcpClient.RandomDelay;
import net.dorokhov.pony2.core.llm.service.PlaywrightMcpClient.ToolCall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlaywrightMcpClientTest {

    private final List<Duration> sleepDurations = new ArrayList<>();

    @Test
    public void shouldCallToolSequenceInOneBrowserSession() {

        StubToolCallback navigateCallback = new StubToolCallback("browser_navigate", "navigateResult");
        StubToolCallback evaluateCallback = new StubToolCallback("browser_evaluate", "evaluateResult");
        PlaywrightMcpClient client = client(providerOf(() -> new ToolCallback[]{navigateCallback, evaluateCallback}));

        List<String> result = client.call(List.of(
                new ToolCall("browser_navigate", "{\"url\":\"https://example.com\"}"),
                new ToolCall("browser_evaluate", "{\"function\":\"() => {}\"}")
        ));

        assertThat(result).containsExactly("navigateResult", "evaluateResult");
        assertThat(navigateCallback.inputs).containsExactly("{\"url\":\"https://example.com\"}");
        assertThat(evaluateCallback.inputs).containsExactly("{\"function\":\"() => {}\"}");
        assertThat(sleepDurations).containsExactly(Duration.ofSeconds(6));
    }

    @Test
    public void shouldApplyDelaysOnlyToConfiguredToolTypes() {

        StubToolCallback snapshotCallback = new StubToolCallback("browser_snapshot", "snapshotResult");
        StubToolCallback scrollCallback = new StubToolCallback("browser_scroll", "scrollResult");
        PlaywrightMcpClient client = client(providerOf(() -> new ToolCallback[]{snapshotCallback, scrollCallback}));

        String snapshotResult = client.call("browser_snapshot", "{}");
        String scrollResult = client.call("browser_scroll", "{}");

        assertThat(snapshotResult).isEqualTo("snapshotResult");
        assertThat(scrollResult).isEqualTo("scrollResult");
        assertThat(sleepDurations).containsExactly(Duration.ofMillis(2500), Duration.ofSeconds(6));
    }

    @Test
    public void shouldFailWhenPlaywrightToolIsMissing() {

        PlaywrightMcpClient client = client(providerOf());

        assertThatThrownBy(() -> client.call("browser_missing", "{}"))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"browser_navigate", "browser_evaluate"})
    public void shouldFailToolCallingWhenBrowserRetriesAreExhausted(String toolName) {
        ToolCallback browserTool = browserTool(toolName);
        ToolCallback browserTabs = browserTool("browser_tabs");
        IllegalStateException browserFailure = new IllegalStateException(
                "Browser is already in use for /data/profile, use --isolated to run multiple instances of the same browser");
        ToolExecutionException toolFailure = new ToolExecutionException(browserTool.getToolDefinition(), browserFailure);
        ToolExecutionException probeFailure = new ToolExecutionException(browserTabs.getToolDefinition(), browserFailure);
        when(browserTool.call(eq("{}"), any(ToolContext.class))).thenThrow(toolFailure);
        when(browserTabs.call(eq("{\"action\":\"list\"}"), any(ToolContext.class))).thenThrow(probeFailure);
        ToolCallback callback = proxiedBrowserTool(browserTool, browserTabs);

        assertThatThrownBy(() -> executeToolCall(callback, "{}"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining(toolName)
                .hasCause(toolFailure)
                .hasRootCause(browserFailure)
                .satisfies(failure -> assertThat(failure.getSuppressed()).containsExactly(probeFailure));

        verify(browserTool, times(2)).call(eq("{}"), any(ToolContext.class));
        verify(browserTabs, times(2)).call(eq("{\"action\":\"list\"}"), any(ToolContext.class));
    }

    private ToolCallback browserTool(String name) {
        ToolCallback browserTool = mock(ToolCallback.class);
        when(browserTool.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name).description(name).inputSchema("{}").build());
        when(browserTool.getToolMetadata()).thenCallRealMethod();
        return browserTool;
    }

    private ToolCallback proxiedBrowserTool(ToolCallback browserTool, ToolCallback... otherTools) {
        ToolCallback[] tools = Stream.concat(Stream.of(browserTool), Arrays.stream(otherTools)).toArray(ToolCallback[]::new);
        PlaywrightMcpClient client = client(providerOf(() -> tools));
        PlaywrightMcpToolRateLimiter rateLimiter = new PlaywrightMcpToolRateLimiter(
                new RateLimitedPlaywrightMcpClient(client, Duration.ZERO, Duration.ZERO, 1));
        return new McpToolArgumentValidator(3).validate(rateLimiter.rateLimit(
                Stream.of(() -> tools)).stream()).getFirst().getToolCallbacks()[0];
    }

    private ToolExecutionResult executeToolCall(ToolCallback callback, String arguments) {
        Prompt prompt = new Prompt("Discover Agalloch", ToolCallingChatOptions.builder().toolCallbacks(callback).build());
        AssistantMessage message = AssistantMessage.builder().toolCalls(List.of(
                new AssistantMessage.ToolCall("call-1", "function", callback.getToolDefinition().name(), arguments))).build();
        return DefaultToolCallingManager.builder().build().executeToolCalls(prompt,
                new ChatResponse(List.of(new Generation(message))));
    }

    @Test
    public void shouldReturnSuccessfulBrowserResultAfterRetry() {
        ToolCallback browserTool = browserTool("browser_navigate");
        ToolCallback browserTabs = browserTool("browser_tabs");
        ToolExecutionException toolFailure = new ToolExecutionException(browserTool.getToolDefinition(),
                new IllegalStateException("Browser temporarily unavailable"));
        when(browserTool.call(eq("{}"), any(ToolContext.class))).thenThrow(toolFailure).thenReturn("Spotify page");
        when(browserTabs.call(eq("{\"action\":\"list\"}"), any(ToolContext.class))).thenReturn("Spotify tab");

        ToolExecutionResult result = executeToolCall(proxiedBrowserTool(browserTool, browserTabs), "{}");

        ToolResponseMessage response = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(response.getResponses().getFirst().responseData()).isEqualTo("Spotify page");
        verify(browserTool, times(2)).call(eq("{}"), any(ToolContext.class));
        verify(browserTabs).call(eq("{\"action\":\"list\"}"), any(ToolContext.class));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "browser_evaluate|TypeError: Cannot read properties of null (reading 'textContent')",
            "browser_evaluate|ReferenceError: artist is not defined",
            "browser_evaluate|SyntaxError: Unexpected token '}'",
            "browser_click|Error: Ref f8e491 not found in the current page snapshot. Try capturing new snapshot.",
            "browser_wait_for|TimeoutError: Timeout 10000ms exceeded.",
            "browser_handle_dialog|Error: The tool can only be used when there is related modal state present."
    })
    public void shouldReturnToolErrorToModelWhenBrowserIsAvailable(String toolName, String errorMessage) {
        ToolCallback browserTool = browserTool(toolName);
        ToolCallback browserTabs = browserTool("browser_tabs");
        ToolExecutionException toolFailure = new ToolExecutionException(browserTool.getToolDefinition(),
                new IllegalStateException(errorMessage));
        when(browserTool.call(eq("{}"), any(ToolContext.class))).thenThrow(toolFailure);
        when(browserTabs.call(eq("{\"action\":\"list\"}"), any(ToolContext.class))).thenReturn("Spotify tab");

        ToolExecutionResult result = executeToolCall(proxiedBrowserTool(browserTool, browserTabs), "{}");

        ToolResponseMessage response = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(response.getResponses().getFirst().responseData()).contains(errorMessage);
        verify(browserTool, times(2)).call(eq("{}"), any(ToolContext.class));
        verify(browserTabs, times(2)).call(eq("{\"action\":\"list\"}"), any(ToolContext.class));
    }

    @Test
    public void shouldAllowArgumentCorrectionWithoutExecutingBrowserTool() {
        ToolCallback browserTool = browserTool("browser_navigate");

        ToolExecutionResult result = executeToolCall(proxiedBrowserTool(browserTool), "{");

        ToolResponseMessage response = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(response.getResponses().getFirst().responseData())
                .contains("The tool was not executed", "Correction 1 of 3");
        verify(browserTool, never()).call(any(), any(ToolContext.class));
    }

    private PlaywrightMcpClient client(ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        return new PlaywrightMcpClient(
                toolCallbackProviders,
                new RandomDelay(Duration.ofMillis(500), Duration.ofMillis(2500)),
                new RandomDelay(Duration.ofSeconds(2), Duration.ofSeconds(6)),
                sleepDurations::add,
                bound -> bound - 1
        );
    }

    private ObjectProvider<ToolCallbackProvider> providerOf(ToolCallbackProvider... providers) {
        return new ObjectProvider<>() {

            @Override
            public ToolCallbackProvider getObject(Object... args) {
                return getObject();
            }

            @Override
            public ToolCallbackProvider getIfAvailable() {
                return providers.length == 0 ? null : providers[0];
            }

            @Override
            public ToolCallbackProvider getIfUnique() {
                return providers.length == 1 ? providers[0] : null;
            }

            @Override
            public ToolCallbackProvider getObject() {
                return providers[0];
            }

            @Override
            public Stream<ToolCallbackProvider> stream() {
                return Arrays.stream(providers);
            }

            @Override
            public Stream<ToolCallbackProvider> orderedStream() {
                return stream();
            }
        };
    }

    private static class StubToolCallback implements ToolCallback {

        private final ToolDefinition toolDefinition;
        private final String result;
        private final List<String> inputs = new ArrayList<>();

        private StubToolCallback(String name, String result) {
            toolDefinition = ToolDefinition.builder()
                    .name(name)
                    .description(name + " description")
                    .inputSchema("{}")
                    .build();
            this.result = result;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        public String call(String input) {
            inputs.add(input);
            return result;
        }
    }
}
