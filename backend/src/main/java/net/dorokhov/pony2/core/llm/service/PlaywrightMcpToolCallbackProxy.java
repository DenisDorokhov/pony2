package net.dorokhov.pony2.core.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class PlaywrightMcpToolCallbackProxy {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PlaywrightMcpCaller playwrightMcpCaller;

    public PlaywrightMcpToolCallbackProxy(PlaywrightMcpCaller playwrightMcpCaller) {
        this.playwrightMcpCaller = playwrightMcpCaller;
    }

    public List<ToolCallbackProvider> proxy(Stream<ToolCallbackProvider> toolCallbackProviders) {
        return toolCallbackProviders
                .map(this::proxy)
                .toList();
    }

    private ToolCallbackProvider proxy(ToolCallbackProvider toolCallbackProvider) {
        Objects.requireNonNull(toolCallbackProvider, "Tool callback provider must not be null.");
        return () -> Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(this::proxy)
                .toArray(ToolCallback[]::new);
    }

    private ToolCallback proxy(ToolCallback toolCallback) {
        String toolName = toolCallback.getToolDefinition().name();
        if (!PlaywrightMcpCaller.isPlaywrightToolName(toolName)) {
            return toolCallback;
        }
        if (toolCallback instanceof RateLimitedPlaywrightMcpToolCallback) {
            return toolCallback;
        }
        logger.debug("Proxying Playwright MCP tool '{}'.", toolName);
        return new RateLimitedPlaywrightMcpToolCallback(toolCallback, playwrightMcpCaller);
    }
}
