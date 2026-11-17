package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.library.service.exception.LlmContextLimitException;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryContext {

    private final DiscoveryContextBudget budget;

    private int pinnedMessageCount = -1;
    private int compactedThrough;
    private List<Message> compactedMessages;
    private List<String> retainedHistory = List.of();
    private final DiscoveryToolHistory toolHistory = new DiscoveryToolHistory();

    public DiscoveryContext(double limitRatio, double triggerRatio, double targetRatio) {
        budget = new DiscoveryContextBudget(limitRatio, triggerRatio, targetRatio);
    }

    public DiscoveryContextBudget getBudget() {
        return budget;
    }

    public int getPinnedMessageCount() {
        return pinnedMessageCount;
    }

    public void pinMessages(int count) {
        if (pinnedMessageCount < 0) {
            pinnedMessageCount = count;
        }
    }

    public boolean hasCompactedHistory() {
        return compactedMessages != null;
    }

    public List<Message> effectiveMessages(List<Message> incoming) {
        pinMessages(incoming.size());
        if (compactedMessages == null) {
            return incoming;
        }
        if (incoming.size() < compactedThrough) {
            throw new LlmContextLimitException("Tool history changed after context compaction.");
        }
        List<Message> messages = new ArrayList<>(compactedMessages);
        messages.addAll(incoming.subList(compactedThrough, incoming.size()));
        return messages;
    }

    public List<String> getRetainedHistory() {
        return retainedHistory;
    }

    public DiscoveryToolHistory getToolHistory() {
        return toolHistory;
    }

    public void applyCompaction(int originalMessageCount, List<Message> messages, List<String> history) {
        compactedThrough = originalMessageCount;
        compactedMessages = messages;
        retainedHistory = history;
        budget.markCompactionApplied();
    }
}
