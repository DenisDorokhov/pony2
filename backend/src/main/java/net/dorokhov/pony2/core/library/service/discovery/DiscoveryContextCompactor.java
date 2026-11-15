package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.common.JsonConverter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DiscoveryContextCompactor {

    private static final int MAX_FOCUSED_EXCHANGE_CHARS = 4000;
    private static final String SUMMARY_SCHEMA = """
            {"type":"object","properties":{"keep":{"type":"array","minItems":1,"maxItems":%d,
             "items":{"type":"integer","minimum":0}}},"required":["keep"],"additionalProperties":false}
            """;

    private final String systemPrompt = readPrompt("discovery-context-compaction-system.txt");
    private final String pinnedTailPrompt = readPrompt("discovery-context-compaction-pinned-tail.txt");
    private final String selectedTailPrompt = readPrompt("discovery-context-compaction-selected-tail.txt");
    private final String inputPrompt = readPrompt("discovery-context-compaction-input.txt");
    private final String historyPrompt = readPrompt("discovery-context-compaction-history.txt");
    private final String excerptPrompt = readPrompt("discovery-context-compaction-excerpt.txt");

    private String readPrompt(String name) {
        try {
            return new ClassPathResource("prompts/" + name).getContentAsString(UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Compaction prepare(DiscoveryContext context, ChatClientRequest effective) {
        List<Message> messages = effective.prompt().getInstructions();
        if (context.getPinnedMessageCount() < 0) {
            context.setPinnedMessageCount(0);
            while (context.getPinnedMessageCount() < messages.size() &&
                    (!(messages.get(context.getPinnedMessageCount()) instanceof AssistantMessage assistant) || assistant.getToolCalls().isEmpty())) {
                context.setPinnedMessageCount(context.getPinnedMessageCount() + 1);
            }
        }
        int historyStart = context.getPinnedMessageCount();
        int tailStart = messages.size();
        if (!messages.isEmpty() && messages.getLast() instanceof ToolResponseMessage) {
            tailStart = Math.max(historyStart, messages.size() - 2);
            if (tailStart < messages.size() && messages.get(tailStart) instanceof ToolResponseMessage) {
                tailStart--;
            }
            if (!isFocusedExchange(messages.subList(tailStart, messages.size()))) {
                tailStart = messages.size();
            }
        }
        if (tailStart <= historyStart) {
            throw new LlmContextLimitException("Context cannot be compacted without dropping the latest tool exchange.");
        }
        // Leave room for complete focused observations and the pinned messages.
        if (context.getCompactionFragmentBudget() == 0) {
            context.setCompactionFragmentBudget(Math.max(1, (int) (context.getContextSize() * context.getTargetRatio()) / 2000));
        }
        int fragmentBudget = context.getCompactionFragmentBudget();
        String instructions = systemPrompt.formatted(tailStart < messages.size() ? pinnedTailPrompt : selectedTailPrompt,
                fragmentBudget);
        OpenAiChatOptions.Builder summaryOptions = OpenAiChatOptions.builder();
        ChatOptions previousOptions = effective.prompt().getOptions();
        if (previousOptions != null) {
            summaryOptions.combineWith(previousOptions.mutate());
        }
        List<String> fragments = historyFragments(context, toolExchanges(context, messages.subList(historyStart, tailStart)));
        List<Map<String, Object>> numbered = new ArrayList<>();
        for (int index = 0; index < fragments.size(); index++) {
            numbered.add(Map.of("index", index, "text", fragments.get(index)));
        }
        Prompt prompt = new Prompt(List.of(new SystemMessage(instructions),
                new UserMessage(inputPrompt.formatted(JsonConverter.toJson(messages.subList(0, historyStart)),
                        JsonConverter.toJson(numbered)))),
                summaryOptions.toolCallbacks(List.of()).toolChoice("none").reasoningEffort("none")
                        .responseFormat(ResponseFormat.builder().jsonSchema(SUMMARY_SCHEMA.formatted(fragmentBudget)).strict(true).build())
                        .maxTokens(Math.max(1024, fragmentBudget * 8 + 32)).build());
        return new Compaction(prompt, messages, historyStart, tailStart, fragments, fragmentBudget);
    }

    public void complete(DiscoveryContext context, int originalMessageCount, Compaction compaction, ChatResponse response) {
        List<String> history = selectedHistory(context, response, compaction.fragments(), compaction.fragmentBudget());
        List<Message> compacted = new ArrayList<>(compaction.messages().subList(0, compaction.historyStart()));
        compacted.add(new UserMessage(historyPrompt.formatted(JsonConverter.toJson(history))));
        compacted.addAll(compaction.messages().subList(compaction.tailStart(), compaction.messages().size()));
        context.applyCompaction(originalMessageCount, compacted, history);
    }

    private boolean isFocusedExchange(List<Message> messages) {
        long size = 0;
        for (Message message : messages) {
            if (message instanceof AssistantMessage assistant) {
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    size += call.id().length() + call.name().length() + call.arguments().length();
                }
            } else if (message instanceof ToolResponseMessage tool) {
                for (ToolResponseMessage.ToolResponse response : tool.getResponses()) {
                    size += response.id().length() + response.name().length() + toolText(response.responseData()).length();
                }
            }
        }
        return size <= MAX_FOCUSED_EXCHANGE_CHARS;
    }

    private List<String> toolExchanges(DiscoveryContext context, List<Message> messages) {
        Map<String, AssistantMessage.ToolCall> calls = new LinkedHashMap<>();
        List<String> exchanges = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof AssistantMessage assistant) {
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    AssistantMessage.ToolCall replaced = calls.put(call.id(), call);
                    if (replaced != null) {
                        exchanges.add("Tool call " + replaced.id() + " " + replaced.name() + ":\n" + replaced.arguments());
                    }
                }
            } else if (message instanceof ToolResponseMessage tool) {
                for (ToolResponseMessage.ToolResponse response : tool.getResponses()) {
                    AssistantMessage.ToolCall call = calls.remove(response.id());
                    String result = toolText(response.responseData());
                    String rawResult = "Tool result " + response.id() + " " + response.name() + ":\n" + result;
                    if (call == null) {
                        exchanges.add(rawResult);
                        continue;
                    }
                    // A short ordinal keeps repeated observations distinct without repeating transport IDs.
                    String exchange = "Tool " + context.nextExchangeIndex() + " " + call.name() + ":\n" + sourceArguments(call.arguments())
                            + "\n\nResult" + (Objects.equals(call.name(), response.name()) ? "" : " " + response.name()) + ":\n" + result;
                    exchanges.add(exchange);
                    String rawCall = "Tool call " + call.id() + " " + call.name() + ":\n" + call.arguments();
                    if (rawCall.length() + rawResult.length() <= MAX_FOCUSED_EXCHANGE_CHARS) {
                        context.getFocusedExchanges().add(exchange);
                    }
                }
            }
        }
        for (AssistantMessage.ToolCall call : calls.values()) {
            exchanges.add("Tool call " + call.id() + " " + call.name() + ":\n" + call.arguments());
        }
        return exchanges;
    }

    private String sourceArguments(String data) {
        try {
            JsonNode arguments = JsonConverter.fromJson(data, JsonNode.class);
            if (arguments.isObject() && arguments.size() == 1) {
                Map.Entry<String, JsonNode> argument = arguments.properties().iterator().next();
                if (argument.getValue().isTextual()) {
                    return JsonConverter.toJson(argument.getKey()) + ":\n" + argument.getValue().asText();
                }
            }
        } catch (RuntimeException ignored) {
            // Preserve non-JSON arguments without changing their contents.
        }
        return data;
    }

    private List<String> selectedHistory(DiscoveryContext context, ChatResponse response, List<String> fragments, int fragmentBudget) {
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
        TreeSet<Integer> indices = new TreeSet<>();
        for (JsonNode index : keep) {
            if (!index.isIntegralNumber() || !index.canConvertToInt() || index.asInt() < 0 || index.asInt() >= fragments.size()) {
                throw new LlmContextLimitException("LLM selected an invalid context source fragment.");
            }
            indices.add(index.asInt());
        }
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
        // Keep focused observations even when the selector prefers unrelated search cards.
        for (String exchange : context.getFocusedExchanges()) {
            if (selected.stream().noneMatch(excerpt -> excerpt.contains(exchange))) {
                selected.add(exchange);
            }
        }
        return selected;
    }

    private List<String> historyFragments(DiscoveryContext context, List<String> exchanges) {
        List<String> history = new ArrayList<>();
        // Preserve omitted gaps when selecting from earlier excerpts again.
        for (String excerpt : context.getRetainedHistory()) {
            history.add(excerptPrompt.formatted(excerpt));
        }
        history.addAll(exchanges);
        String source = String.join("\n\n", history);
        List<String> fragments = new ArrayList<>();
        for (int start = 0; start < source.length();) {
            int end = Math.min(source.length(), start + 1000);
            if (end < source.length() && Character.isHighSurrogate(source.charAt(end - 1)) && Character.isLowSurrogate(source.charAt(end))) {
                end--;
            }
            fragments.add(source.substring(start, end));
            start = end;
        }
        return fragments;
    }

    private String toolText(String data) {
        try {
            JsonNode content = JsonConverter.fromJson(data, JsonNode.class);
            if (content.isArray() && !content.isEmpty()) {
                List<String> texts = new ArrayList<>();
                for (JsonNode block : content) {
                    if (!block.isObject() || !block.path("text").isTextual() ||
                            !(block.size() == 1 || block.size() == 2 && "text".equals(block.path("type").asText()))) {
                        return data;
                    }
                    texts.add(block.path("text").asText());
                }
                return String.join("\n", texts);
            }
        } catch (RuntimeException ignored) {
            // Plain tool text is already the source data; only unwrap valid MCP text blocks.
        }
        return data;
    }

    public record Compaction(Prompt prompt, List<Message> messages, int historyStart, int tailStart,
                             List<String> fragments, int fragmentBudget) {}
}
