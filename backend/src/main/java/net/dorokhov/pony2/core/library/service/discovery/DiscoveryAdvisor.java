package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryChatClient.Cancellation;
import net.dorokhov.pony2.core.llm.service.ChatModelImpl;
import net.dorokhov.pony2.core.llm.service.LlmContextService;
import net.dorokhov.pony2.core.llm.service.exception.LlmContextLimitException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionSummarizer;

import java.util.List;
import java.util.Set;

/**
 * Handles requests to the model during one {@code DiscoveryChatClient.call()}.
 * If the message history gets too long, replaces completed tool calls and their results with a summary
 * and retries the model request. The original input is kept, and completed tools are not run again.
 * Each call has its own session and cancellation state.
 */
final class DiscoveryAdvisor implements CallAdvisor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ChatModelImpl chatModel;
    private final LlmContextService contextService;
    private final DiscoveryContextBudgetManager budgetManager;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final DiscoveryCompactionSummarizer compactionSummarizer;
    private final DiscoveryContext context;
    private final Cancellation cancellation;

    DiscoveryAdvisor(
            ChatModelImpl chatModel,
            LlmContextService contextService,
            DiscoveryContextBudgetManager budgetManager,
            LibraryJobSynchronizer jobSynchronizer,
            DiscoveryCompactionSummarizer compactionSummarizer,
            DiscoveryContext context,
            Cancellation cancellation
    ) {
        this.chatModel = chatModel;
        this.contextService = contextService;
        this.budgetManager = budgetManager;
        this.jobSynchronizer = jobSynchronizer;
        this.compactionSummarizer = compactionSummarizer;
        this.context = context;
        this.cancellation = cancellation;
    }

    @Override
    public @NonNull ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        jobSynchronizer.interruptDiscoveryIfStopped();
        ChatClientResponse response;
        while (true) {
            try {
                response = chain.nextCall(request);
                break;
            } catch (DiscoveryContextInterceptor.CompactionRequiredException required) {
                jobSynchronizer.interruptDiscoveryIfStopped();
                logger.debug("Compacting discovery context at {} input tokens.", required.inputTokens());
                request = compact(request);
                jobSynchronizer.interruptDiscoveryIfStopped();
                chain = chain.copy(this);
            }
        }
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
        if (chatResponse != null) {
            budgetManager.checkResponse(context.getBudget(), chatResponse);
        }
        return response;
    }

    private ChatClientRequest compact(ChatClientRequest request) {
        cancellation.check();
        Set<String> mandatorySessionEventIds = context.getMandatorySessionEventIds();
        CompactionSummarizer summarizer = compactionSummarizer.create(this::summarize, request.prompt().getOptions(),
                mandatorySessionEventIds.size(), cancellation::check);
        CompactionResult result = context.getSessions().compact(context.getSessionId(), ignored -> true,
                new DiscoveryCompactionStrategy(mandatorySessionEventIds, summarizer));
        cancellation.check();
        if (result.isUnchanged()) {
            throw new LlmContextLimitException(
                    "No completed tool round can be compacted.");
        }
        // Reload messages from the session so these tool results are not saved a second time.
        return request.mutate().prompt(new Prompt(List.of(), request.prompt().getOptions())).build();
    }

    private ChatResponse summarize(Prompt prompt) {
        cancellation.check();
        DiscoveryContextInterceptor summaryInterceptor = new DiscoveryContextInterceptor(contextService, budgetManager,
                context.getBudget(), false);
        ChatResponse response = chatModel.createChatModel(builder -> builder
                .interceptor(cancellation::intercept).interceptor(summaryInterceptor))
                .call(prompt);
        cancellation.check();
        budgetManager.checkResponse(context.getBudget(), response);
        return response;
    }

    @Override
    public @NonNull String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // Run once per model request, including requests made after tools return their results.
        return ToolCallingAdvisor.DEFAULT_ORDER + 1;
    }
}
