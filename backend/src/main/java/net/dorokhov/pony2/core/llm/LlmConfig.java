package net.dorokhov.pony2.core.llm;

import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.core.llm.service.ChatModelImpl;
import net.dorokhov.pony2.core.llm.service.FetchUrlTool;
import net.dorokhov.pony2.core.llm.service.PlaywrightMcpToolRateLimiter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Configuration
public class LlmConfig {

    private static final Set<String> EXCLUDED_TOOLS = Set.of(
            "browser_take_screenshot"
    );

    @Bean
    public BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> llmChatClientFactory(
            ChatModelImpl chatModel,
            ObservationRegistry observationRegistry,
            FetchUrlTool fetchUrlTool,
            PlaywrightMcpToolRateLimiter playwrightMcpToolRateLimiter,
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders
    ) {
        Object[] tools = Stream.concat(
                Stream.of(fetchUrlTool),
                playwrightMcpToolRateLimiter.rateLimit(toolCallbackProviders.orderedStream()
                        .map(this::excludeTools)).stream()
        ).toArray();
        return (customizer, toolsEnabled) -> {
            ChatClient.Builder builder = ChatClient.builder(chatModel.createChatModel(customizer), observationRegistry, null, null);
            if (toolsEnabled) {
                builder.defaultTools(tools);
            }
            return builder.build();
        };
    }

    private ToolCallbackProvider excludeTools(ToolCallbackProvider provider) {
        return () -> Arrays.stream(provider.getToolCallbacks())
                .filter(callback -> !EXCLUDED_TOOLS.contains(callback.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
    }
}
