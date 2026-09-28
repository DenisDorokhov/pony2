package net.dorokhov.pony2.core.llm;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

@Configuration
public class LlmConfig {

    @Bean
    public ChatClient llmChatClient(
            AiChatModel aiChatModel,
            ObservationRegistry observationRegistry,
            FetchUrlTool fetchUrlTool,
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders
    ) {
        Object[] tools = Stream.concat(
                Stream.of(fetchUrlTool),
                toolCallbackProviders.orderedStream()
        ).toArray();
        return ChatClient.builder(aiChatModel, observationRegistry, null, null)
                .defaultTools(tools)
                .build();
    }
}
