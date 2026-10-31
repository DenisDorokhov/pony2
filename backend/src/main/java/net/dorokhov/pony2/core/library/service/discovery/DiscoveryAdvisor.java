package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryAdvisor implements BaseAdvisor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LibraryJobSynchronizer jobSynchronizer;

    public DiscoveryAdvisor(LibraryJobSynchronizer jobSynchronizer) {
        this.jobSynchronizer = jobSynchronizer;
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
