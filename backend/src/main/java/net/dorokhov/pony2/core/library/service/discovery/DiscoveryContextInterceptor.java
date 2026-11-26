package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.llm.service.LlmContextService;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public class DiscoveryContextInterceptor implements Interceptor {

    private final LlmContextService contextService;
    private final DiscoveryContextBudgetManager budgetManager;
    private final DiscoveryContextBudget budget;
    private final boolean compactionEnabled;

    public DiscoveryContextInterceptor(
            LlmContextService contextService,
            DiscoveryContextBudgetManager budgetManager,
            DiscoveryContextBudget budget,
            boolean compactionEnabled
    ) {
        this.contextService = contextService;
        this.budgetManager = budgetManager;
        this.budget = budget;
        this.compactionEnabled = compactionEnabled;
    }

    @Override
    public @NonNull Response intercept(Interceptor.@NonNull Chain chain) throws IOException {
        LlmContextService.PreparedRequest request = contextService.inspect(chain, budget.getContextSize());
        if (request == null) {
            return chain.proceed(chain.request());
        }
        return switch (budgetManager.decide(budget, request.context(), compactionEnabled)) {
            case DiscoveryContextBudgetManager.Generate generation -> chain.proceed(request.withOutputLimit(generation.maxTokens()));
            case DiscoveryContextBudgetManager.Compact compaction -> throw new CompactionRequiredException(compaction.inputTokens());
            default -> throw new IllegalStateException("Unexpected context budget decision.");
        };
    }

    public static class CompactionRequiredException extends RuntimeException {

        private final int inputTokens;

        private CompactionRequiredException(int inputTokens) {
            super("Context compaction required at " + inputTokens + " tokens.");
            this.inputTokens = inputTokens;
        }

        public int inputTokens() {
            return inputTokens;
        }
    }
}
