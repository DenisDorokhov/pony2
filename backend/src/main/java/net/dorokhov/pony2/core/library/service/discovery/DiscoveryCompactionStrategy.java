package net.dorokhov.pony2.core.library.service.discovery;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionRequest;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionSummarizer;
import org.springframework.ai.session.compaction.SummaryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Replaces completed tool calls and their results with a summary.
 * Keeps the original input messages unchanged and includes any earlier summary when making the next one.
 */
public class DiscoveryCompactionStrategy implements CompactionStrategy {

    private final Set<String> mandatorySessionEventIds;
    private final CompactionSummarizer summarizer;

    public DiscoveryCompactionStrategy(Set<String> mandatorySessionEventIds, CompactionSummarizer summarizer) {
        this.mandatorySessionEventIds = Set.copyOf(mandatorySessionEventIds);
        this.summarizer = summarizer;
    }

    @Override
    public @NonNull CompactionResult compact(CompactionRequest request) {
        List<SessionEvent> pinned = request.events().stream().filter(event -> mandatorySessionEventIds.contains(event.getId())).toList();
        List<SessionEvent> prior = request.events().stream().filter(SessionEvent::isSynthetic).toList();
        List<SessionEvent> history = request.events().stream()
                .filter(event -> !mandatorySessionEventIds.contains(event.getId()) && !event.isSynthetic()).toList();
        if (history.isEmpty()) {
            return CompactionResult.unchanged();
        }
        List<SessionEvent> covered = new ArrayList<>(prior);
        covered.addAll(history);
        List<Message> summary = summarizer.summarize(new SummaryRequest(request.session(), prior, history, pinned));
        return new CompactionResult(covered, pinned, List.of(), summary, 0, "discovery-tool-rounds");
    }
}
