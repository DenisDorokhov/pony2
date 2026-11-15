package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
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
import org.springframework.stereotype.Component;

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
        DiscoveryContext context = (DiscoveryContext) original.context().get(DiscoveryContext.KEY);
        ChatClientRequest effective = context != null ? context.effectiveRequest(original) : original;
        ChatClientResponse response;
        try {
            response = chain.nextCall(effective);
        } catch (DiscoveryContext.CompactionRequired required) {
            jobSynchronizer.interruptDiscoveryIfStopped();
            logger.info("Compacting discovery context at {} input tokens.", required.inputTokens());
            ChatClientRequest compacted = requireNonNull(context).compact(original, effective);
            jobSynchronizer.interruptDiscoveryIfStopped();
            response = chain.copy(this).nextCall(compacted);
        }
        ChatClientResponse checked = after(response, chain);
        if (context != null && checked.chatResponse() != null) {
            context.checkResponse(checked.chatResponse());
        }
        return checked;
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
