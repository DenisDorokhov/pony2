package net.dorokhov.pony2.core.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Component
public class PlaywrightMcpToolRateLimiter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RateLimitedPlaywrightMcpClient rateLimitedPlaywrightMcpClient;

    public PlaywrightMcpToolRateLimiter(RateLimitedPlaywrightMcpClient rateLimitedPlaywrightMcpClient) {
        this.rateLimitedPlaywrightMcpClient = rateLimitedPlaywrightMcpClient;
    }

    public List<ToolCallbackProvider> rateLimit(Stream<ToolCallbackProvider> toolCallbackProviders) {
        return toolCallbackProviders
                .map(this::rateLimit)
                .toList();
    }

    private ToolCallbackProvider rateLimit(ToolCallbackProvider toolCallbackProvider) {
        return () -> Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(this::rateLimit)
                .toArray(ToolCallback[]::new);
    }

    private ToolCallback rateLimit(ToolCallback toolCallback) {
        String toolName = toolCallback.getToolDefinition().name();
        if (!RateLimitedPlaywrightMcpClient.isPlaywrightToolName(toolName)) {
            return toolCallback;
        }
        if (toolCallback instanceof RateLimitedToolCallback) {
            return toolCallback;
        }
        logger.debug("Decorating Playwright MCP tool callback '{}' with rate limit.", toolName);
        return new RateLimitedToolCallback(toolCallback, rateLimitedPlaywrightMcpClient);
    }

    private static class RateLimitedToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final RateLimitedPlaywrightMcpClient rateLimitedPlaywrightMcpClient;

        private RateLimitedToolCallback(
                ToolCallback delegate,
                RateLimitedPlaywrightMcpClient rateLimitedPlaywrightMcpClient
        ) {
            this.delegate = delegate;
            this.rateLimitedPlaywrightMcpClient = rateLimitedPlaywrightMcpClient;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String input) {
            return rateLimitedPlaywrightMcpClient.call(delegate, input);
        }

        @Override
        public String call(String input, ToolContext toolContext) {
            return rateLimitedPlaywrightMcpClient.call(delegate, input, toolContext);
        }
    }
}
