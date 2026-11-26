package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.llm.service.ChatModelImpl;
import net.dorokhov.pony2.core.llm.service.LlmContextService;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

@Component
public class DiscoveryChatClient {

    private final BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> chatClientFactory;
    private final ChatModelImpl chatModel;
    private final LlmContextService contextService;
    private final DiscoveryContextBudgetManager budgetManager;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final DiscoveryCompactionSummarizer compactionSummarizer;
    private final double contextLimitRatio;
    private final double compactionTriggerRatio;

    private final Object lock = new Object();
    private final Set<Cancellation> activeCalls = new LinkedHashSet<>();

    private boolean shutdown;

    public DiscoveryChatClient(
            BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> chatClientFactory,
            ChatModelImpl chatModel,
            LlmContextService contextService,
            DiscoveryContextBudgetManager budgetManager,
            LibraryJobSynchronizer jobSynchronizer,
            DiscoveryCompactionSummarizer compactionSummarizer,
            @Value("${pony.llm.contextLimitRatio:0.98}")
            double contextLimitRatio,
            @Value("${pony.llm.compactionTriggerRatio:0.7}")
            double compactionTriggerRatio
    ) {
        if (!(0 < compactionTriggerRatio && compactionTriggerRatio < contextLimitRatio && contextLimitRatio < 1)) {
            throw new IllegalArgumentException("Context ratios must satisfy 0 < trigger < limit < 1.");
        }
        this.chatClientFactory = chatClientFactory;
        this.chatModel = chatModel;
        this.contextService = contextService;
        this.budgetManager = budgetManager;
        this.jobSynchronizer = jobSynchronizer;
        this.compactionSummarizer = compactionSummarizer;
        this.contextLimitRatio = contextLimitRatio;
        this.compactionTriggerRatio = compactionTriggerRatio;
    }

    @EventListener(ContextClosedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onApplicationShutdown() {
        List<Cancellation> cancellations;
        synchronized (lock) {
            shutdown = true;
            cancellations = new ArrayList<>(activeCalls);
        }
        cancellations.forEach(Cancellation::cancel);
    }

    /**
     * Sends the messages to the model and waits for its reply, including any tool calls.
     * Each call uses a new session. The original messages are kept when tool results are summarized.
     * Combines request options with the configured model settings before building the request.
     * Cancelling discovery or shutting down the application stops active HTTP requests, including summary requests.
     * Deletes the session when this method finishes, including when it throws an exception.
     */
    public @Nullable ChatResponse call(boolean toolsEnabled, List<Message> messages, ChatOptions.Builder<?> options) {
        Cancellation cancellation = new Cancellation();
        synchronized (lock) {
            if (shutdown) {
                throw new DiscoveryInterruptedException();
            }
            activeCalls.add(cancellation);
        }
        try (
                LibraryJobSynchronizer.CancellationSubscription ignored = jobSynchronizer.onCancel(cancellation::cancel);
                DiscoveryContext context = new DiscoveryContext(messages, contextLimitRatio, compactionTriggerRatio)
        ) {
            cancellation.check();
            DiscoveryContextInterceptor interceptor = new DiscoveryContextInterceptor(contextService, budgetManager,
                    context.getBudget(), true);
            DiscoveryAdvisor advisor = new DiscoveryAdvisor(chatModel, contextService, budgetManager, jobSynchronizer,
                    compactionSummarizer, context, cancellation);
            ChatClient client = chatClientFactory.apply(builder -> builder
                    .interceptor(cancellation::intercept).interceptor(interceptor), toolsEnabled);
            // The session already contains the input messages. Let Spring load them instead of saving a second copy.
            ChatResponse response = client.prompt().options(options)
                    .advisors(advisor, SessionMemoryAdvisor.builder(context.getSessions()).build())
                    .advisors(spec -> spec.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, context.getSessionId()))
                    .call().chatResponse();
            cancellation.check();
            return response;
        } catch (Throwable t) {
            cancellation.check();
            throw t;
        } finally {
            cancellation.currentCall.set(null);
            synchronized (lock) {
                activeCalls.remove(cancellation);
            }
        }
    }

    static final class Cancellation {

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<Call> currentCall = new AtomicReference<>();

        private void cancel() {
            cancelled.set(true);
            Call call = currentCall.get();
            if (call != null) {
                call.cancel();
            }
        }

        Response intercept(Interceptor.Chain chain) throws IOException {
            // Remember this HTTP call so cancellation can stop it while its response body is still being read.
            currentCall.set(chain.call());
            check();
            try {
                return chain.proceed(chain.request());
            } catch (IOException e) {
                // Stop SDK retries when this call was cancelled or its thread was interrupted.
                // Other I/O failures keep the SDK's normal retry behavior.
                check();
                throw e;
            }
        }

        void check() {
            if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                throw new DiscoveryInterruptedException();
            }
        }
    }
}
