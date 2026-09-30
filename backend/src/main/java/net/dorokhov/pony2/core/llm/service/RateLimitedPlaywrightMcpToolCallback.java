package net.dorokhov.pony2.core.llm.service;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Objects;

class RateLimitedPlaywrightMcpToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final PlaywrightMcpCaller playwrightMcpCaller;

    RateLimitedPlaywrightMcpToolCallback(
            ToolCallback delegate,
            PlaywrightMcpCaller playwrightMcpCaller
    ) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate tool callback must not be null.");
        this.playwrightMcpCaller = Objects.requireNonNull(playwrightMcpCaller, "Playwright MCP tool caller must not be null.");
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
        return playwrightMcpCaller.call(delegate, input);
    }

    @Override
    public String call(String input, ToolContext toolContext) {
        return playwrightMcpCaller.call(delegate, input, toolContext);
    }
}
