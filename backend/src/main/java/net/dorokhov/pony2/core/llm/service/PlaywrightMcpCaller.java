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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongUnaryOperator;

@Component
public class PlaywrightMcpCaller {

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

    private final ObjectProvider<ToolCallbackProvider> toolCallbackProviders;
    private final RateLimitedRequestExecutor rateLimitedRequestExecutor;
    private final RandomDelay humanActionDelay;
    private final RandomDelay pageChangingDelay;
    private final RateLimitedRequestExecutor.Sleeper sleeper;
    private final LongUnaryOperator randomMillisSupplier;

    @Autowired
    public PlaywrightMcpCaller(
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
            @Value("${pony.llm.playwrightMcp.rateLimit.notMoreOftenThan:2s}") Duration requestInterval,
            @Value("${pony.llm.playwrightMcp.rateLimit.randomDelay:4s}") Duration requestRandomDelay,
            @Value("${pony.llm.playwrightMcp.rateLimit.retriesOnException:1}") int retriesOnException,
            @Value("${pony.llm.playwrightMcp.humanActionDelay.min:500ms}") Duration humanActionMinDelay,
            @Value("${pony.llm.playwrightMcp.humanActionDelay.max:2500ms}") Duration humanActionMaxDelay,
            @Value("${pony.llm.playwrightMcp.pageChangingDelay.min:2s}") Duration pageChangingMinDelay,
            @Value("${pony.llm.playwrightMcp.pageChangingDelay.max:6s}") Duration pageChangingMaxDelay
    ) {
        this(
                toolCallbackProviders,
                new RateLimitedRequestExecutor(
                        "playwright-mcp",
                        new RateLimitedRequestExecutor.Settings(
                                requestInterval,
                                requestRandomDelay,
                                retriesOnException
                        )
                ),
                new RandomDelay(humanActionMinDelay, humanActionMaxDelay),
                new RandomDelay(pageChangingMinDelay, pageChangingMaxDelay),
                Thread::sleep,
                bound -> ThreadLocalRandom.current().nextLong(bound)
        );
    }

    PlaywrightMcpCaller(
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
            RateLimitedRequestExecutor rateLimitedRequestExecutor,
            RandomDelay humanActionDelay,
            RandomDelay pageChangingDelay,
            RateLimitedRequestExecutor.Sleeper sleeper,
            LongUnaryOperator randomMillisSupplier
    ) {
        this.toolCallbackProviders = Objects.requireNonNull(toolCallbackProviders, "Tool callback providers must not be null.");
        this.rateLimitedRequestExecutor = Objects.requireNonNull(rateLimitedRequestExecutor, "Rate-limited request executor must not be null.");
        this.humanActionDelay = Objects.requireNonNull(humanActionDelay, "Human action delay must not be null.");
        this.pageChangingDelay = Objects.requireNonNull(pageChangingDelay, "Page changing delay must not be null.");
        this.sleeper = Objects.requireNonNull(sleeper, "Sleeper must not be null.");
        this.randomMillisSupplier = Objects.requireNonNull(randomMillisSupplier, "Random millis supplier must not be null.");
    }

    public String call(String toolName, String input) {
        return call(findPlaywrightTool(toolName), input);
    }

    public String call(ToolCallback toolCallback, String input) {
        return call(toolCallback, input, null);
    }

    public String call(ToolCallback toolCallback, String input, ToolContext toolContext) {
        Objects.requireNonNull(toolCallback, "Tool callback must not be null.");
        String toolName = toolCallback.getToolDefinition().name();
        if (!isPlaywrightToolName(toolName)) {
            throw new IllegalArgumentException("Only Playwright MCP tools can be called: " + toolName);
        }

        return rateLimitedRequestExecutor.execute(() -> {
            logger.debug("Calling proxied Playwright MCP tool '{}'. Input length: {}.", toolName, inputLength(input));
            waitBeforeCallIfNeeded(toolName);
            String result = toolContext == null ? toolCallback.call(input) : toolCallback.call(input, toolContext);
            waitAfterCallIfNeeded(toolName);
            logger.debug("Finished proxied Playwright MCP tool '{}'. Result length: {}.", toolName, inputLength(result));
            return result;
        });
    }

    public static boolean isPlaywrightToolName(String toolName) {
        return toolName != null && toolName.startsWith(PLAYWRIGHT_TOOL_PREFIX);
    }

    private ToolCallback findPlaywrightTool(String toolName) {
        if (!isPlaywrightToolName(toolName)) {
            throw new IllegalArgumentException("Only Playwright MCP tools can be called: " + toolName);
        }
        return toolCallbackProviders.orderedStream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .filter(callback -> callback.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Playwright MCP tool is not available: " + toolName));
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

    private int inputLength(String input) {
        return input == null ? 0 : input.length();
    }

    record RandomDelay(Duration min, Duration max) {

        RandomDelay {
            Objects.requireNonNull(min, "Min delay must not be null.");
            Objects.requireNonNull(max, "Max delay must not be null.");
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
