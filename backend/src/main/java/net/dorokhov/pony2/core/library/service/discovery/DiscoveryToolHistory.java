package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.common.JsonConverter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DiscoveryToolHistory {

    private static final int MAX_FOCUSED_EXCHANGE_CHARS = 4000;

    private int nextExchangeIndex;
    private final Set<String> focusedExchanges = new LinkedHashSet<>();

    public boolean isShortExchange(List<Message> messages) {
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

    public List<String> collect(List<Message> messages) {
        Map<String, AssistantMessage.ToolCall> calls = new LinkedHashMap<>();
        List<String> exchanges = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof AssistantMessage assistant) {
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    AssistantMessage.ToolCall replaced = calls.put(call.id(), call);
                    if (replaced != null) {
                        exchanges.add(toolCallText(replaced));
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
                    String exchange = "Tool " + nextExchangeIndex++ + " " + call.name() + ":\n" + sourceArguments(call.arguments())
                            + "\n\nResult" + (Objects.equals(call.name(), response.name()) ? "" : " " + response.name()) + ":\n" + result;
                    exchanges.add(exchange);
                    String rawCall = toolCallText(call);
                    if (rawCall.length() + rawResult.length() <= MAX_FOCUSED_EXCHANGE_CHARS) {
                        focusedExchanges.add(exchange);
                    }
                }
            }
        }
        for (AssistantMessage.ToolCall call : calls.values()) {
            exchanges.add(toolCallText(call));
        }
        return exchanges;
    }

    private String toolCallText(AssistantMessage.ToolCall call) {
        return "Tool call " + call.id() + " " + call.name() + ":\n" + call.arguments();
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

    public void addMissingFocusedExchanges(List<String> selected) {
        // Keep focused observations even when the selector prefers unrelated search cards.
        for (String exchange : focusedExchanges) {
            if (selected.stream().noneMatch(excerpt -> excerpt.contains(exchange))) {
                selected.add(exchange);
            }
        }
    }
}
