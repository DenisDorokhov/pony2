package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.core.llm.service.PlaywrightMcpClient.RandomDelay;
import net.dorokhov.pony2.core.llm.service.PlaywrightMcpClient.ToolCall;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
