package net.dorokhov.pony2.core.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimitedPlaywrightMcpClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PlaywrightMcpClient playwrightMcpClient;
    private final RateLimitedRequestExecutor rateLimitedRequestExecutor;

    @Autowired
    public RateLimitedPlaywrightMcpClient(
            PlaywrightMcpClient playwrightMcpClient,
            @Value("${pony.llm.playwrightMcp.rateLimit.notMoreOftenThan:2s}") Duration requestInterval,
            @Value("${pony.llm.playwrightMcp.rateLimit.randomDelay:4s}") Duration requestRandomDelay,
            @Value("${pony.llm.playwrightMcp.rateLimit.retriesOnException:1}") int retriesOnException
    ) {
        this(
                playwrightMcpClient,
                new RateLimitedRequestExecutor(
                        "playwright-mcp",
                        new RateLimitedRequestExecutor.Settings(
                                requestInterval,
                                requestRandomDelay,
                                retriesOnException
                        )
                )
        );
    }

    RateLimitedPlaywrightMcpClient(
            PlaywrightMcpClient playwrightMcpClient,
            RateLimitedRequestExecutor rateLimitedRequestExecutor
    ) {
        this.playwrightMcpClient = playwrightMcpClient;
        this.rateLimitedRequestExecutor = rateLimitedRequestExecutor;
    }

    public String call(String toolName, String input) {
        if (!isPlaywrightToolName(toolName)) {
            throw new IllegalArgumentException("Only Playwright MCP tools can be called: " + toolName);
        }
        return rateLimitedRequestExecutor.execute(() -> {
            logger.trace("Calling rate-limited Playwright MCP tool '{}'.", toolName);
            return playwrightMcpClient.call(toolName, input);
        });
    }

    public String call(ToolCallback toolCallback, String input) {
        return call(toolCallback, input, null);
    }

    public String call(ToolCallback toolCallback, String input, ToolContext toolContext) {
        String toolName = toolCallback.getToolDefinition().name();
        if (!isPlaywrightToolName(toolName)) {
            throw new IllegalArgumentException("Only Playwright MCP tools can be called: " + toolName);
        }
        return rateLimitedRequestExecutor.execute(() -> {
            logger.trace("Calling rate-limited Playwright MCP tool '{}'.", toolName);
            return playwrightMcpClient.call(toolCallback, input, toolContext);
        });
    }

    public static boolean isPlaywrightToolName(String toolName) {
        return PlaywrightMcpClient.isPlaywrightToolName(toolName);
    }
}
