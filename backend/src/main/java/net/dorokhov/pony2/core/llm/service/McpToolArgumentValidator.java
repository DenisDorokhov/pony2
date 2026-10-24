package net.dorokhov.pony2.core.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
public class McpToolArgumentValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpToolArgumentValidator.class);
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private final int maxArgumentCorrections;

    public McpToolArgumentValidator(@Value("${pony.llm.mcp.maxArgumentCorrections:3}") int maxArgumentCorrections) {
        if (maxArgumentCorrections < 0) {
            throw new IllegalArgumentException("Maximum argument corrections must not be negative.");
        }
        this.maxArgumentCorrections = maxArgumentCorrections;
    }

    /**
     * Creates a shared correction budget for all supplied MCP tools in one conversation.
     * Validation wraps the supplied callbacks so it runs before their execution and retries.
     */
    public List<ToolCallbackProvider> validate(Stream<ToolCallbackProvider> toolCallbackProviders) {
        AtomicInteger invalidArgumentCount = new AtomicInteger();
        return toolCallbackProviders
                .map(provider -> validate(provider, invalidArgumentCount))
                .toList();
    }

    private ToolCallbackProvider validate(ToolCallbackProvider provider, AtomicInteger invalidArgumentCount) {
        return () -> Arrays.stream(provider.getToolCallbacks())
                .map(callback -> new ValidatingToolCallback(callback, invalidArgumentCount, maxArgumentCorrections))
                .toArray(ToolCallback[]::new);
    }

    private static final class ValidatingToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final AtomicInteger invalidArgumentCount;
        private final int maxArgumentCorrections;

        private ValidatingToolCallback(ToolCallback delegate, AtomicInteger invalidArgumentCount, int maxArgumentCorrections) {
            this.delegate = delegate;
            this.invalidArgumentCount = invalidArgumentCount;
            this.maxArgumentCorrections = maxArgumentCorrections;
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
            return delegate.call(validateArguments(input));
        }

        private String validateArguments(String input) {
            String arguments = StringUtils.hasText(input) ? input : "{}";
            try {
                JsonNode parsed = MAPPER.readTree(arguments);
                if (!parsed.isObject()) {
                    throw new IllegalArgumentException("Tool arguments must be a JSON object.");
                }
            } catch (JacksonException | IllegalArgumentException e) {
                int correction = invalidArgumentCount.incrementAndGet();
                String toolName = getToolDefinition().name();
                if (correction > maxArgumentCorrections) {
                    throw new IllegalStateException("MCP tool argument correction limit (%d) exceeded for '%s'."
                            .formatted(maxArgumentCorrections, toolName), e);
                }
                LOGGER.warn("Invalid JSON arguments for MCP tool '{}'. Correction {} of {}.",
                        toolName, correction, maxArgumentCorrections);
                throw new ToolExecutionException(getToolDefinition(), new IllegalArgumentException(
                        ("Invalid JSON arguments for %s. The tool was not executed. "
                                + "Generate the complete arguments again as a valid JSON object. Correction %d of %d.")
                                .formatted(toolName, correction, maxArgumentCorrections), e));
            }
            return arguments;
        }

        @Override
        public String call(String input, ToolContext toolContext) {
            return delegate.call(validateArguments(input), toolContext);
        }
    }
}
