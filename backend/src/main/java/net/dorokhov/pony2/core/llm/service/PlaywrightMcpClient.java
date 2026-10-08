package net.dorokhov.pony2.core.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongUnaryOperator;

@Component
public class PlaywrightMcpClient {

    private static final String PLAYWRIGHT_TOOL_PREFIX = "browser_";

    private static final Set<String> HUMAN_ACTION_TOOL_NAMES = Set.of(
            "browser_click",
            "browser_type",
            "browser_press",
            "browser_scroll",
            "browser_select_option",
            "browser_drag"
    );

    private static final Set<String> PAGE_CHANGING_TOOL_NAMES = Set.of(
            "browser_navigate",
            "browser_click",
            "browser_type",
            "browser_press",
            "browser_scroll",
            "browser_select_option",
            "browser_drag"
    );

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Object toolCallLock = new Object();
    private final ObjectProvider<ToolCallbackProvider> toolCallbackProviders;
    private final RandomDelay humanActionDelay;
    private final RandomDelay pageChangingDelay;
    private final RateLimitedRequestExecutor.Sleeper sleeper;
    private final LongUnaryOperator randomMillisSupplier;

    @Autowired
    public PlaywrightMcpClient(
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
            @Value("${pony.llm.playwrightMcp.humanActionDelay.min:500ms}") Duration humanActionMinDelay,
            @Value("${pony.llm.playwrightMcp.humanActionDelay.max:2500ms}") Duration humanActionMaxDelay,
            @Value("${pony.llm.playwrightMcp.pageChangingDelay.min:2s}") Duration pageChangingMinDelay,
            @Value("${pony.llm.playwrightMcp.pageChangingDelay.max:6s}") Duration pageChangingMaxDelay
    ) {
        this(
                toolCallbackProviders,
                new RandomDelay(humanActionMinDelay, humanActionMaxDelay),
                new RandomDelay(pageChangingMinDelay, pageChangingMaxDelay),
                Thread::sleep,
                bound -> ThreadLocalRandom.current().nextLong(bound)
        );
    }

    PlaywrightMcpClient(
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
            RandomDelay humanActionDelay,
            RandomDelay pageChangingDelay,
            RateLimitedRequestExecutor.Sleeper sleeper,
            LongUnaryOperator randomMillisSupplier
    ) {
        this.toolCallbackProviders = toolCallbackProviders;
        this.humanActionDelay = humanActionDelay;
        this.pageChangingDelay = pageChangingDelay;
        this.sleeper = sleeper;
        this.randomMillisSupplier = randomMillisSupplier;
    }

    public String call(String toolName, String input) {
        return call(findPlaywrightTool(toolName), input, null);
    }

    public String call(ToolCallback toolCallback, String input, ToolContext toolContext) {
        validatePlaywrightToolName(toolCallback.getToolDefinition().name());
        synchronized (toolCallLock) {
            return invoke(toolCallback, input, toolContext);
        }
    }

    public List<String> call(List<ToolCall> toolCalls) {
        synchronized (toolCallLock) {
            return toolCalls.stream()
                    .map(toolCall -> invoke(findPlaywrightTool(toolCall.name()), toolCall.input(), null))
                    .toList();
        }
    }

    public static boolean isPlaywrightToolName(String toolName) {
        return toolName != null && toolName.startsWith(PLAYWRIGHT_TOOL_PREFIX);
    }

    private ToolCallback findPlaywrightTool(String toolName) {
        validatePlaywrightToolName(toolName);
        return toolCallbackProviders.orderedStream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .filter(callback -> callback.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Playwright MCP tool is not available: " + toolName));
    }

    private void validatePlaywrightToolName(String toolName) {
        if (!isPlaywrightToolName(toolName)) {
            throw new IllegalArgumentException("Only Playwright MCP tools can be called: " + toolName);
        }
    }

    private String invoke(ToolCallback toolCallback, String input, ToolContext toolContext) {
        String toolName = toolCallback.getToolDefinition().name();
        logger.debug("Calling proxied Playwright MCP tool '{}': {}.", toolName, input);
        waitBeforeCallIfNeeded(toolName);
        String result = toolContext == null ? toolCallback.call(input) : toolCallback.call(input, toolContext);
        waitAfterCallIfNeeded(toolName);
        logger.debug("Finished proxied Playwright MCP tool '{}': {}", toolName,
                result.length() > 500 ? result.substring(0, 500) + "..." : result + ".");
        return result;
    }

    private void waitBeforeCallIfNeeded(String toolName) {
        if (HUMAN_ACTION_TOOL_NAMES.contains(toolName)) {
            waitRandomDelay("human action", toolName, humanActionDelay);
        }
    }

    private void waitAfterCallIfNeeded(String toolName) {
        if (PAGE_CHANGING_TOOL_NAMES.contains(toolName)) {
            waitRandomDelay("page changing", toolName, pageChangingDelay);
        }
    }

    private void waitRandomDelay(String delayName, String toolName, RandomDelay randomDelay) {
        Duration delay = randomDelay.next(randomMillisSupplier);
        if (delay.isZero()) {
            return;
        }
        logger.debug("Waiting {} {} delay for Playwright MCP tool '{}'.", delay, delayName, toolName);
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Playwright MCP tool call was interrupted.", e);
        }
    }

    public record ToolCall(String name, String input) {
    }

    record RandomDelay(Duration min, Duration max) {

        RandomDelay {
            if (min.isNegative()) {
                throw new IllegalArgumentException("Min delay must not be negative.");
            }
            if (max.compareTo(min) < 0) {
                throw new IllegalArgumentException("Max delay must not be less than min delay.");
            }
        }

        Duration next(LongUnaryOperator randomMillisSupplier) {
            long minMillis = min.toMillis();
            long maxMillis = max.toMillis();
            if (maxMillis == minMillis) {
                return Duration.ofMillis(minMillis);
            }
            return Duration.ofMillis(minMillis + randomMillisSupplier.applyAsLong(maxMillis - minMillis + 1));
        }
    }
}
