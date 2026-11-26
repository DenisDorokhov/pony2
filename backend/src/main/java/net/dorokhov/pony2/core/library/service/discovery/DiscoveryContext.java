package net.dorokhov.pony2.core.library.service.discovery;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DiscoveryContext implements AutoCloseable {

    private final DiscoveryContextBudget budget;
    private final SessionService sessions;
    private final String sessionId;
    private final Set<String> mandatorySessionEventIds;

    public DiscoveryContext(List<Message> callerMessages, double limitRatio, double triggerRatio) {
        sessions = DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build()).allowSystemMessages(true).build();
        sessionId = sessions.create(CreateSessionRequest.builder().build()).id();
        budget = new DiscoveryContextBudget(limitRatio, triggerRatio);
        // Save every input message now. Spring's memory advisor would only save the last user message.
        mandatorySessionEventIds = callerMessages.stream().map(message -> {
            SessionEvent event = SessionEvent.builder().sessionId(sessionId).message(message).build();
            sessions.appendEvent(event);
            return event.getId();
        }).collect(Collectors.toUnmodifiableSet());
    }

    public DiscoveryContextBudget getBudget() {
        return budget;
    }

    public SessionService getSessions() {
        return sessions;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Set<String> getMandatorySessionEventIds() {
        return mandatorySessionEventIds;
    }

    @Override
    public void close() {
        sessions.delete(sessionId);
    }
}
