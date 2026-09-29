package net.dorokhov.pony2.core.llm;

import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.core.llm.service.FetchUrlTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

@Configuration
public class LlmConfig {

    @Bean
    public ChatClient llmChatClient(
            ChatModel chatModel,
            ObservationRegistry observationRegistry,
            FetchUrlTool fetchUrlTool,
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders
    ) {
        Object[] tools = Stream.concat(
                Stream.of(fetchUrlTool),
                toolCallbackProviders.orderedStream()
        ).toArray();
        return ChatClient.builder(chatModel, observationRegistry, null, null)
                .defaultTools(tools)
                .build();
    }
}
