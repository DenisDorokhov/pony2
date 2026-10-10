package net.dorokhov.pony2.core.library.service.discovery;

import com.google.common.base.Stopwatch;
import com.google.common.hash.Hashing;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.llm.domain.LlmCacheRegion;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.library.service.exception.FollowUpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Component
public class DiscoveryTaskLlmExecutor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryChatClient chatClient;
    private final LlmCacheService cacheService;
    private final DiscoveryTaskRepository taskRepository;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final ShutdownService shutdownService;
    private final TransactionTemplate transactionTemplate;

    public DiscoveryTaskLlmExecutor(
            DiscoveryChatClient chatClient,
            LlmCacheService cacheService,
            DiscoveryTaskRepository taskRepository,
            LibraryJobSynchronizer jobSynchronizer,
            ShutdownService shutdownService,
            PlatformTransactionManager transactionManager
    ) {
        this.chatClient = chatClient;
        this.cacheService = cacheService;
        this.taskRepository = taskRepository;
        this.jobSynchronizer = jobSynchronizer;
        this.shutdownService = shutdownService;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    /**
     * Makes a synchronous model call, or reuses its cached response. The caller supplies conversation history.
     * The response handler may throw FollowUpException to append a prompt and request one additional response.
     * Raw requests and responses are appended to parallel JSON arrays before the response is processed.
     * Only an accepted response enters the cache, under the original request's key. Null cache settings disable caching.
     * Calls for the same task must be sequential; task status and the final result belong to its lifecycle.
     */
    public <R> R call(
            DiscoveryTask task, Request request, @Nullable CacheSettings cacheSettings, Function<String, R> responseHandler
    ) {

        interruptIfNeeded();

        String rawRequest = JsonConverter.toJson(request);
        List<Object> requests = task.getRawRequest() != null
                ? new ArrayList<>(JsonConverter.listFromJson(task.getRawRequest(), Object.class)) : new ArrayList<>();
        List<String> responses = task.getRawResult() != null
                ? new ArrayList<>(JsonConverter.listFromJson(task.getRawResult(), String.class)) : new ArrayList<>();
        requests.add(request);
        responses.add(null);
        saveExchange(task, requests, responses);

        String key = cacheSettings != null ? cacheSettings.keyPrefix() + ":" + Hashing.sha256().hashString(rawRequest, UTF_8) : null;
        Optional<String> cached = cacheSettings != null ? cacheService.get(cacheSettings.region(), key, cacheSettings.version()) : Optional.empty();
        Stopwatch stopwatch = Stopwatch.createStarted();
        String response;
        if (cached.isPresent()) {
            logger.debug("LLM cache hit for discovery task '{}' of type {}. Region: {}, key: '{}', version: {}.",
                    task.getId(), task.getType(), cacheSettings.region(), key, cacheSettings.version());
            CacheEntry entry = JsonConverter.fromJson(cached.get(), CacheEntry.class);
            requests.set(requests.size() - 1, entry.request());
            response = entry.response();
        } else {
            Prompt prompt = request.toPrompt();
            if (logger.isDebugEnabled()) {
                logger.debug("Requesting LLM response for discovery task '{}' of type {}.\n\nRequest:\n\n{}",
                        task.getId(), task.getType(), JsonConverter.toPrettyJson(request));
            }
            response = chatClient.call(spec -> {
                spec.messages(prompt.getInstructions());
                if (prompt.getOptions() != null) {
                    spec.options(prompt.getOptions().mutate());
                }
                return spec.call().content();
            });
        }
        interruptIfNeeded();

        responses.set(responses.size() - 1, response);
        saveExchange(task, requests, responses);
        if (logger.isDebugEnabled()) {
            logger.debug("LLM exchange for discovery task '{}' of type {} completed in {}. Source: {}.\n\nRequest:\n\n{}\n\nResponse:\n\n{}",
                    task.getId(), task.getType(), stopwatch.elapsed(), cached.isPresent() ? "cache" : "llm",
                    JsonConverter.toPrettyJson(requests.getLast()), formatResponseForLog(response));
        }
        R result;
        Request acceptedRequest = request;
        boolean retried = false;
        try {
            result = responseHandler.apply(response);
        } catch (FollowUpException e) {
            acceptedRequest = new FollowUpRequest(request, response, e.getPrompt());
            response = call(task, acceptedRequest, null, Function.identity());
            result = responseHandler.apply(response);
            retried = true;
        }
        interruptIfNeeded();
        if (cacheSettings != null && (cached.isEmpty() || retried)) {
            cacheService.put(cacheSettings.region(), key, cacheSettings.version(),
                    JsonConverter.toJson(new CacheEntry(acceptedRequest, response)));
        }
        return result;
    }

    private String formatResponseForLog(@Nullable String response) {
        if (response == null) {
            return "null";
        }
        try {
            return JsonConverter.toPrettyJson(JsonConverter.fromJson(response));
        } catch (RuntimeException e) {
            return response;
        }
    }

    private void saveExchange(DiscoveryTask task, List<Object> requests, List<String> responses) {
        transactionTemplate.executeWithoutResult(status -> taskRepository.save(task
                .setRawRequest(JsonConverter.toJson(requests))
                .setRawResult(JsonConverter.toJson(responses))));
    }

    private void interruptIfNeeded() {
        jobSynchronizer.interruptDiscoveryIfCancelled();
        if (shutdownService.isShutdown()) {
            throw new DiscoveryInterruptedException();
        }
    }

    /**
     * A serializable snapshot of all prompt inputs, including previous messages and any model options.
     * Its JSON is used for both the raw request and the cache key.
     */
    public interface Request {
        Prompt toPrompt();
    }

    public record CacheSettings(LlmCacheRegion region, String keyPrefix, int version) {}

    private record CacheEntry(Object request, @Nullable String response) {}

    private record FollowUpRequest(Request originalRequest, @Nullable String previousResponse, String userPrompt) implements Request {
        @Override
        public Prompt toPrompt() {
            Prompt originalPrompt = originalRequest.toPrompt();
            List<Message> messages = new ArrayList<>(originalPrompt.getInstructions());
            if (previousResponse != null && !previousResponse.isBlank()) {
                messages.add(new AssistantMessage(previousResponse));
            }
            messages.add(new UserMessage(userPrompt));
            return new Prompt(messages, originalPrompt.getOptions());
        }
    }
}
