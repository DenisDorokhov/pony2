package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.LlmContextLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
public class DiscoveryAdvisor implements BaseAdvisor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LibraryJobSynchronizer jobSynchronizer;

    public DiscoveryAdvisor(LibraryJobSynchronizer jobSynchronizer) {
        this.jobSynchronizer = jobSynchronizer;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientRequest original = before(request, chain);
        DiscoveryChatClient.Invocation invocation = (DiscoveryChatClient.Invocation) original.context().get(DiscoveryChatClient.INVOCATION_KEY);
        DiscoveryContext context = invocation != null ? invocation.context() : null;
        ChatClientRequest effective = context != null ? effectiveRequest(original, context) : original;
        ChatClientResponse response;
        CallAdvisorChain currentChain = chain;
        while (true) {
            try {
                response = currentChain.nextCall(effective);
                break;
            } catch (DiscoveryContextInterceptor.CompactionRequired required) {
                jobSynchronizer.interruptDiscoveryIfStopped();
                logger.info("Compacting discovery context at {} input tokens.", required.inputTokens());
                requireNonNull(invocation).compact(original, effective);
                effective = effectiveRequest(original, requireNonNull(context));
                jobSynchronizer.interruptDiscoveryIfStopped();
                currentChain = chain.copy(this);
            }
        }
        ChatClientResponse checked = after(response, chain);
        if (invocation != null && checked.chatResponse() != null) {
            invocation.checkResponse(checked.chatResponse());
        }
        return checked;
    }

    ChatClientRequest effectiveRequest(ChatClientRequest original, DiscoveryContext context) {
        if (context.getPinnedMessageCount() < 0) {
            context.setPinnedMessageCount(original.prompt().getInstructions().size());
        }
        List<Message> compacted = context.getCompactedMessages();
        if (compacted == null) {
            return original;
        }
        List<Message> incoming = original.prompt().getInstructions();
        if (incoming.size() < context.getCompactedThrough()) {
            throw new LlmContextLimitException("Tool history changed after context compaction.");
        }
        List<Message> messages = new ArrayList<>(compacted);
        messages.addAll(incoming.subList(context.getCompactedThrough(), incoming.size()));
        return original.mutate().prompt(new Prompt(messages, original.prompt().getOptions())).build();
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        jobSynchronizer.interruptDiscoveryIfStopped();
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        jobSynchronizer.interruptDiscoveryIfStopped();
        ChatResponse chatResponse = response.chatResponse();
        if (logger.isDebugEnabled() && chatResponse != null) {
            for (Generation generation : chatResponse.getResults()) {
                Object reasoning = generation.getOutput().getMetadata().get("reasoningContent");
                if (reasoning instanceof String text && !text.isBlank()) {
                    logger.debug("Discovery LLM thinking: {}", text.trim());
                }
            }
        }
        return response;
    }

    @Override
    public int getOrder() {
        // Run inside the tool-calling loop to check shutdown and log reasoning for every model invocation.
        return ToolCallingAdvisor.DEFAULT_ORDER + 1;
    }
}
