package net.dorokhov.pony2.core.llm.service;

import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ChatModelImpl implements ChatModel {

    private final ConfigService configService;
    private final ObservationRegistry observationRegistry;

    public ChatModelImpl(
            ConfigService configService,
            ObservationRegistry observationRegistry
    ) {
        this.configService = configService;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public @NonNull ChatResponse call(@NonNull Prompt prompt) {
        ConfigSet configSet = configService.get();
        return createChatModel(configSet).call(prompt);
    }

    @Override
    public @NonNull ChatOptions getOptions() {
        return createOptions(configService.get());
    }

    private OpenAiChatModel createChatModel(ConfigSet configSet) {
        return OpenAiChatModel.builder()
                .options(createOptions(configSet))
                .observationRegistry(observationRegistry)
                .build();
    }

    private OpenAiChatOptions createOptions(ConfigSet configSet) {
        if (!configSet.llmEnabled()) {
            throw new IllegalStateException("LLM is not configured");
        }
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .baseUrl(configSet.llmUrl())
                .model(configSet.llmModel())
                .timeout(Duration.ofMinutes(5));
        if (configSet.llmApiKey() != null) {
            builder.apiKey(configSet.llmApiKey());
        } else {
            builder.apiKey(new NoopApiKey());
        }
        return builder.build();
    }
}
