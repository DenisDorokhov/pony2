package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import org.springframework.ai.chat.messages.Message;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DiscoveryContext {

    private final double limitRatio;
    private final double triggerRatio;
    private final double targetRatio;

    private int contextSize;
    private int compactionFragmentBudget;
    private int pinnedMessageCount = -1;
    private boolean summarizing;
    private boolean verifyCompactionTarget;
    private boolean contextLimitApplied;
    private int nextExchangeIndex;
    private int compactedThrough;
    private List<Message> compactedMessages;
    private List<String> retainedHistory = List.of();
    private final Set<String> focusedExchanges = new LinkedHashSet<>();

    public DiscoveryContext(double limitRatio, double triggerRatio, double targetRatio) {
        this.limitRatio = limitRatio;
        this.triggerRatio = triggerRatio;
        this.targetRatio = targetRatio;
    }

    public double getLimitRatio() {
        return limitRatio;
    }

    public double getTriggerRatio() {
        return triggerRatio;
    }

    public double getTargetRatio() {
        return targetRatio;
    }

    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
    }

    public int getCompactionFragmentBudget() {
        return compactionFragmentBudget;
    }

    public void setCompactionFragmentBudget(int compactionFragmentBudget) {
        this.compactionFragmentBudget = compactionFragmentBudget;
    }

    public int getPinnedMessageCount() {
        return pinnedMessageCount;
    }

    public void setPinnedMessageCount(int pinnedMessageCount) {
        this.pinnedMessageCount = pinnedMessageCount;
    }

    public boolean isSummarizing() {
        return summarizing;
    }

    public void setSummarizing(boolean summarizing) {
        this.summarizing = summarizing;
    }

    public boolean isVerifyCompactionTarget() {
        return verifyCompactionTarget;
    }

    public void setVerifyCompactionTarget(boolean verifyCompactionTarget) {
        this.verifyCompactionTarget = verifyCompactionTarget;
    }

    public boolean isContextLimitApplied() {
        return contextLimitApplied;
    }

    public void setContextLimitApplied(boolean contextLimitApplied) {
        this.contextLimitApplied = contextLimitApplied;
    }

    public int nextExchangeIndex() {
        return nextExchangeIndex++;
    }

    public int getCompactedThrough() {
        return compactedThrough;
    }

    public @Nullable List<Message> getCompactedMessages() {
        return compactedMessages;
    }

    public List<String> getRetainedHistory() {
        return retainedHistory;
    }

    public Set<String> getFocusedExchanges() {
        return focusedExchanges;
    }

    public void applyCompaction(int originalMessageCount, List<Message> messages, List<String> history) {
        compactedThrough = originalMessageCount;
        compactedMessages = messages;
        retainedHistory = history;
        verifyCompactionTarget = true;
    }
}
