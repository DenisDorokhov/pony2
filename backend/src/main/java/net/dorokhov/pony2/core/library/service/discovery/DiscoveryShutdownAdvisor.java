package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryShutdownAdvisor implements BaseAdvisor {

    private final ShutdownService shutdownService;

    public DiscoveryShutdownAdvisor(ShutdownService shutdownService) {
        this.shutdownService = shutdownService;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        checkShutdown();
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        checkShutdown();
        return response;
    }

    @Override
    public int getOrder() {
        // Run inside the tool-calling loop so every model invocation checks for shutdown.
        return ToolCallingAdvisor.DEFAULT_ORDER + 1;
    }

    private void checkShutdown() {
        if (shutdownService.isShutdown()) {
            throw new DiscoveryInterruptedException();
        }
    }
}
