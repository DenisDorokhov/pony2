package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmPromptTemplate;
import net.dorokhov.pony2.core.library.service.exception.LlmContextLimitException;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class DiscoveryContextCompactor {

    private static final int SOURCE_FRAGMENT_CHARS = 1000;
    private static final int FRAGMENT_BUDGET_DIVISOR = SOURCE_FRAGMENT_CHARS * 2;
    private static final String SELECTION_SCHEMA = """
            {"type":"object","properties":{"keep":{"type":"array","minItems":1,"maxItems":%d,
             "items":{"type":"integer","minimum":0}}},"required":["keep"],"additionalProperties":false}
            """;

    private final LlmPromptTemplate systemPrompt = readPrompt("common-compact-system");
    private final LlmPromptTemplate userPrompt = readPrompt("common-compact-user");
    private final LlmPromptTemplate historyUserPrompt = readPrompt("common-history-user");

    private LlmPromptTemplate readPrompt(String name) {
        try {
            return new LlmPromptTemplate(new ClassPathResource("prompts/" + name + ".vm"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Compaction prepare(DiscoveryContext context, ChatClientRequest effective) {
        List<Message> messages = effective.prompt().getInstructions();
        int historyStart = pinnedHistoryStart(context, messages);
        int tailStart = retainedTailStart(context, messages, historyStart);
        if (tailStart <= historyStart) {
            throw new LlmContextLimitException("Context cannot be compacted without dropping the latest tool exchange.");
        }
        // Leave room for complete focused observations and the pinned messages.
        context.getBudget().initializeCompactionFragmentBudget(FRAGMENT_BUDGET_DIVISOR);
        int fragmentBudget = context.getBudget().getCompactionFragmentBudget();
        String instructions = systemPrompt.render(Map.of("tailPinned", tailStart < messages.size(), "fragmentBudget", fragmentBudget));
        OpenAiChatOptions.Builder options = selectionOptions(effective.prompt().getOptions());
        List<String> exchanges = context.getToolHistory().collect(messages.subList(historyStart, tailStart));
        List<String> fragments = historyFragments(context, exchanges);
        Prompt prompt = selectionPrompt(instructions, messages.subList(0, historyStart), fragments, fragmentBudget, options);
        return new Compaction(prompt, messages, historyStart, tailStart, fragments, fragmentBudget);
    }

    private int pinnedHistoryStart(DiscoveryContext context, List<Message> messages) {
        if (context.getPinnedMessageCount() < 0) {
            int pinnedMessageCount = 0;
            while (pinnedMessageCount < messages.size() &&
                    (!(messages.get(pinnedMessageCount) instanceof AssistantMessage assistant) || assistant.getToolCalls().isEmpty())) {
                pinnedMessageCount++;
            }
            context.pinMessages(pinnedMessageCount);
        }
        return context.getPinnedMessageCount();
    }

    private int retainedTailStart(DiscoveryContext context, List<Message> messages, int historyStart) {
        int tailStart = messages.size();
        if (!messages.isEmpty() && messages.getLast() instanceof ToolResponseMessage) {
            tailStart = Math.max(historyStart, messages.size() - 2);
            if (tailStart < messages.size() && messages.get(tailStart) instanceof ToolResponseMessage) {
                tailStart--;
            }
            if (!context.getToolHistory().isShortExchange(messages.subList(tailStart, messages.size()))) {
                tailStart = messages.size();
            }
        }
        return tailStart;
    }

    private OpenAiChatOptions.Builder selectionOptions(@Nullable ChatOptions previousOptions) {
        OpenAiChatOptions.Builder selectionOptions = OpenAiChatOptions.builder();
        if (previousOptions != null) {
            selectionOptions.combineWith(previousOptions.mutate());
        }
        return selectionOptions;
    }

    private List<String> historyFragments(DiscoveryContext context, List<String> exchanges) {
        List<String> history = new ArrayList<>();
        // Preserve omitted gaps when selecting from earlier excerpts again.
        for (String excerpt : context.getRetainedHistory()) {
            history.add("Separate earlier source excerpt:\n" + excerpt);
        }
        history.addAll(exchanges);
        String source = String.join("\n\n", history);
        List<String> fragments = new ArrayList<>();
        for (int start = 0; start < source.length();) {
            int end = Math.min(source.length(), start + SOURCE_FRAGMENT_CHARS);
            if (end < source.length() && Character.isHighSurrogate(source.charAt(end - 1)) && Character.isLowSurrogate(source.charAt(end))) {
                end--;
            }
            fragments.add(source.substring(start, end));
            start = end;
        }
        return fragments;
    }

    private Prompt selectionPrompt(
            String instructions, List<Message> pinnedMessages, List<String> fragments,
            int fragmentBudget, OpenAiChatOptions.Builder options
    ) {
        List<Map<String, Object>> numbered = new ArrayList<>();
        for (int index = 0; index < fragments.size(); index++) {
            numbered.add(Map.of("index", index, "text", fragments.get(index)));
        }
        return new Prompt(List.of(new SystemMessage(instructions),
                new UserMessage(userPrompt.render(Map.of("taskContextJson", JsonConverter.toJson(pinnedMessages),
                        "fragmentsJson", JsonConverter.toJson(numbered))))),
                options.toolCallbacks(List.of()).toolChoice("none").reasoningEffort("none")
                        .responseFormat(ResponseFormat.builder().jsonSchema(SELECTION_SCHEMA.formatted(fragmentBudget)).strict(true).build())
                        .maxTokens(Math.max(1024, fragmentBudget * 8 + 32)).build());
    }

    public void complete(DiscoveryContext context, int originalMessageCount, Compaction compaction, ChatResponse response) {
        List<String> history = selectedHistory(context, response, compaction.fragments(), compaction.fragmentBudget());
        List<Message> compacted = new ArrayList<>(compaction.messages().subList(0, compaction.historyStart()));
        compacted.add(new UserMessage(historyUserPrompt.render("historyJson", JsonConverter.toJson(history))));
        compacted.addAll(compaction.messages().subList(compaction.tailStart(), compaction.messages().size()));
        context.applyCompaction(originalMessageCount, compacted, history);
    }

    private List<String> selectedHistory(DiscoveryContext context, ChatResponse response, List<String> fragments, int fragmentBudget) {
        SortedSet<Integer> indices = selectedIndices(response, fragments.size(), fragmentBudget);
        List<String> selected = mergeSelectedFragments(fragments, indices);
        context.getToolHistory().addMissingFocusedExchanges(selected);
        return selected;
    }

    private SortedSet<Integer> selectedIndices(ChatResponse response, int fragmentCount, int fragmentBudget) {
        String content = response.getResult() != null ? response.getResult().getOutput().getText() : null;
        if (content == null || content.isBlank()) {
            throw new LlmContextLimitException("LLM returned an empty context summary.");
        }
        JsonNode parsed;
        try {
            parsed = JsonConverter.fromJson(content, JsonNode.class);
        } catch (RuntimeException e) {
            throw new LlmContextLimitException("LLM returned an invalid context summary.");
        }
        JsonNode keep = parsed.path("keep");
        if (!keep.isArray() || keep.isEmpty()) {
            throw new LlmContextLimitException("LLM did not select any context source fragments.");
        }
        if (keep.size() > fragmentBudget) {
            throw new LlmContextLimitException("LLM selected too many context source fragments.");
        }
        SortedSet<Integer> indices = new TreeSet<>();
        for (JsonNode index : keep) {
            if (!index.isIntegralNumber() || !index.canConvertToInt() || index.asInt() < 0 || index.asInt() >= fragmentCount) {
                throw new LlmContextLimitException("LLM selected an invalid context source fragment.");
            }
            indices.add(index.asInt());
        }
        return indices;
    }

    private List<String> mergeSelectedFragments(List<String> fragments, SortedSet<Integer> indices) {
        List<String> selected = new ArrayList<>();
        StringBuilder contiguous = new StringBuilder();
        int previous = -2;
        for (Integer index : indices) {
            if (index != previous + 1 && contiguous.length() > 0) {
                selected.add(contiguous.toString());
                contiguous.setLength(0);
            }
            contiguous.append(fragments.get(index));
            previous = index;
        }
        selected.add(contiguous.toString());
        return selected;
    }

    public record Compaction(Prompt prompt, List<Message> messages, int historyStart, int tailStart,
                             List<String> fragments, int fragmentBudget) {}
}
