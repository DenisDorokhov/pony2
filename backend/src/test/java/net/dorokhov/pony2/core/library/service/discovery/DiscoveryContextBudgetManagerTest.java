package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.llm.service.LlmContextService;
import net.dorokhov.pony2.core.llm.service.exception.LlmContextLimitException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscoveryContextBudgetManagerTest {

    private final DiscoveryContextBudgetManager manager = new DiscoveryContextBudgetManager();
    private final DiscoveryContextBudget budget = new DiscoveryContextBudget(.98, .7);

    @Test
    void shouldUseNativeContextThresholds() {
        assertThat(manager.decide(budget, new LlmContextService.ContextInfo(180224, 126155, 0, true), true))
                .isEqualTo(new DiscoveryContextBudgetManager.Generate(50464));
        assertThat(manager.decide(budget, new LlmContextService.ContextInfo(180224, 126156, 0, true), true))
                .isEqualTo(new DiscoveryContextBudgetManager.Compact(126156));
        assertThat(manager.decide(budget, new LlmContextService.ContextInfo(180224, 54067, 0, true), true))
                .isEqualTo(new DiscoveryContextBudgetManager.Generate(122552));
    }

    @Test
    void shouldCompactToolHistoryBeforeEnforcingTheGenerationCeiling() {
        assertThat(manager.decide(budget, new LlmContextService.ContextInfo(1000, 990, 0, true), true))
                .isEqualTo(new DiscoveryContextBudgetManager.Compact(990));
        assertThatThrownBy(() -> manager.decide(budget, new LlmContextService.ContextInfo(1000, 990, 0, true), false))
                .isInstanceOf(LlmContextLimitException.class)
                .hasMessage("LLM prompt reached the context limit: 990/1000 tokens.");
    }

    @Test
    void shouldApplyTheBudgetWithoutCompactingSummaryRequests() {
        LlmContextService.ContextInfo context = new LlmContextService.ContextInfo(1000, 700, 0, true);
        assertThat(manager.decide(budget, context, false))
                .isEqualTo(new DiscoveryContextBudgetManager.Generate(280));
        assertThat(manager.decide(budget, context, true))
                .isEqualTo(new DiscoveryContextBudgetManager.Compact(700));
    }

    @Test
    void shouldKeepTheRequestedOutputLimitWithinTheContextBudget() {
        assertThat(manager.decide(budget, new LlmContextService.ContextInfo(1000, 900, 79, false), true))
                .isEqualTo(new DiscoveryContextBudgetManager.Generate(79));
        assertThat(manager.decide(budget, new LlmContextService.ContextInfo(1000, 900, 80, false), true))
                .isEqualTo(new DiscoveryContextBudgetManager.Generate(80));
        assertThat(manager.decide(budget, new LlmContextService.ContextInfo(1000, 900, 81, false), true))
                .isEqualTo(new DiscoveryContextBudgetManager.Generate(80));
    }
}
