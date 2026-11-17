package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.service.exception.LlmContextLimitException;
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
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveryContextTest {

    private final AtomicInteger tokenCount = new AtomicInteger(100);
    private final List<Request> requests = new ArrayList<>();
    private final AtomicReference<Prompt> summaryInput = new AtomicReference<>();
    private final TestContext context = new TestContext(0.9, 0.7, 0.3, prompt -> {
        summaryInput.set(prompt);
        return response("{\"keep\":[0]}", "STOP");
    });

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

    private Interceptor.Chain chain(String payload) throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request original = new Request.Builder().url("http://localhost/v1/chat/completions")
                .post(RequestBody.create(payload, MediaType.get("application/json"))).build();
        when(chain.request()).thenReturn(original);
        when(chain.proceed(any())).thenAnswer(invocation -> {
            Request request = invocation.getArgument(0);
            requests.add(request);
            Object result = switch (request.url().encodedPath()) {
                case "/props" -> Map.of("default_generation_settings", Map.of("n_ctx", 1000));
                case "/apply-template" -> Map.of("prompt", "actual server template");
                case "/tokenize" -> Map.of("tokens", java.util.Collections.nCopies(tokenCount.get(), 1));
                case "/v1/chat/completions" -> Map.of("usage", Map.of("prompt_tokens", 350));
                default -> Map.of();
            };
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(ResponseBody.create(JsonConverter.toJson(result), MediaType.get("application/json"))).build();
        });
        return chain;
    }

    @Test
    void shouldAccountForImageEmbeddingsWithABoundedSingleTokenPreflight() throws Exception {
        try (Response ignored = context.intercept(chain("""
                {"model":"test","n_predict":-1,"messages":[{"role":"user","content":[
                 {"type":"text","text":"Find faces"},{"type":"image_url","image_url":{"url":"data:image/png;base64,test"}}]}]}
                """))) {
            assertThat(requests).extracting(request -> request.url().encodedPath())
                    .containsExactly("/props", "/v1/chat/completions", "/v1/chat/completions");
            assertThat(body(requests.get(1)).path("max_tokens").asInt()).isEqualTo(1);
            assertThat(body(requests.getLast()).path("max_tokens").asInt()).isEqualTo(550);
            assertThat(body(requests.getLast()).has("n_predict")).isFalse();
        }
    }

    private JsonNode body(Request request) throws IOException {
        Buffer buffer = new Buffer();
        java.util.Objects.requireNonNull(request.body()).writeTo(buffer);
        return JsonConverter.fromJson(buffer.readUtf8(), JsonNode.class);
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
                .isInstanceOf(DiscoveryContextInterceptor.CompactionRequired.class);
    }

    private String withToolHistory() {
        return "{\"model\":\"test\",\"messages\":[{\"role\":\"tool\",\"content\":\"data\"}]}";
    }

    @Test
    void shouldFailImmediatelyAtNinetyPercentEvenWhenToolHistoryCouldBeCompacted() throws Exception {
        tokenCount.set(900);
        assertThatThrownBy(() -> context.intercept(chain(withToolHistory())))
                .isInstanceOf(LlmContextLimitException.class).hasMessageContaining("context limit");
        assertThat(summaryInput.get()).isNull();
        assertThat(requests).hasSize(3);
    }

    @Test
    void shouldKeepCompactedHistoryAcrossFollowingToolRoundsAndRepeatedCompaction() throws Exception {
        trigger();
        ChatClientRequest original = request(history());
        ChatClientRequest compacted = context.compact(original, original);
        assertThat(summaryInput.get().getUserMessage().getText()).contains("task", "Exact artist id").doesNotContain("fresh page", "unverified claim");
        assertThat(JsonConverter.toJson(compacted.prompt().getInstructions()))
                .contains("task", "Exact artist id", "fresh page").doesNotContain("unverified claim");
        tokenCount.set(250);
        try (Response ignored = context.intercept(chain(withToolHistory()))) {
            assertThat(body(requests.getLast()).path("max_tokens").asInt()).isEqualTo(650);
        }
        List<Message> nextHistory = new ArrayList<>(history());
        nextHistory.add(toolCall("next call"));
        nextHistory.add(tool("next data"));
        ChatClientRequest next = request(nextHistory);
        ChatClientRequest effective = context.effectiveRequest(next);
        assertThat(JsonConverter.toJson(effective.prompt().getInstructions()))
                .contains("Exact artist id", "next data").doesNotContain("unverified claim");
        trigger();
        ChatClientRequest second = context.compact(next, effective);
        assertThat(summaryInput.get().getUserMessage().getText()).contains("Exact artist id", "fresh page");
        assertThat(second.prompt().getInstructions()).filteredOn(message -> message.getText().startsWith("Summary of earlier"))
                .hasSize(1);
    }

    private void trigger() throws Exception {
        tokenCount.set(700);
        assertThatThrownBy(() -> context.intercept(chain(withToolHistory())))
                .isInstanceOf(DiscoveryContextInterceptor.CompactionRequired.class);
    }

    @Test
    void shouldSelectFromAnOversizedLatestToolResultAndKeepItsEvidenceAcrossCompactions() throws Exception {
        TestContext selecting = new TestContext(0.9, 0.7, 0.3, prompt -> {
            String input = prompt.getUserMessage().getText().split("Numbered source fragments:\\n", 2)[1];
            JsonNode fragments = JsonConverter.fromJson(input, JsonNode.class);
            List<Integer> keep = new ArrayList<>();
            for (JsonNode fragment : fragments) {
                if (fragment.path("text").asText().contains("required-newest-release")) {
                    keep.add(fragment.path("index").asInt());
                }
            }
            assertThat(keep).isNotEmpty();
            return response(JsonConverter.toJson(Map.of("keep", keep)), "STOP");
        });
        selecting.getBudget().setContextSize(100_000);
        List<Message> messages = new ArrayList<>(history());
        messages.set(5, tool("page boilerplate ".repeat(6000)
                + "\nrequired-newest-release: Confirmed release 2009 https://example.test/album/latest"));
        selecting.effectiveRequest(request(messages.subList(0, 2)));
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = selecting.compact(original, original);

        assertThat(compacted.prompt().getInstructions()).hasSize(3);
        assertThat(compacted.prompt().getInstructions().subList(0, 2)).isEqualTo(messages.subList(0, 2));
        assertThat(JsonConverter.toJson(compacted.prompt().getInstructions()))
                .contains("Exact artist id", "required-newest-release", "Confirmed release 2009", "https://example.test/album/latest")
                .doesNotContain("page boilerplate ".repeat(1000));

        ChatClientRequest reselected = selecting.compact(original, compacted);
        assertThat(reselected.prompt().getInstructions()).hasSize(3);
        assertThat(JsonConverter.toJson(reselected.prompt().getInstructions()))
                .contains("Exact artist id", "required-newest-release", "Confirmed release 2009", "https://example.test/album/latest")
                .doesNotContain("page boilerplate ".repeat(1000));

        messages.add(toolCall("next call"));
        messages.add(tool("next data"));
        ChatClientRequest next = request(messages);
        ChatClientRequest second = selecting.compact(next, selecting.effectiveRequest(next));

        assertThat(JsonConverter.toJson(second.prompt().getInstructions()))
                .contains("Exact artist id", "required-newest-release", "Confirmed release 2009", "next data")
                .doesNotContain("page boilerplate ".repeat(1000));
        assertThat(second.prompt().getInstructions().subList(second.prompt().getInstructions().size() - 2,
                second.prompt().getInstructions().size())).isEqualTo(messages.subList(messages.size() - 2, messages.size()));
    }

    private ChatClientRequest request(List<Message> messages) {
        return ChatClientRequest.builder().prompt(new Prompt(messages)).build();
    }

    @Test
    void shouldCompactAnOversizedFirstToolExchangeWithoutLeavingAnUnansweredToolCall() {
        TestContext selecting = new TestContext(0.9, 0.7, 0.3,
                prompt -> response("{\"keep\":[0]}", "STOP"));
        selecting.getBudget().setContextSize(100_000);
        List<Message> messages = List.of(new SystemMessage("rules"), new UserMessage("task"),
                toolCall("first call"), tool("required first observation\n" + "boilerplate ".repeat(1000)));
        selecting.effectiveRequest(request(messages.subList(0, 2)));
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = selecting.compact(original, original);

        assertThat(compacted.prompt().getInstructions()).hasSize(3);
        assertThat(compacted.prompt().getInstructions().subList(0, 2)).isEqualTo(messages.subList(0, 2));
        assertThat(compacted.prompt().getInstructions()).noneMatch(message ->
                message instanceof AssistantMessage || message instanceof ToolResponseMessage);
        assertThat(compacted.prompt().getUserMessage().getText()).contains("required first observation")
                .doesNotContain("boilerplate ".repeat(1000));
    }

    private List<Message> history() {
        return List.of(new SystemMessage("rules"), new UserMessage("task"),
                toolCall("unverified claim"), tool("Exact artist id"),
                toolCall("latest call"), tool("fresh page"));
    }

    private AssistantMessage toolCall(String text) {
        return AssistantMessage.builder().content(text).toolCalls(List.of(
                new AssistantMessage.ToolCall("id", "function", "browser_snapshot", "{}"))).build();
    }

    private ToolResponseMessage tool(String text) {
        return ToolResponseMessage.builder().responses(List.of(
                new ToolResponseMessage.ToolResponse("id", "browser_snapshot", text))).build();
    }

    @Test
    void shouldKeepSeparateSourceRangesDistinctWhenCompactingAgain() throws Exception {
        AtomicInteger selections = new AtomicInteger();
        TestContext selecting = new TestContext(0.9, 0.7, 0.3, prompt -> {
            summaryInput.set(prompt);
            return response(selections.incrementAndGet() == 1 ? "{\"keep\":[0,2]}" : "{\"keep\":[0]}", "STOP");
        });
        selecting.getBudget().setContextSize(100_000);
        List<Message> messages = new ArrayList<>(history());
        messages.set(3, tool("x".repeat(5500)));
        ChatClientRequest original = request(messages);
        ChatClientRequest compacted = selecting.compact(original, original);
        String summary = compacted.prompt().getInstructions().get(2).getText().split(":\n", 2)[1];
        JsonNode ranges = JsonConverter.fromJson(summary, JsonNode.class);
        assertThat(ranges.size()).isEqualTo(2);

        messages.add(toolCall("next call"));
        messages.add(tool("next data"));
        ChatClientRequest next = request(messages);
        selecting.compact(next, selecting.effectiveRequest(next));

        String numbered = summaryInput.get().getUserMessage().getText().split("\n\nNumbered source fragments:\n", 2)[1];
        StringBuilder source = new StringBuilder();
        for (JsonNode fragment : JsonConverter.fromJson(numbered, JsonNode.class)) {
            source.append(fragment.path("text").asText());
        }
        assertThat(source.toString()).contains(ranges.get(0).asText(), ranges.get(1).asText())
                .doesNotContain(ranges.get(0).asText() + "\n\n" + ranges.get(1).asText());
    }

    @Test
    void shouldKeepFocusedEvidenceWhenBothSelectionsDropItsFragments() throws Exception {
        String observed = "{\"title\":\"Confirmed release\",\"year\":2009,\"albums\":[]}";
        List<Message> messages = new ArrayList<>(history());
        messages.set(3, tool("unrelated page ".repeat(500)));
        messages.add(4, AssistantMessage.builder().toolCalls(List.of(new AssistantMessage.ToolCall(
                "focused", "function", "inspect", "{\"url\":\"https://example.test/album/confirmed\"}"))).build());
        messages.add(5, ToolResponseMessage.builder().responses(List.of(
                new ToolResponseMessage.ToolResponse("focused", "inspect", observed))).build());
        ChatClientRequest original = request(messages);

        ChatClientRequest first = context.compact(original, original);
        ChatClientRequest second = context.compact(original, first);

        assertThat(JsonConverter.toJson(first.prompt().getInstructions())).contains("Confirmed release", "2009", "albums");
        List<?> excerpts = JsonConverter.fromJson(second.prompt().getInstructions().get(2).getText().split(":\n", 2)[1], List.class);
        assertThat(excerpts).anySatisfy(excerpt -> assertThat(excerpt.toString())
                .contains("https://example.test/album/confirmed", observed));
    }

    @Test
    void shouldKeepEachFocusedResultWithItsOwnToolCall() throws Exception {
        List<Message> messages = new ArrayList<>(history());
        messages.set(3, tool("unrelated page ".repeat(500)));
        messages.add(4, AssistantMessage.builder().toolCalls(List.of(
                new AssistantMessage.ToolCall("first", "function", "inspect", "first query"),
                new AssistantMessage.ToolCall("second", "function", "inspect", "second query"))).build());
        messages.add(5, ToolResponseMessage.builder().responses(List.of(
                new ToolResponseMessage.ToolResponse("second", "inspect", "second result"),
                new ToolResponseMessage.ToolResponse("first", "inspect", "first result"))).build());
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = context.compact(original, original);

        List<?> excerpts = JsonConverter.fromJson(compacted.prompt().getInstructions().get(2).getText().split(":\n", 2)[1], List.class);
        assertThat(excerpts).anySatisfy(excerpt -> assertThat(excerpt.toString())
                .contains("inspect:\nfirst query\n\nResult:\nfirst result"));
        assertThat(excerpts).anySatisfy(excerpt -> assertThat(excerpt.toString())
                .contains("inspect:\nsecond query\n\nResult:\nsecond result"));
    }

    @Test
    void shouldPreserveDecodedArgumentAndObservedIdentifiersWithoutRepeatingTransportIdentifiers() throws Exception {
        String transportId = "transport-id-also-present-in-source";
        String code = "() => {\n  const key = \"" + transportId + "\";\n  return 'Артист 🎵';\n}";
        String observed = "Artist id music-id; album https://example.test/album/release; no matching release found";
        List<Message> messages = new ArrayList<>(history());
        messages.set(2, AssistantMessage.builder().toolCalls(List.of(new AssistantMessage.ToolCall(
                transportId, "function", "inspect", JsonConverter.toJson(Map.of("function", code))))).build());
        messages.set(3, ToolResponseMessage.builder().responses(List.of(
                new ToolResponseMessage.ToolResponse(transportId, "inspect", observed))).build());
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = context.compact(original, original);

        List<?> excerpts = JsonConverter.fromJson(compacted.prompt().getInstructions().get(2).getText().split(":\n", 2)[1], List.class);
        assertThat(excerpts).anySatisfy(excerpt -> assertThat(excerpt.toString()).contains(code, observed, "function", "inspect")
                .containsOnlyOnce(transportId));
    }

    @Test
    void shouldKeepIdenticalObservationsDistinctAcrossCompactions() {
        List<Message> messages = new ArrayList<>(history());
        messages.set(3, tool("identical observation"));
        messages.add(4, toolCall("same query on another page"));
        messages.add(5, tool("identical observation"));
        ChatClientRequest original = request(messages);

        ChatClientRequest first = context.compact(original, original);
        ChatClientRequest second = context.compact(original, first);

        List<?> excerpts = JsonConverter.fromJson(second.prompt().getInstructions().get(2).getText().split(":\n", 2)[1], List.class);
        String source = String.join("\n", excerpts.stream().map(Object::toString).toList());
        assertThat(source.split("Result:\\nidentical observation", -1)).hasSize(3);
    }

    @Test
    void shouldRejectCompactionThatDoesNotReachThirtyPercent() throws Exception {
        trigger();
        ChatClientRequest original = request(history());
        context.compact(original, original);
        tokenCount.set(301);
        assertThatThrownBy(() -> context.intercept(chain(withToolHistory())))
                .isInstanceOf(LlmContextLimitException.class).hasMessageContaining("target context");
    }

    @Test
    void shouldKeepTargetVerificationPendingWhileAccountingForSelectionRequests() throws Exception {
        trigger();
        ChatClientRequest original = request(history());
        context.compact(original, original);
        DiscoveryContextInterceptor selectionInterceptor = DiscoveryContextInterceptor.forSelection(context.getBudget());

        try (Response ignored = selectionInterceptor.intercept(chain(withToolHistory()))) {
            assertThat(body(requests.getLast()).path("max_tokens").asInt()).isEqualTo(200);
        }

        tokenCount.set(301);
        assertThatThrownBy(() -> context.intercept(chain(withToolHistory())))
                .isInstanceOf(LlmContextLimitException.class).hasMessageContaining("target context");
    }

    @Test
    void shouldRejectSelectionRequestsAtTheContextLimitWithoutGenerating() throws Exception {
        tokenCount.set(900);
        DiscoveryContextInterceptor selectionInterceptor = DiscoveryContextInterceptor.forSelection(context.getBudget());

        assertThatThrownBy(() -> selectionInterceptor.intercept(chain(withToolHistory())))
                .isInstanceOf(LlmContextLimitException.class).hasMessageContaining("prompt reached the context limit");

        assertThat(requests).hasSize(3);
        assertThat(requests).noneMatch(request -> request.url().encodedPath().endsWith("/chat/completions"));
    }

    @Test
    void shouldBoundReselectionByHalvingTheFragmentBudgetUntilOne() throws Exception {
        context.getBudget().setContextSize(100_000);
        ChatClientRequest original = request(history());
        ChatClientRequest effective = original;
        tokenCount.set(30_001);
        for (int budget : new int[]{15, 7, 3, 1}) {
            effective = context.compact(original, effective);
            assertThat(summaryInput.get().getSystemMessage().getText()).contains("at most " + budget + " fragments");
            assertThat(JsonConverter.toJson(effective.prompt().getInstructions())).contains("Exact artist id", "fresh page");
            if (budget > 1) {
                assertThatThrownBy(() -> context.intercept(chain(withToolHistory())))
                        .isInstanceOf(DiscoveryContextInterceptor.CompactionRequired.class);
            } else {
                assertThatThrownBy(() -> context.intercept(chain(withToolHistory())))
                        .isInstanceOf(LlmContextLimitException.class).hasMessageContaining("target context");
            }
        }
        assertThat(requests).noneMatch(request -> request.url().encodedPath().equals("/v1/chat/completions"));
    }

    @Test
    void shouldRejectASelectorThatIgnoresTheFragmentBudget() throws Exception {
        TestContext selecting = new TestContext(0.9, 0.7, 0.3,
                prompt -> response("{\"keep\":[0,1]}", "STOP"));
        tokenCount.set(700);
        assertThatThrownBy(() -> selecting.intercept(chain(withToolHistory())))
                .isInstanceOf(DiscoveryContextInterceptor.CompactionRequired.class);
        List<Message> messages = new ArrayList<>(history());
        messages.set(3, tool("large page ".repeat(500)));
        ChatClientRequest original = request(messages);

        assertThatThrownBy(() -> selecting.compact(original, original))
                .isInstanceOf(LlmContextLimitException.class).hasMessageContaining("too many context source fragments");
    }

    @Test
    void shouldNotDiscardTheOnlyRecentToolExchange() throws Exception {
        trigger();
        ChatClientRequest original = request(history().subList(0, 4));
        assertThatThrownBy(() -> context.compact(original, original))
                .isInstanceOf(LlmContextLimitException.class).hasMessageContaining("latest tool exchange");
        assertThat(summaryInput.get()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"NOT_FOUND\"}", "not a summary object", "{\"keep\":[]}",
            "{\"keep\":[-1]}", "{\"keep\":[999]}", "{\"keep\":[\"0\"]}"})
    void shouldRejectAnAnswerThatDoesNotContainAContextHandoff(String answer) throws Exception {
        TestContext invalid = new TestContext(0.9, 0.7, 0.3, prompt -> response(answer, "STOP"));
        tokenCount.set(700);
        assertThatThrownBy(() -> invalid.intercept(chain(withToolHistory())))
                .isInstanceOf(DiscoveryContextInterceptor.CompactionRequired.class);
        ChatClientRequest original = request(history());

        assertThatThrownBy(() -> invalid.compact(original, original)).isInstanceOf(LlmContextLimitException.class);
    }

    @Test
    void shouldKeepOriginalFollowUpInstructionsAndPriorAnswerOutsideSelectedHistory() throws Exception {
        trigger();
        List<Message> messages = new ArrayList<>(history());
        messages.add(2, new AssistantMessage("prior answer"));
        messages.add(3, new UserMessage("preserve this follow-up instruction"));
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = context.compact(original, original);

        assertThat(compacted.prompt().getInstructions().subList(0, 4)).isEqualTo(messages.subList(0, 4));
    }

    @Test
    void shouldInsertOnlySelectedSourceDataEvenIfSelectorAddsAnInventedConclusion() throws Exception {
        TestContext selecting = new TestContext(0.9, 0.7, 0.3,
                prompt -> response("{\"keep\":[0],\"summary\":\"Invented NOT_FOUND conclusion\"}", "STOP"));
        tokenCount.set(700);
        assertThatThrownBy(() -> selecting.intercept(chain(withToolHistory())))
                .isInstanceOf(DiscoveryContextInterceptor.CompactionRequired.class);
        ChatClientRequest original = request(history());

        ChatClientRequest compacted = selecting.compact(original, original);

        assertThat(JsonConverter.toJson(compacted.prompt().getInstructions()))
                .contains("Exact artist id").doesNotContain("Invented NOT_FOUND", "unverified claim");
    }

    @Test
    void shouldRejoinAdjacentSourceFragmentsWithoutChangingCompleteUnicodeText() throws Exception {
        String completeText = "Original biography: " + "😀абв\n".repeat(600) + "complete ending";
        TestContext selecting = new TestContext(0.9, 0.7, 0.3, prompt -> {
            String input = prompt.getUserMessage().getText().split("Numbered source fragments:\\n", 2)[1];
            JsonNode fragments = JsonConverter.fromJson(input, JsonNode.class);
            List<Integer> indices = new ArrayList<>();
            for (JsonNode item : fragments) {
                String fragment = item.path("text").asText();
                assertThat(item.path("index").asInt()).isEqualTo(indices.size());
                indices.add(item.path("index").asInt());
                assertThat(new String(fragment.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.charset.StandardCharsets.UTF_8))
                        .isEqualTo(fragment);
            }
            java.util.Collections.reverse(indices);
            return response(JsonConverter.toJson(Map.of("keep", indices)), "STOP");
        });
        tokenCount.set(700);
        assertThatThrownBy(() -> selecting.intercept(chain(withToolHistory())))
                .isInstanceOf(DiscoveryContextInterceptor.CompactionRequired.class);
        List<Message> messages = new ArrayList<>(history());
        messages.set(3, tool(completeText));
        selecting.getBudget().setContextSize(100_000);
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = selecting.compact(original, original);

        String selected = compacted.prompt().getInstructions().get(2).getText().split(":\\n", 2)[1];
        List<?> excerpts = JsonConverter.fromJson(selected, List.class);
        assertThat(excerpts).hasSize(1);
        assertThat(excerpts.getFirst().toString()).contains(completeText);
    }

    @Test
    void shouldUnwrapMcpTextWithoutChangingItsContents() throws Exception {
        trigger();
        String observed = "Aram\nhttps://example.test/artist/id\nExact original text";
        List<Message> messages = new ArrayList<>(history());
        messages.set(3, tool(JsonConverter.toJson(List.of(Map.of("text", observed)))));
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = context.compact(original, original);

        String selected = compacted.prompt().getInstructions().get(2).getText().split(":\\n", 2)[1];
        List<?> excerpts = JsonConverter.fromJson(selected, List.class);
        assertThat(excerpts.getFirst().toString()).contains(observed);
    }

    @Test
    void shouldKeepAdditionalFieldsInDomainJsonObjectsThatContainText() throws Exception {
        trigger();
        String observed = JsonConverter.toJson(List.of(Map.of("text", "Name", "id", "observed-id", "url", "observed-url")));
        List<Message> messages = new ArrayList<>(history());
        messages.set(3, tool(observed));
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = context.compact(original, original);

        String selected = compacted.prompt().getInstructions().get(2).getText().split(":\\n", 2)[1];
        List<?> excerpts = JsonConverter.fromJson(selected, List.class);
        assertThat(excerpts.getFirst().toString()).contains(observed);
    }

    @Test
    void shouldPinAllOriginalCallerMessagesIncludingInstructionsAfterProvidedToolHistory() throws Exception {
        List<Message> initial = new ArrayList<>(history());
        initial.add(new UserMessage("original caller instruction after supplied tool history"));
        context.effectiveRequest(request(initial));
        trigger();
        List<Message> messages = new ArrayList<>(initial);
        messages.add(toolCall("new call"));
        messages.add(tool("new source"));
        messages.add(toolCall("latest call"));
        messages.add(tool("latest source"));
        ChatClientRequest original = request(messages);

        ChatClientRequest compacted = context.compact(original, original);

        assertThat(compacted.prompt().getInstructions().subList(0, initial.size())).isEqualTo(initial);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LENGTH", "length"})
    void shouldRejectATruncatedResponseEvenWhenItContainsValidJson(String reason) {
        assertThatThrownBy(() -> DiscoveryContextInterceptor.checkFinishReason(response("{}", reason)))
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
    void shouldPreserveLengthHandlingForASmallerExistingOutputLimit() throws Exception {
        try (Response ignored = context.intercept(chain("{\"model\":\"test\",\"messages\":[],\"max_tokens\":32}"))) {
            context.checkResponse(response("{}", "LENGTH"));
            assertThat(body(requests.getLast()).path("max_tokens").asInt()).isEqualTo(32);
        }
    }

    private ChatResponse response(String text, String reason) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(reason).build())));
    }
    private static final class TestContext extends DiscoveryContext {

        private final DiscoveryContextInterceptor interceptor = new DiscoveryContextInterceptor(getBudget());
        private final DiscoveryContextCompactor compactor = new DiscoveryContextCompactor();
        private final Function<Prompt, ChatResponse> selector;

        private TestContext(double limitRatio, double triggerRatio, double targetRatio, Function<Prompt, ChatResponse> selector) {
            super(limitRatio, triggerRatio, targetRatio);
            this.selector = selector;
        }

        private Response intercept(Interceptor.Chain chain) throws IOException {
            return interceptor.intercept(chain);
        }

        private ChatClientRequest effectiveRequest(ChatClientRequest original) {
            List<Message> messages = effectiveMessages(original.prompt().getInstructions());
            return original.mutate().prompt(new Prompt(messages, original.prompt().getOptions())).build();
        }

        private ChatClientRequest compact(ChatClientRequest original, ChatClientRequest effective) {
            DiscoveryContextCompactor.Compaction compaction = compactor.prepare(this, effective);
            ChatResponse response = selector.apply(compaction.prompt());
            DiscoveryContextInterceptor.checkFinishReason(response);
            interceptor.checkUsage(response);
            compactor.complete(this, original.prompt().getInstructions().size(), compaction, response);
            return effectiveRequest(original);
        }

        private void checkUsage(ChatResponse response) {
            interceptor.checkUsage(response);
        }

        private void checkResponse(ChatResponse response) {
            interceptor.checkResponse(response);
        }
    }

}
