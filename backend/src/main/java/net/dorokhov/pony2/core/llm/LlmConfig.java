package net.dorokhov.pony2.core.llm;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean
    public ChatClient llmChatClient(AiChatModel aiChatModel, ObservationRegistry observationRegistry) {
        return ChatClient.builder(aiChatModel, observationRegistry, null, null).build();
    }
}
