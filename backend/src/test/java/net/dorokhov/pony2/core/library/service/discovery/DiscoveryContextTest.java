package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.llm.service.LlmContextServiceImpl;
import net.dorokhov.pony2.core.llm.service.LlmContextService;
import net.dorokhov.pony2.core.llm.service.exception.LlmContextLimitException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveryContextTest {

    private final AtomicInteger tokenCount = new AtomicInteger(100);
    private final List<Request> requests = new ArrayList<>();
    private final DiscoveryContextBudgetManager budgetManager = new DiscoveryContextBudgetManager();
    private final TestContext context = new TestContext();

    @Test
    void shouldAllowOnlyOneMoreTokenBelowThe98PercentCeiling() throws Exception {
        tokenCount.set(979);
        DiscoveryContextInterceptor interceptor = new DiscoveryContextInterceptor(new LlmContextServiceImpl(), budgetManager,
                new DiscoveryContextBudget(.98, .7), true);
        try (Response ignored = interceptor.intercept(chain("{\"model\":\"test\",\"messages\":[]}"))) {
            assertThat(body(requests.getLast()).path("max_tokens").asInt()).isEqualTo(1);
        }
    }

    @Test
    void shouldRejectTheFirstRequestAt98PercentWithoutGeneration() throws Exception {
        tokenCount.set(980);
        DiscoveryContextInterceptor interceptor = new DiscoveryContextInterceptor(new LlmContextServiceImpl(), budgetManager,
                new DiscoveryContextBudget(.98, .7), true);
        Interceptor.Chain chain = chain("{\"model\":\"test\",\"messages\":[]}");
        assertThatThrownBy(() -> interceptor.intercept(chain)).isInstanceOf(LlmContextLimitException.class);
        assertThat(requests).hasSize(3).noneMatch(request -> request.url().encodedPath().endsWith("chat/completions"));
    }

    @Test
    void shouldApplyTheBudgetToARequestPreparedByAnotherProvider() throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        LlmContextService service = mock(LlmContextService.class);
        LlmContextService.PreparedRequest prepared = mock(LlmContextService.PreparedRequest.class);
        when(service.inspect(chain, 0)).thenReturn(prepared);
        when(prepared.context()).thenReturn(new LlmContextService.ContextInfo(1000, 180, 900, false));
        Request limitedRequest = new Request.Builder().url("http://localhost/v2/generate")
                .post(RequestBody.create("{\"output_limit\":800}", MediaType.get("application/json"))).build();
        when(prepared.withOutputLimit(800)).thenReturn(limitedRequest);
        DiscoveryContextInterceptor interceptor = new DiscoveryContextInterceptor(service, budgetManager,
                new DiscoveryContextBudget(.98, .7), true);
        try (Response expected = new Response.Builder().request(limitedRequest).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(ResponseBody.create("", MediaType.get("application/json"))).build()) {
            when(chain.proceed(limitedRequest)).thenReturn(expected);
            assertThat(interceptor.intercept(chain)).isSameAs(expected);
        }
    }

    @Test
    void shouldLimitTheFirstGenerationIncludingToolsAndTheServerTemplate() throws Exception {
        String payload = """
                {"model":"test","messages":[{"role":"user","content":"Find artist"}],
                 "tools":[{"type":"function","function":{"name":"browser_snapshot"}}]}
                """;
        try (Response ignored = context.intercept(chain(payload))) {
            assertThat(requests).extracting(request -> request.url().encodedPath())
                    .containsExactly("/props", "/apply-template", "/tokenize", "/v1/chat/completions");
            JsonNode templateBody = body(requests.get(1));
            assertThat(templateBody.path("tools").get(0).path("function").path("name").asText()).isEqualTo("browser_snapshot");
            assertThat(body(requests.get(2)).path("content").asText()).isEqualTo("actual server template");
            assertThat(body(requests.getLast()).path("max_tokens").asInt()).isEqualTo(800);
        }
    }

    @Test
    void shouldPreserveTheSdkPayloadExceptForTheGenerationLimit() throws Exception {
        String payload = """
                {"model":"test","max_tokens":80,"max_completion_tokens":17,"n_predict":-1,"stream":false,
                 "messages":[{"role":"system","content":"Find artist"},
                  {"role":"user","content":[{"type":"text","text":"Артист 🎵"}]},
                  {"role":"assistant","content":null,"reasoning_content":"Keep the source URL",
                   "tool_calls":[{"id":"call_1","type":"function","function":{"name":"lookup","arguments":"{}"}}]},
                  {"role":"assistant","reasoning_content":"No content field"},
                  {"role":"tool","tool_call_id":"call_1","content":"https://source.test/artist/A12?si=a%2Fb"}],
                 "tools":[{"type":"function","function":{"name":"lookup","parameters":{"type":"object","properties":{}}}}],
                 "response_format":{"type":"json_schema","json_schema":{"name":"artist","schema":{"type":"object"}}},
                 "chat_template_kwargs":{"enable_thinking":true,"extra":null},"min_p":0.05}
                """;
        try (Response ignored = context.intercept(chain(payload))) {
            assertThat(requests).extracting(request -> request.url().encodedPath())
                    .containsExactly("/props", "/apply-template", "/tokenize", "/v1/chat/completions");
            assertThat(requests.getFirst().url().queryParameter("model")).isEqualTo("test");
            assertThat(rawBody(requests.get(1))).isEqualTo(payload);
            assertThat(body(requests.get(2))).isEqualTo(JsonConverter.fromJson("""
                    {"model":"test","content":"actual server template","add_special":true,"parse_special":true}
                    """, JsonNode.class));
            String expected = payload.replace("\"max_tokens\":80,\"max_completion_tokens\":17,\"n_predict\":-1", "\"max_tokens\":17");
            assertThat(body(requests.getLast())).isEqualTo(JsonConverter.fromJson(expected, JsonNode.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"tokens\":null}", "{\"tokens\":{}}"})
    void shouldRejectMissingOrMalformedTokenListsWithoutGenerating(String response) throws IOException {
        Interceptor.Chain chain = chain("{\"model\":\"test\",\"messages\":[]}", response);
        assertThatThrownBy(() -> context.intercept(chain)).isInstanceOf(LlmContextLimitException.class);
        assertThat(requests).extracting(request -> request.url().encodedPath())
                .containsExactly("/props", "/apply-template", "/tokenize");
    }

    private Interceptor.Chain chain(String payload) throws IOException {
        return chain(payload, JsonConverter.toJson(Map.of("tokens", java.util.Collections.nCopies(tokenCount.get(), 1))));
    }

    private Interceptor.Chain chain(String payload, String tokenizeResponse) throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request original = new Request.Builder().url("http://localhost/v1/chat/completions")
                .post(RequestBody.create(payload, MediaType.get("application/json"))).build();
        when(chain.request()).thenReturn(original);
        when(chain.proceed(any())).thenAnswer(invocation -> {
            Request request = invocation.getArgument(0);
            requests.add(request);
            String result = switch (request.url().encodedPath()) {
                case "/props" -> """
                        {"default_generation_settings":{"n_ctx":1000,"temperature":0.8},"total_slots":1}
                        """;
                case "/apply-template" -> "{\"prompt\":\"actual server template\"}";
                case "/tokenize" -> tokenizeResponse;
                case "/v1/chat/completions" -> "{\"usage\":{\"prompt_tokens\":350,\"completion_tokens\":1},\"choices\":[]}";
                default -> "{}";
            };
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(ResponseBody.create(result, MediaType.get("application/json"))).build();
        });
        return chain;
    }

    @Test
    void shouldAccountForImageEmbeddingsWithABoundedSingleTokenPreflight() throws Exception {
        String payload = """
                {"model":"test","n_predict":-1,"stream":true,"messages":[{"role":"user","content":[
                 {"type":"text","text":"Find faces"},{"type":"image_url","image_url":{"url":"data:image/png;base64,test"}}]}]}
                """;
        try (Response ignored = context.intercept(chain(payload))) {
            assertThat(requests).extracting(request -> request.url().encodedPath())
                    .containsExactly("/props", "/v1/chat/completions", "/v1/chat/completions");
            assertThat(body(requests.get(1))).isEqualTo(JsonConverter.fromJson(
                    payload.replace("\"n_predict\":-1,\"stream\":true", "\"max_tokens\":1,\"stream\":false"), JsonNode.class));
            assertThat(body(requests.getLast())).isEqualTo(JsonConverter.fromJson(
                    payload.replace("\"n_predict\":-1", "\"max_tokens\":550"), JsonNode.class));
        }
    }

    private JsonNode body(Request request) throws IOException {
        return JsonConverter.fromJson(rawBody(request), JsonNode.class);
    }

    private String rawBody(Request request) throws IOException {
        Buffer buffer = new Buffer();
        java.util.Objects.requireNonNull(request.body()).writeTo(buffer);
        return buffer.readUtf8();
    }

    @Test
    void shouldPreserveAnExistingSmallerOutputLimit() throws Exception {
        try (Response ignored = context.intercept(chain("""
                {"model":"test","messages":[],"max_completion_tokens":17}
                """))) {
            JsonNode sent = body(requests.getLast());
            assertThat(sent.path("max_tokens").asInt()).isEqualTo(17);
            assertThat(sent.has("max_completion_tokens")).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {900, 950, 1000})
    void shouldRejectAnOversizedInitialPromptWithoutGenerating(int tokens) throws Exception {
        tokenCount.set(tokens);
        Interceptor.Chain chain = chain("{\"model\":\"test\",\"messages\":[]}");
        assertThatThrownBy(() -> context.intercept(chain)).isInstanceOf(LlmContextLimitException.class);
        assertThat(requests).hasSize(3);
    }

    @Test
    void shouldTriggerAtSeventyPercentOnlyForToolHistory() throws Exception {
        tokenCount.set(700);
        try (Response ignored = context.intercept(chain("{\"model\":\"test\",\"messages\":[]}"))) {
            assertThat(body(requests.getLast()).path("max_tokens").asInt()).isEqualTo(200);
        }
        assertThatThrownBy(() -> context.intercept(chain(withToolHistory())))
                .isInstanceOf(DiscoveryContextInterceptor.CompactionRequiredException.class);
    }

    private String withToolHistory() {
        return "{\"model\":\"test\",\"messages\":[{\"role\":\"tool\",\"content\":\"data\"}]}";
    }

    @ParameterizedTest
    @ValueSource(ints = {900, 950, 1000, 1100})
    void shouldRequireCompactionAtOrAboveTheContextLimitWhenToolHistoryExists(int tokens) throws Exception {
        tokenCount.set(tokens);
        assertThatThrownBy(() -> context.intercept(chain(withToolHistory())))
                .isInstanceOfSatisfying(DiscoveryContextInterceptor.CompactionRequiredException.class,
                        required -> assertThat(required.inputTokens()).isEqualTo(tokens));
        assertThat(requests).hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LENGTH", "length"})
    void shouldRejectATruncatedResponseEvenWhenItContainsValidJson(String reason) {
        assertThatThrownBy(() -> DiscoveryContextBudgetManager.checkFinishReason(response("{}", reason)))
                .isInstanceOf(LlmContextLimitException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {900, 901})
    void shouldRejectAQuotaReachingResponseEvenWithStopFinishReason(int totalTokens) throws Exception {
        try (Response ignored = context.intercept(chain("{\"model\":\"test\",\"messages\":[]}"))) {
            ChatResponse response = new ChatResponse(response("{}", "STOP").getResults(),
                    ChatResponseMetadata.builder().usage(new DefaultUsage(800, totalTokens - 800)).build());
            assertThatThrownBy(() -> context.checkUsage(response)).isInstanceOf(LlmContextLimitException.class);
        }
    }

    @Test
    void shouldRejectLengthAtTheContextCeiling() throws Exception {
        try (Response ignored = context.intercept(chain("{\"model\":\"test\",\"messages\":[]}"))) {
            assertThatThrownBy(() -> context.checkResponse(response("{}", "LENGTH")))
                    .isInstanceOf(LlmContextLimitException.class);
        }
    }

    @Test
    void shouldRejectLengthForASmallerExistingOutputLimit() throws Exception {
        try (Response ignored = context.intercept(chain("{\"model\":\"test\",\"messages\":[],\"max_tokens\":32}"))) {
            assertThatThrownBy(() -> context.checkResponse(response("{}", "LENGTH")))
                    .isInstanceOf(LlmContextLimitException.class);
            assertThat(body(requests.getLast()).path("max_tokens").asInt()).isEqualTo(32);
        }
    }

    private ChatResponse response(String text, String reason) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(reason).build())));
    }

    private static final class TestContext extends DiscoveryContext {
        private final DiscoveryContextBudgetManager budgetManager = new DiscoveryContextBudgetManager();
        private final DiscoveryContextInterceptor interceptor = new DiscoveryContextInterceptor(new LlmContextServiceImpl(),
                budgetManager, getBudget(), true);

        private TestContext() {
            super(List.of(), 0.9, 0.7);
        }

        private Response intercept(Interceptor.Chain chain) throws IOException {
            return interceptor.intercept(chain);
        }

        private void checkUsage(ChatResponse response) {
            budgetManager.checkUsage(getBudget(), response);
        }

        private void checkResponse(ChatResponse response) {
            budgetManager.checkResponse(getBudget(), response);
        }
    }
}
