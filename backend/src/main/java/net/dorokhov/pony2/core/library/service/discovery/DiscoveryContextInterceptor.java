package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.service.exception.LlmContextLimitException;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;

public class DiscoveryContextInterceptor implements Interceptor {

    private static final MediaType JSON = MediaType.get("application/json");

    private final DiscoveryContext context;

    public DiscoveryContextInterceptor(DiscoveryContext context) {
        this.context = context;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        if (!request.url().encodedPath().endsWith("/chat/completions") || request.body() == null) {
            return chain.proceed(request);
        }
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        ObjectNode body = (ObjectNode) JsonConverter.fromJson(buffer.readUtf8(), JsonNode.class);
        String model = body.path("model").asText();
        if (context.getContextSize() == 0) {
            HttpUrl propertiesUrl = endpoint(request, "/props").newBuilder().addQueryParameter("model", model).build();
            context.setContextSize(exchange(chain, request.newBuilder().url(propertiesUrl).get().build())
                    .path("default_generation_settings").path("n_ctx").asInt());
            if (context.getContextSize() <= 0) {
                throw new LlmContextLimitException("LLM server did not report a positive per-request context size for model '" + model + "'.");
            }
        }
        int inputTokens = inputTokens(chain, request, body, model);
        int remaining = (int) (context.getContextSize() * context.getLimitRatio()) - inputTokens;
        if (remaining <= 0) {
            throw new LlmContextLimitException("LLM prompt reached the context limit: " + inputTokens + "/" + context.getContextSize() + " tokens.");
        }
        if (context.isVerifyCompactionTarget() && !context.isSummarizing()) {
            context.setVerifyCompactionTarget(false);
            if (inputTokens > (int) (context.getContextSize() * context.getTargetRatio())) {
                if (context.getCompactionFragmentBudget() > 1) {
                    context.setCompactionFragmentBudget(context.getCompactionFragmentBudget() / 2);
                    throw new CompactionRequired(inputTokens);
                }
                throw new LlmContextLimitException("Compacted prompt exceeds the target context size: " + inputTokens + " tokens.");
            }
        } else if (!context.isSummarizing() && inputTokens >= (int) (context.getContextSize() * context.getTriggerRatio()) && hasToolHistory(body)) {
            throw new CompactionRequired(inputTokens);
        }
        int existingLimit = body.hasNonNull("max_completion_tokens") ? body.path("max_completion_tokens").asInt()
                : body.path("max_tokens").asInt();
        context.setContextLimitApplied(existingLimit <= 0 || existingLimit >= remaining);
        if (existingLimit > 0) {
            remaining = Math.min(remaining, existingLimit);
        }
        body.remove("max_completion_tokens");
        body.remove("n_predict");
        body.put("max_tokens", remaining);
        return chain.proceed(request.newBuilder().post(RequestBody.create(body.toString(), JSON)).build());
    }

    private int inputTokens(Interceptor.Chain chain, Request request, ObjectNode body, String model) throws IOException {
        for (JsonNode message : body.path("messages")) {
            for (JsonNode part : message.path("content")) {
                if (part.isObject() && !"text".equals(part.path("type").asText())) {
                    // A text tokenizer cannot account for image embeddings. Use a bounded one-token media preflight.
                    ObjectNode preflight = body.deepCopy();
                    preflight.remove("max_completion_tokens");
                    preflight.remove("n_predict");
                    preflight.put("max_tokens", 1);
                    preflight.put("stream", false);
                    JsonNode usage = exchange(chain, request.newBuilder()
                            .post(RequestBody.create(preflight.toString(), JSON)).build()).path("usage");
                    int count = usage.path("prompt_tokens").asInt();
                    if (count <= 0) {
                        throw new LlmContextLimitException("LLM server did not report multimodal prompt usage.");
                    }
                    return count;
                }
            }
        }
        String template = exchange(chain, jsonRequest(request, "/apply-template", body)).path("prompt").asText();
        if (template.isEmpty()) {
            throw new LlmContextLimitException("LLM server returned an empty chat template.");
        }
        JsonNode tokens = exchange(chain, jsonRequest(request, "/tokenize", Map.of(
                "model", model, "content", template, "add_special", true, "parse_special", true))).path("tokens");
        if (!tokens.isArray()) {
            throw new LlmContextLimitException("LLM server did not return prompt tokens.");
        }
        return tokens.size();
    }

    private HttpUrl endpoint(Request request, String suffix) {
        String path = request.url().encodedPath();
        String prefix = path.substring(0, path.length() - "/chat/completions".length());
        if (prefix.endsWith("/v1")) {
            prefix = prefix.substring(0, prefix.length() - 3);
        }
        return request.url().newBuilder().encodedPath(prefix + suffix).query(null).build();
    }

    private JsonNode exchange(Interceptor.Chain chain, Request request) throws IOException {
        try (Response response = chain.proceed(request)) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new LlmContextLimitException("LLM context accounting failed at " + request.url().encodedPath()
                        + " with HTTP " + response.code() + ": " + (body != null ? body.string() : "empty response"));
            }
            return JsonConverter.fromJson(body.string(), JsonNode.class);
        }
    }

    private Request jsonRequest(Request original, String suffix, Object body) {
        return original.newBuilder().url(endpoint(original, suffix))
                .post(RequestBody.create(JsonConverter.toJson(body), JSON)).build();
    }

    private boolean hasToolHistory(ObjectNode body) {
        for (JsonNode message : body.path("messages")) {
            if ("tool".equals(message.path("role").asText())) {
                return true;
            }
        }
        return false;
    }

    public static void checkFinishReason(ChatResponse response) {
        for (Generation generation : response.getResults()) {
            if ("length".equalsIgnoreCase(generation.getMetadata().getFinishReason())) {
                throw new LlmContextLimitException("LLM generation reached its token limit before completing the response.");
            }
        }
    }

    public void checkUsage(ChatResponse response) {
        Integer promptTokens = response.getMetadata().getUsage().getPromptTokens();
        Integer completionTokens = response.getMetadata().getUsage().getCompletionTokens();
        if (context.getContextSize() > 0 && promptTokens != null && completionTokens != null &&
                (long) promptTokens + completionTokens >= (int) (context.getContextSize() * context.getLimitRatio())) {
            throw new LlmContextLimitException("LLM response reached the context limit: "
                    + ((long) promptTokens + completionTokens) + "/" + context.getContextSize() + " tokens.");
        }
    }

    public void checkResponse(ChatResponse response) {
        if (context.isContextLimitApplied()) {
            checkFinishReason(response);
        }
        checkUsage(response);
    }

    public static class CompactionRequired extends RuntimeException {

        private final int inputTokens;

        private CompactionRequired(int inputTokens) {
            super("Context compaction required at " + inputTokens + " tokens.");
            this.inputTokens = inputTokens;
        }

        public int inputTokens() {
            return inputTokens;
        }
    }
}
