package net.dorokhov.pony2.core.library.service.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.ChatModelSummarizer;
import org.springframework.ai.session.compaction.CompactionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class DiscoveryCompactionStrategyTest {

    @Test
    void shouldSummarizeWholeLargeRoundAndPinEveryCallerMessage() {
        String evidence = "x".repeat(990) + "https://open.spotify.com/artist/1AbCdEfGhIjKlMnOpQrStU?si=a%2Fb " +
                "{\"candidateId\":\"1234-5678\",\"verified\":false,\"reason\":\"Другой артист 🎵\"}" + "z".repeat(5000);
        AtomicReference<Prompt> summaryPrompt = new AtomicReference<>();
        ChatModel model = model(summaryPrompt, "Verified evidence summary");
        List<Message> initial = List.of(new SystemMessage("system"), new UserMessage("artist input"),
                new UserMessage("output instructions"));
        try (DiscoveryContext context = new DiscoveryContext(initial, .98, .7)) {
            List<Message> conversation = new ArrayList<>(initial);
            conversation.add(call("a", "{}"));
            conversation.add(result("a", evidence));
            append(context, conversation.subList(initial.size(), conversation.size()));

            CompactionResult compacted = compact(context, model);

            assertThat(compacted.coveredEvents()).hasSize(2);
            assertThat(compacted.pinnedEvents()).hasSize(3);
            assertThat(compacted.preservedEvents()).isEmpty();
            assertThat(summaryPrompt.get().getUserMessage().getText()).contains(evidence);
            assertThat(summaryPrompt.get().getUserMessage().getText()).contains("artist input", "output instructions");
            assertThat(messages(context)).startsWith(initial.toArray(Message[]::new))
                    .hasSize(5).extracting(Message::getText).contains("Verified evidence summary");
            assertThat(context.getSessions().getEvents(context.getSessionId(), EventFilter.active()))
                    .noneMatch(event -> event.getMessage() instanceof ToolResponseMessage);
        }
    }

    @Test
    void shouldSummarizeEveryCompleteParallelRoundAndCarryPriorSummaryWithoutDuplicateEvents() {
        AtomicReference<Prompt> summaryPrompt = new AtomicReference<>();
        ChatModel model = model(summaryPrompt, "Earlier summary with https://source.test/id/AB12");
        try (DiscoveryContext context = new DiscoveryContext(List.of(new UserMessage("task")), .98, .7)) {
            List<Message> conversation = new ArrayList<>();
            conversation.add(call("old", "{}"));
            conversation.add(result("old", "old evidence"));
            conversation.add(AssistantMessage.builder().toolCalls(List.of(
                    new AssistantMessage.ToolCall("a", "function", "lookup", "{}"),
                    new AssistantMessage.ToolCall("b", "function", "lookup", "{}"))).build());
            conversation.add(result("b", "second result first"));
            conversation.add(result("a", "first result last"));
            append(context, conversation);
            CompactionResult first = compact(context, model);
            assertThat(first.coveredEvents()).hasSize(5);
            assertThat(first.preservedEvents()).isEmpty();
            assertThat(summaryPrompt.get().getUserMessage().getText())
                    .contains("old evidence", "second result first", "first result last");
            assertThat(messages(context)).hasSize(3);

            append(context, List.of(call("new", "{}"), result("new", "new evidence")));
            CompactionResult second = compact(context, model);
            assertThat(second.coveredEvents()).hasSize(4);
            assertThat(summaryPrompt.get().getUserMessage().getText())
                    .contains("=== PRIOR SUMMARY ===", "Earlier summary with https://source.test/id/AB12",
                            "new evidence");
            List<Message> secondView = messages(context);
            assertThat(messages(context)).isEqualTo(secondView);
            assertThat(secondView).hasSize(3);
        }
    }

    @Test
    void shouldNotSummarizeAgainWithoutNewToolHistory() {
        AtomicReference<Prompt> summaryPrompt = new AtomicReference<>();
        ChatModel model = model(summaryPrompt, "Retained evidence");
        try (DiscoveryContext context = new DiscoveryContext(List.of(new UserMessage("task")), .98, .7)) {
            append(context, List.of(call("a", "{}"), result("a", "evidence")));
            compact(context, model);
            List<Message> active = messages(context);

            assertThat(compact(context, model).isUnchanged()).isTrue();
            assertThat(messages(context)).isEqualTo(active);
            verify(model, times(1)).call(any(Prompt.class));
        }
    }

    private void append(DiscoveryContext context, List<Message> messages) {
        for (Message message : messages) {
            context.getSessions().appendEvent(SessionEvent.builder().sessionId(context.getSessionId()).message(message).build());
        }
    }

    private List<Message> messages(DiscoveryContext context) {
        return context.getSessions().getEvents(context.getSessionId(), EventFilter.active()).stream()
                .map(SessionEvent::getMessage).toList();
    }

    private CompactionResult compact(DiscoveryContext context, ChatModel model) {
        return context.getSessions().compact(context.getSessionId(), request -> true,
                new DiscoveryCompactionStrategy(context.getMandatorySessionEventIds(), ChatModelSummarizer.builder(model)
                        .overlapSize(context.getMandatorySessionEventIds().size()).build()));
    }

    private ChatModel model(AtomicReference<Prompt> prompt, String summary) {
        ChatModel model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            prompt.set(invocation.getArgument(0));
            return new ChatResponse(List.of(new Generation(new AssistantMessage(summary))));
        });
        return model;
    }

    private AssistantMessage call(String id, String arguments) {
        return AssistantMessage.builder().toolCalls(List.of(
                new AssistantMessage.ToolCall(id, "function", "lookup", arguments))).build();
    }

    private ToolResponseMessage result(String id, String data) {
        return ToolResponseMessage.builder().responses(List.of(
                new ToolResponseMessage.ToolResponse(id, "lookup", data))).build();
    }
}
