package net.dorokhov.pony2.core.llm.service;

import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class ChatModelImpl implements ChatModel {

    private final ConfigService configService;
    private final ObservationRegistry observationRegistry;
    private final Duration timeout;
    private final @Nullable Double temperature;
    private final @Nullable Double topP;
    private final @Nullable Integer topK;
    private final @Nullable Double minP;
    private final @Nullable Double presencePenalty;
    private final @Nullable Double repeatPenalty;

    public ChatModelImpl(
            ConfigService configService,
            ObservationRegistry observationRegistry,
            @Value("${pony.llm.timeout:10m}") Duration timeout,
            @Value("${pony.llm.temperature:#{null}}") @Nullable Double temperature,
            @Value("${pony.llm.topP:#{null}}") @Nullable Double topP,
            @Value("${pony.llm.topK:#{null}}") @Nullable Integer topK,
            @Value("${pony.llm.minP:#{null}}") @Nullable Double minP,
            @Value("${pony.llm.presencePenalty:#{null}}") @Nullable Double presencePenalty,
            @Value("${pony.llm.repeatPenalty:#{null}}") @Nullable Double repeatPenalty
    ) {
        this.configService = configService;
        this.observationRegistry = observationRegistry;
        this.timeout = timeout;
        this.temperature = temperature;
        this.topP = topP;
        this.topK = topK;
        this.minP = minP;
        this.presencePenalty = presencePenalty;
        this.repeatPenalty = repeatPenalty;
    }

    @Override
    public @NonNull ChatResponse call(@NonNull Prompt prompt) {
        return createChatModel(builder -> {}).call(prompt);
    }

    @Override
    public @NonNull ChatOptions getOptions() {
        return createOptions(configService.get());
    }

    public ChatModel createChatModel(OpenAiHttpClientBuilderCustomizer customizer) {
        return OpenAiChatModel.builder()
                .options(createOptions(configService.get()))
                .observationRegistry(observationRegistry)
                .httpClientBuilderCustomizer(customizer)
                .build();
    }

    private OpenAiChatOptions createOptions(ConfigSet configSet) {
        if (!configSet.discoveryEnabled()) {
            throw new IllegalStateException("LLM is not configured");
        }
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .baseUrl(configSet.llmUrl())
                .model(configSet.llmModel())
                .timeout(timeout)
                .temperature(temperature)
                .topP(topP)
                .presencePenalty(presencePenalty);
        Map<String, Object> extraBody = new HashMap<>();
        if (topK != null) {
            extraBody.put("top_k", topK);
        }
        if (minP != null) {
            extraBody.put("min_p", minP);
        }
        if (repeatPenalty != null) {
            extraBody.put("repeat_penalty", repeatPenalty);
        }
        if (!extraBody.isEmpty()) {
            builder.extraBody(extraBody);
        }
        if (configSet.llmApiKey() != null) {
            builder.apiKey(configSet.llmApiKey());
        } else {
            builder.apiKey(new NoopApiKey());
        }
        return builder.build();
    }
}
