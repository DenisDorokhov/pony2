package net.dorokhov.pony2.core.llm.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.core.llm.service.exception.LlmContextLimitException;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asks llama.cpp for its context size and counts the input tokens in a model request.
 * For images and other media, makes a one-token request and reads the input token count from its response.
 * Uses the same HTTP settings and cancellation handling as the original model request.
 * Preserves all other request fields when changing the output token limit.
 */
@Service
public class LlmContextServiceImpl implements LlmContextService {

    private static final String COMPLETIONS_PATH = "/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    @Override
    public @Nullable PreparedRequest inspect(Interceptor.Chain chain, int knownContextSize) throws IOException {
        Request request = chain.request();
        if (!request.url().encodedPath().endsWith(COMPLETIONS_PATH) || request.body() == null) {
            return null;
        }
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        Map<String, Object> body = MAPPER.readValue(buffer.readUtf8(), new TypeReference<>() {});
        int contextSize = knownContextSize == 0 ? resolveContextSize(chain, request, (String) body.get("model")) : knownContextSize;
        Number requestedLimit = (Number) body.get("max_completion_tokens");
        if (requestedLimit == null) {
            requestedLimit = (Number) body.get("max_tokens");
        }
        List<?> messages = (List<?>) body.get("messages");
        ContextInfo context = new ContextInfo(contextSize, countInputTokens(chain, request, body),
                requestedLimit != null ? requestedLimit.intValue() : 0,
                messages.stream().anyMatch(message -> "tool".equals(((Map<?, ?>) message).get("role"))));
        return new PreparedRequestImpl(context, request, body);
    }

    private int resolveContextSize(Interceptor.Chain chain, Request request, String model) throws IOException {
        HttpUrl url = endpoint(request, "/props").newBuilder().addQueryParameter("model", model).build();
        PropertiesResponse properties = exchange(chain, request.newBuilder().url(url).get().build(), PropertiesResponse.class);
        GenerationSettings settings = properties != null ? properties.defaultGenerationSettings() : null;
        Integer contextSize = settings != null ? settings.nCtx() : null;
        if (contextSize == null || contextSize <= 0) {
            throw new LlmContextLimitException("LLM server did not report a positive per-request context size for model '" + model + "'.");
        }
        return contextSize;
    }

    private int countInputTokens(Interceptor.Chain chain, Request request, Map<String, Object> body) throws IOException {
        List<?> messages = (List<?>) body.get("messages");
        boolean hasMedia = messages.stream().map(message -> ((Map<?, ?>) message).get("content"))
                .anyMatch(content -> content instanceof List<?> parts &&
                        parts.stream().anyMatch(part -> !"text".equals(((Map<?, ?>) part).get("type"))));
        if (hasMedia) {
            // A text tokenizer cannot count image tokens. Ask for one output token so the server reports the full input usage.
            Map<String, Object> preflight = new LinkedHashMap<>(body);
            preflight.put("max_tokens", 1);
            preflight.remove("max_completion_tokens");
            preflight.remove("n_predict");
            preflight.put("stream", false);
            MediaPreflightResponse response = exchange(chain, jsonRequest(request, request.url(), preflight),
                    MediaPreflightResponse.class);
            Usage usage = response != null ? response.usage() : null;
            Integer promptTokens = usage != null ? usage.promptTokens() : null;
            if (promptTokens == null || promptTokens <= 0) {
                throw new LlmContextLimitException("LLM server did not report multimodal prompt usage.");
            }
            return promptTokens;
        }
        TemplateResponse template = exchange(chain, request.newBuilder().url(endpoint(request, "/apply-template")).build(),
                TemplateResponse.class);
        if (template == null || template.prompt() == null || template.prompt().isEmpty()) {
            throw new LlmContextLimitException("LLM server returned an empty chat template.");
        }
        TokenizeResponse response = exchange(chain,
                jsonRequest(request, endpoint(request, "/tokenize"), new TokenizeRequest((String) body.get("model"), template.prompt(), true, true)),
                TokenizeResponse.class);
        if (response == null || response.tokens() == null) {
            throw new LlmContextLimitException("LLM server did not return prompt tokens.");
        }
        return response.tokens().size();
    }

    private HttpUrl endpoint(Request request, String suffix) {
        String path = request.url().encodedPath();
        String prefix = path.substring(0, path.length() - COMPLETIONS_PATH.length());
        if (prefix.endsWith("/v1")) {
            prefix = prefix.substring(0, prefix.length() - 3);
        }
        return request.url().newBuilder().encodedPath(prefix + suffix).query(null).build();
    }

    private <T> @Nullable T exchange(Interceptor.Chain chain, Request request, Class<T> responseType) throws IOException {
        try (Response response = chain.proceed(request)) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new LlmContextLimitException("LLM context accounting failed at " + request.url().encodedPath()
                        + " with HTTP " + response.code() + ": " + (body != null ? body.string() : "empty response"));
            }
            return MAPPER.readValue(body.string(), responseType);
        } catch (MismatchedInputException e) {
            throw new LlmContextLimitException("LLM context accounting returned an invalid response at " + request.url().encodedPath() + ".");
        }
    }

    private static Request jsonRequest(Request original, HttpUrl url, Object body) {
        return original.newBuilder().url(url)
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON)).build();
    }

    private record PreparedRequestImpl(ContextInfo context, Request request, Map<String, Object> body)
            implements PreparedRequest {

        @Override
        public Request withOutputLimit(int maxTokens) {
            Map<String, Object> limitedBody = new LinkedHashMap<>(body);
            limitedBody.put("max_tokens", maxTokens);
            limitedBody.remove("max_completion_tokens");
            limitedBody.remove("n_predict");
            return jsonRequest(request, request.url(), limitedBody);
        }
    }

    private record PropertiesResponse(@Nullable GenerationSettings defaultGenerationSettings) {}

    private record GenerationSettings(@Nullable Integer nCtx) {}

    private record TemplateResponse(@Nullable String prompt) {}

    private record TokenizeRequest(String model, String content, boolean addSpecial, boolean parseSpecial) {}

    private record TokenizeResponse(@Nullable List<Integer> tokens) {}

    private record MediaPreflightResponse(@Nullable Usage usage) {}

    private record Usage(@Nullable Integer promptTokens) {}
}
