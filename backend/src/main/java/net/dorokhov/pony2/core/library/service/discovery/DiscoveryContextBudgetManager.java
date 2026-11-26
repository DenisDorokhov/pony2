package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.llm.service.LlmContextService;
import net.dorokhov.pony2.core.llm.service.exception.LlmContextLimitException;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;

/**
 * Decides when to shorten the message history and how many tokens the model may generate.
 * Summary requests still obey the token limit, but cannot trigger another summary.
 * Also checks that model responses finish normally and stay within the token budget.
 */
@Component
public class DiscoveryContextBudgetManager {

    public Decision decide(DiscoveryContextBudget budget, LlmContextService.ContextInfo context, boolean compactionEnabled) {
        int contextSize = context.contextSize();
        int inputTokens = context.inputTokens();
        budget.setContextSize(contextSize);
        if (compactionEnabled && inputTokens >= (int) (contextSize * budget.getTriggerRatio()) && context.hasToolHistory()) {
            return new Compact(inputTokens);
        }

        int remaining = limitTokens(budget) - inputTokens;
        if (remaining <= 0) {
            throw new LlmContextLimitException("LLM prompt reached the context limit: " + inputTokens + "/" + contextSize + " tokens.");
        }
        int requestedLimit = context.requestedOutputLimit();
        if (requestedLimit > 0 && requestedLimit < remaining) {
            return new Generate(requestedLimit);
        }
        return new Generate(remaining);
    }

    public int limitTokens(DiscoveryContextBudget budget) {
        return (int) (budget.getContextSize() * budget.getLimitRatio());
    }

    /**
     * Throws an exception if the model stopped because of a token limit,
     * or if the input and output token counts together reach the configured budget.
     */
    public void checkResponse(DiscoveryContextBudget budget, ChatResponse response) {
        checkFinishReason(response);
        checkUsage(budget, response);
    }

    public static void checkFinishReason(ChatResponse response) {
        for (Generation generation : response.getResults()) {
            if ("length".equalsIgnoreCase(generation.getMetadata().getFinishReason())) {
                throw new LlmContextLimitException("LLM generation reached its token limit before completing the response.");
            }
        }
    }

    public void checkUsage(DiscoveryContextBudget budget, ChatResponse response) {
        Integer promptTokens = response.getMetadata().getUsage().getPromptTokens();
        Integer completionTokens = response.getMetadata().getUsage().getCompletionTokens();
        if (budget.getContextSize() > 0 && promptTokens != null && completionTokens != null &&
                (long) promptTokens + completionTokens >= limitTokens(budget)) {
            throw new LlmContextLimitException("LLM response reached the context limit: "
                    + ((long) promptTokens + completionTokens) + "/" + budget.getContextSize() + " tokens.");
        }
    }

    public interface Decision {}

    public record Generate(int maxTokens) implements Decision {}

    public record Compact(int inputTokens) implements Decision {}
}
