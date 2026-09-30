package net.dorokhov.pony2.core.llm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlaywrightMcpToolRateLimiterTest {

    private PlaywrightMcpToolRateLimiter rateLimiter;

    @Mock
    private RateLimitedPlaywrightMcpClient rateLimitedPlaywrightMcpClient;

    @BeforeEach
    void setUp() {
        rateLimiter = new PlaywrightMcpToolRateLimiter(rateLimitedPlaywrightMcpClient);
    }

    @Test
    public void shouldRateLimitPlaywrightToolCallbacks() {

        StubToolCallback browserTool = new StubToolCallback("browser_scroll");
        StubToolCallback otherTool = new StubToolCallback("other_tool");

        ToolCallbackProvider proxiedProvider = rateLimiter.rateLimit(Stream.of(() -> new ToolCallback[]{browserTool, otherTool})).getFirst();
        ToolCallback[] proxiedCallbacks = proxiedProvider.getToolCallbacks();

        assertThat(proxiedCallbacks).hasSize(2);
        assertThat(proxiedCallbacks[0]).isNotSameAs(browserTool);
        assertThat(proxiedCallbacks[0].getToolDefinition()).isSameAs(browserTool.getToolDefinition());
        assertThat(proxiedCallbacks[0].getToolMetadata()).isSameAs(browserTool.getToolMetadata());
        assertThat(proxiedCallbacks[1]).isSameAs(otherTool);
    }

    @Test
    public void shouldCallPlaywrightToolThroughRateLimitedClient() {

        StubToolCallback browserTool = new StubToolCallback("browser_click");
        ToolCallback proxiedCallback = rateLimiter.rateLimit(Stream.of(() -> new ToolCallback[]{browserTool})).getFirst().getToolCallbacks()[0];

        when(rateLimitedPlaywrightMcpClient.call(browserTool, "{}")).thenReturn("result");

        assertThat(proxiedCallback.call("{}")).isEqualTo("result");

        verify(rateLimitedPlaywrightMcpClient).call(browserTool, "{}");
    }

    @Test
    public void shouldCallPlaywrightToolWithContextThroughRateLimitedClient() {

        StubToolCallback browserTool = new StubToolCallback("browser_click");
        ToolCallback proxiedCallback = rateLimiter.rateLimit(Stream.of(() -> new ToolCallback[]{browserTool})).getFirst().getToolCallbacks()[0];
        ToolContext toolContext = new ToolContext(Map.of("key", "value"));

        when(rateLimitedPlaywrightMcpClient.call(browserTool, "{}", toolContext)).thenReturn("result");

        assertThat(proxiedCallback.call("{}", toolContext)).isEqualTo("result");

        verify(rateLimitedPlaywrightMcpClient).call(browserTool, "{}", toolContext);
    }

    private static class StubToolCallback implements ToolCallback {

        private final ToolDefinition toolDefinition;
        private final ToolMetadata toolMetadata = ToolMetadata.builder()
                .returnDirect(true)
                .build();

        private StubToolCallback(String name) {
            toolDefinition = ToolDefinition.builder()
                    .name(name)
                    .description(name + " description")
                    .inputSchema("{}")
                    .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return toolMetadata;
        }

        @Override
        public String call(String input) {
            return "raw result";
        }
    }
}
