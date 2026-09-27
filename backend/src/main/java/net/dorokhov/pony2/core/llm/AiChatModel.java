package net.dorokhov.pony2.core.llm;

import io.micrometer.observation.ObservationRegistry;
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

import java.util.Objects;

@Component
public class AiChatModel implements ChatModel {

    private final ConfigService configService;
    private final ObservationRegistry observationRegistry;

    private String cachedUrl;
    private String cachedApiKey;
    private OpenAiChatModel openAiChatModel;

    public AiChatModel(
            ConfigService configService,
            ObservationRegistry observationRegistry
    ) {
        this.configService = configService;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public @NonNull ChatResponse call(@NonNull Prompt prompt) {
        return getOpenAiChatModel().call(prompt);
    }

    @Override
    public @NonNull ChatOptions getOptions() {
        return configService.getLlmUrl()
                .map(llmUrl -> createOptions(llmUrl, configService.getLlmApiKey().orElse(null)))
                .orElseGet(() -> OpenAiChatOptions.builder()
                        .apiKey(new NoopApiKey())
                        .build());
    }

    private synchronized OpenAiChatModel getOpenAiChatModel() {
        String llmUrl = configService.getLlmUrl()
                .orElseThrow(() -> new IllegalStateException("LLM URL is not configured"));
        String llmApiKey = configService.getLlmApiKey().orElse(null);
        if (!Objects.equals(llmUrl, cachedUrl) || !Objects.equals(llmApiKey, cachedApiKey)) {
            OpenAiChatModel newOpenAiChatModel = createChatModel(llmUrl, llmApiKey);
            cachedUrl = llmUrl;
            cachedApiKey = llmApiKey;
            openAiChatModel = newOpenAiChatModel;
        }
        return openAiChatModel;
    }

    private OpenAiChatModel createChatModel(String llmUrl, String llmApiKey) {
        return OpenAiChatModel.builder()
                .options(createOptions(llmUrl, llmApiKey))
                .observationRegistry(observationRegistry)
                .build();
    }

    private OpenAiChatOptions createOptions(String llmUrl, String llmApiKey) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .baseUrl(llmUrl);
        if (llmApiKey != null) {
            builder.apiKey(llmApiKey);
        } else {
            builder.apiKey(new NoopApiKey());
        }
        return builder.build();
    }
}
