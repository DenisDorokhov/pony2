package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
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
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@Component
public class DiscoveryChatClient {

    static final String INVOCATION_KEY = DiscoveryChatClient.class.getName();

    private final BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> chatClientFactory;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final DiscoveryAdvisor discoveryAdvisor;
    private final double contextLimitRatio;
    private final double compactionTriggerRatio;
    private final double compactionTargetRatio;
    private final DiscoveryContextCompactor contextCompactor = new DiscoveryContextCompactor();

    private final Object lock = new Object();
    private final Set<Cancellation> activeCalls = new LinkedHashSet<>();

    private boolean shutdown;

    public DiscoveryChatClient(
            BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> chatClientFactory,
            LibraryJobSynchronizer jobSynchronizer,
            DiscoveryAdvisor discoveryAdvisor,
            @Value("${pony.llm.contextLimitRatio:0.9}") double contextLimitRatio,
            @Value("${pony.llm.compactionTriggerRatio:0.7}") double compactionTriggerRatio,
            @Value("${pony.llm.compactionTargetRatio:0.3}") double compactionTargetRatio
    ) {
        if (!(0 < compactionTargetRatio && compactionTargetRatio < compactionTriggerRatio &&
                compactionTriggerRatio < contextLimitRatio && contextLimitRatio < 1)) {
            throw new IllegalArgumentException("Context ratios must satisfy 0 < target < trigger < limit < 1.");
        }
        this.chatClientFactory = chatClientFactory;
        this.jobSynchronizer = jobSynchronizer;
        this.discoveryAdvisor = discoveryAdvisor;
        this.contextLimitRatio = contextLimitRatio;
        this.compactionTriggerRatio = compactionTriggerRatio;
        this.compactionTargetRatio = compactionTargetRatio;
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
     * Executes a synchronous request, including tool-calling rounds, with discovery and shutdown cancellation.
     * The callback must consume the response before returning; deferred and streaming results are not supported.
     */
    public <T> T call(Function<ChatClient.ChatClientRequestSpec, T> request) {
        return call(true, request);
    }

    public <T> T call(boolean toolsEnabled, Function<ChatClient.ChatClientRequestSpec, T> request) {
        Cancellation cancellation = new Cancellation();
        synchronized (lock) {
            if (shutdown) {
                throw new DiscoveryInterruptedException();
            }
            activeCalls.add(cancellation);
        }
        try (LibraryJobSynchronizer.CancellationSubscription ignored = jobSynchronizer.onCancel(cancellation::cancel)) {
            cancellation.check();
            Invocation invocation = new Invocation(cancellation);
            ChatClient client = chatClientFactory.apply(invocation.customizer, toolsEnabled);
            T result = request.apply(client.prompt().advisors(discoveryAdvisor)
                    .advisors(spec -> spec.param(INVOCATION_KEY, invocation)));
            cancellation.check();
            return result;
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

    public final class Invocation {

        private final DiscoveryContext context = new DiscoveryContext(contextLimitRatio, compactionTriggerRatio, compactionTargetRatio);
        private final DiscoveryContextInterceptor interceptor = new DiscoveryContextInterceptor(context.getBudget());
        private final Cancellation cancellation;
        private final OpenAiHttpClientBuilderCustomizer customizer;

        private Invocation(Cancellation cancellation) {
            this.cancellation = cancellation;
            customizer = builder -> builder.interceptor(cancellation::intercept).interceptor(interceptor);
        }

        public ChatClientRequest effectiveRequest(ChatClientRequest original) {
            List<Message> messages = context.effectiveMessages(original.prompt().getInstructions());
            if (!context.hasCompactedHistory()) {
                return original;
            }
            return original.mutate().prompt(new Prompt(messages, original.prompt().getOptions())).build();
        }

        public void compact(ChatClientRequest original, ChatClientRequest effective) {
            DiscoveryContextCompactor.Compaction compaction = contextCompactor.prepare(context, effective);
            ChatResponse response = selectFragments(compaction.prompt());
            contextCompactor.complete(context, original.prompt().getInstructions().size(), compaction, response);
        }

        private ChatResponse selectFragments(Prompt prompt) {
            cancellation.check();
            DiscoveryContextInterceptor selectionInterceptor = DiscoveryContextInterceptor.forSelection(context.getBudget());
            ChatClient client = chatClientFactory.apply(builder -> builder
                    .interceptor(cancellation::intercept).interceptor(selectionInterceptor), false);
            ChatResponse response = client.prompt().messages(prompt.getInstructions())
                    .options(requireNonNull(prompt.getOptions()).mutate()).call().chatResponse();
            cancellation.check();
            requireNonNull(response, "LLM returned no compaction response.");
            DiscoveryContextInterceptor.checkFinishReason(response);
            selectionInterceptor.checkUsage(response);
            return response;
        }

        public void checkResponse(ChatResponse response) {
            interceptor.checkResponse(response);
        }
    }

    private static final class Cancellation {

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<Call> currentCall = new AtomicReference<>();

        private void cancel() {
            cancelled.set(true);
            Call call = currentCall.get();
            if (call != null) {
                call.cancel();
            }
        }

        private Response intercept(Interceptor.Chain chain) throws IOException {
            // Keep the call reachable until the response body is read, including between tool rounds.
            currentCall.set(chain.call());
            check();
            try {
                return chain.proceed(chain.request());
            } catch (IOException e) {
                // Stop SDK retries only for cancellation; ordinary I/O failures retain their retry policy.
                check();
                throw e;
            }
        }

        private void check() {
            if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                throw new DiscoveryInterruptedException();
            }
        }
    }
}
