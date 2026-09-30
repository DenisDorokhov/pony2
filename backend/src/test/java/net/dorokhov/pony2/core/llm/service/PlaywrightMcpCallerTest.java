package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.core.llm.service.PlaywrightMcpCaller.RandomDelay;
import net.dorokhov.pony2.core.llm.service.RateLimitedRequestExecutor.Settings;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PlaywrightMcpCallerTest {

    private final MutableClock clock = new MutableClock();
    private final List<Duration> sleepDurations = new ArrayList<>();

    @Test
    public void shouldCallPlaywrightToolByNameWithConfiguredDelays() {

        StubToolCallback toolCallback = new StubToolCallback("browser_scroll", "result");
        PlaywrightMcpCaller toolCaller = toolCaller(providerOf((ToolCallbackProvider) () ->
                new ToolCallback[]{toolCallback}));

        String result = toolCaller.call("browser_scroll", "{}");

        assertThat(result).isEqualTo("result");
        assertThat(sleepDurations).containsExactly(Duration.ofMillis(2500), Duration.ofSeconds(6));
    }

    @Test
    public void shouldNotAddHumanDelaysToReadOnlyPlaywrightTool() {

        StubToolCallback toolCallback = new StubToolCallback("browser_snapshot", "result");
        PlaywrightMcpCaller toolCaller = toolCaller(providerOf((ToolCallbackProvider) () ->
                new ToolCallback[]{toolCallback}));

        String result = toolCaller.call("browser_snapshot", "{}");

        assertThat(result).isEqualTo("result");
        assertThat(sleepDurations).isEmpty();
    }

    @Test
    public void shouldFailWhenPlaywrightToolIsMissing() {

        PlaywrightMcpCaller toolCaller = toolCaller(providerOf());

        assertThatThrownBy(() -> toolCaller.call("browser_missing", "{}"))
                .isInstanceOf(IllegalStateException.class);
    }

    private PlaywrightMcpCaller toolCaller(ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        return new PlaywrightMcpCaller(
                toolCallbackProviders,
                new RateLimitedRequestExecutor(
                        "test-playwright-mcp",
                        new Settings(Duration.ZERO, Duration.ZERO, 0),
                        clock,
                        duration -> {
                            sleepDurations.add(duration);
                            clock.plus(duration);
                        },
                        bound -> 0
                ),
                new RandomDelay(Duration.ofMillis(500), Duration.ofMillis(2500)),
                new RandomDelay(Duration.ofSeconds(2), Duration.ofSeconds(6)),
                duration -> {
                    sleepDurations.add(duration);
                    clock.plus(duration);
                },
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
            return result;
        }
    }

    private static class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-09-30T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void plus(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
