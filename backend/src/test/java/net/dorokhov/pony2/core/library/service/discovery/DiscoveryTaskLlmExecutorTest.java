package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.library.service.exception.FollowUpException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static net.dorokhov.pony2.api.llm.domain.LlmCacheRegion.SPOTIFY;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryTaskLlmExecutorTest {

    private static final DiscoveryTaskLlmExecutor.CacheSettings CACHE_SETTINGS =
            new DiscoveryTaskLlmExecutor.CacheSettings(SPOTIFY, "dialogue", 1);

    @Mock private ChatModel model;
    @Mock private LlmCacheService cacheService;
    @Mock private DiscoveryTaskRepository taskRepository;

    private final LibraryJobSynchronizer jobSynchronizer = new LibraryJobSynchronizer();
    private final ShutdownService shutdownService = new ShutdownService();
    private final Map<String, String> cache = new HashMap<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private LibraryJobSynchronizer.LibraryJobRegistration jobRegistration;
    private DiscoveryTaskLlmExecutor operation;

    @BeforeEach
    void setUp() throws Exception {
        jobRegistration = jobSynchronizer.registerDiscoveryJob();
        lenient().when(cacheService.get(eq(SPOTIFY), anyString(), eq(1)))
                .thenAnswer(invocation -> Optional.ofNullable(cache.get(invocation.getArgument(1))));
        lenient().when(cacheService.put(eq(SPOTIFY), anyString(), eq(1), anyString())).thenAnswer(invocation -> {
            cache.put(invocation.getArgument(1), invocation.getArgument(3));
            return null;
        });
        lenient().when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        lenient().when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            prompts.add(prompt);
            return response("answer " + prompts.size());
        });
        ChatClient client = ChatClient.builder(model).build();
        DiscoveryChatClient discoveryClient = new DiscoveryChatClient(customizer -> client,
                jobSynchronizer, new DiscoveryAdvisor(shutdownService, jobSynchronizer));
        operation = new DiscoveryTaskLlmExecutor(discoveryClient, cacheService, taskRepository,
                jobSynchronizer, shutdownService, transactionManager());
    }

    @AfterEach
    void tearDown() {
        jobRegistration.close();
    }

    @Test
    void shouldContinueDialogueAndReplayEachExchangeFromCache() {
        DiscoveryTask task = task();
        DialogueRequest search = searchRequest();
        String answer = operation.call(task, search, CACHE_SETTINGS, Function.identity());
        DialogueRequest verification = verificationRequest(search, answer);

        String verified = operation.call(task, verification, CACHE_SETTINGS, Function.identity());

        assertThat(verified).isEqualTo("answer 2");
        assertThat(prompts).hasSize(2);
        assertThat(prompts.get(1).getInstructions()).extracting(Message::getText)
                .containsExactly("Find artist data", "Artist", answer, "Verify this answer");
        assertThat(JsonConverter.listFromJson(task.getRawRequest(), Object.class)).hasSize(2);
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(answer, verified);
        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.STARTED);
        assertThat(task.getResult()).isNull();
        assertThat(cache).hasSize(2);

        DiscoveryTask cachedTask = task();
        String cachedAnswer = operation.call(cachedTask, search, CACHE_SETTINGS, Function.identity());
        String cachedVerification = operation.call(cachedTask, verificationRequest(search, cachedAnswer), CACHE_SETTINGS, Function.identity());

        assertThat(cachedAnswer).isEqualTo(answer);
        assertThat(cachedVerification).isEqualTo(verified);
        assertThat(cachedTask.getRawRequest()).isEqualTo(task.getRawRequest());
        assertThat(cachedTask.getRawResult()).isEqualTo(task.getRawResult());
        verify(model, times(2)).call(any(Prompt.class));

        operation.call(task(), verificationRequest(search, "different answer"), CACHE_SETTINGS, Function.identity());

        verify(model, times(3)).call(any(Prompt.class));
        assertThat(cache).hasSize(3);
    }

    @Test
    void shouldPreserveBothResponsesWithoutCachingRejectedVerification() {
        DiscoveryTask task = task();
        DialogueRequest search = searchRequest();
        String answer = operation.call(task, search, CACHE_SETTINGS, Function.identity());
        IllegalStateException rejected = new IllegalStateException("Verification failed");

        assertThatThrownBy(() -> operation.call(task, verificationRequest(search, answer), CACHE_SETTINGS, rawResponse -> {
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(answer, rawResponse);
            throw rejected;
        })).isSameAs(rejected);

        assertThat(cache).hasSize(1);
        assertThat(JsonConverter.listFromJson(task.getRawRequest(), Object.class)).hasSize(2);
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(answer, "answer 2");
    }

    @Test
    void shouldPreserveFirstExchangeWhenNextCallFails() {
        DiscoveryTask task = task();
        DialogueRequest search = searchRequest();
        String answer = operation.call(task, search, CACHE_SETTINGS, Function.identity());
        IllegalStateException failure = new IllegalStateException("Model unavailable");
        when(model.call(any(Prompt.class))).thenThrow(failure);

        assertThatThrownBy(() -> operation.call(task, verificationRequest(search, answer), CACHE_SETTINGS, Function.identity()))
                .isSameAs(failure);

        assertThat(JsonConverter.listFromJson(task.getRawRequest(), Object.class)).hasSize(2);
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(answer, null);
        assertThat(cache).hasSize(1);
    }

    @Test
    void shouldProcessCachedResponsesBeforeReturning() {
        DialogueRequest request = searchRequest();
        operation.call(task(), request, CACHE_SETTINGS, Function.identity());
        DiscoveryTask cachedTask = task();
        IllegalStateException rejected = new IllegalStateException("Response rejected");

        assertThatThrownBy(() -> operation.call(cachedTask, request, CACHE_SETTINGS, response -> {
            throw rejected;
        })).isSameAs(rejected);

        assertThat(JsonConverter.fromJson(cachedTask.getRawResult(), String[].class)).containsExactly("answer 1");
        verify(model).call(any(Prompt.class));
        verify(cacheService).put(eq(SPOTIFY), anyString(), eq(1), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldRequestOneMorePromptAndCacheAcceptedResponse(boolean cachedResponse) {
        DiscoveryTask task = task();
        DialogueRequest search = searchRequest();
        if (cachedResponse) {
            operation.call(task(), search, CACHE_SETTINGS, Function.identity());
        }
        String result = operation.call(task, search, CACHE_SETTINGS, answer -> {
            if (answer.equals("answer 1")) {
                throw new FollowUpException("Verify this answer", new IllegalStateException("Invalid response"));
            }
            return answer;
        });

        assertThat(result).isEqualTo("answer 2");
        assertThat(prompts.get(1).getInstructions()).extracting(Message::getText)
                .containsExactly("Find artist data", "Artist", "answer 1", "Verify this answer");
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly("answer 1", "answer 2");
        Object[] requests = JsonConverter.fromJson(task.getRawRequest(), Object[].class);
        assertThat(requests).hasSize(2);
        assertThat(cache).hasSize(1);

        DiscoveryTask cachedTask = task();
        assertThat(operation.call(cachedTask, search, CACHE_SETTINGS, Function.identity())).isEqualTo("answer 2");
        assertThat(JsonConverter.fromJson(cachedTask.getRawResult(), String[].class)).containsExactly("answer 2");
        assertThat(JsonConverter.fromJson(cachedTask.getRawRequest(), Object[].class)).containsExactly(requests[1]);
        verify(model, times(2)).call(any(Prompt.class));
    }

    @Test
    void shouldStopAfterOneAdditionalPromptWithoutCachingRejectedResponses() {
        DiscoveryTask task = task();
        FollowUpException rejected = new FollowUpException("Verify this answer", new IllegalStateException("Invalid response"));

        assertThatThrownBy(() -> operation.call(task, searchRequest(), CACHE_SETTINGS, answer -> {
            throw rejected;
        })).isSameAs(rejected);

        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly("answer 1", "answer 2");
        assertThat(cache).isEmpty();
        verify(model, times(2)).call(any(Prompt.class));
    }

    @Test
    void shouldInterruptBeforeProcessingCachedResponse() {
        DialogueRequest request = searchRequest();
        operation.call(task(), request, CACHE_SETTINGS, Function.identity());
        String cached = cache.values().iterator().next();
        when(cacheService.get(eq(SPOTIFY), anyString(), eq(1))).thenAnswer(invocation -> {
            jobSynchronizer.cancelDiscovery();
            return Optional.of(cached);
        });
        DiscoveryTask cancelledTask = task();

        assertThatThrownBy(() -> operation.call(cancelledTask, request, CACHE_SETTINGS, response -> {
            throw new AssertionError("A cancelled operation must not process the response");
        })).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(cancelledTask.getRawResult()).isEqualTo("[null]");
        verify(model).call(any(Prompt.class));
    }

    private DiscoveryTask task() {
        return new DiscoveryTask().setStatus(DiscoveryTask.Status.STARTED);
    }

    private DialogueRequest searchRequest() {
        return new DialogueRequest(List.of(new SystemMessage("Find artist data"), new UserMessage("Artist")));
    }

    private DialogueRequest verificationRequest(DialogueRequest search, String answer) {
        List<Message> messages = new ArrayList<>(search.messages());
        messages.add(new AssistantMessage(answer));
        messages.add(new UserMessage("Verify this answer"));
        return new DialogueRequest(messages);
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private record DialogueRequest(List<Message> messages) implements DiscoveryTaskLlmExecutor.Request {
        private DialogueRequest {
            messages = List.copyOf(messages);
        }

        @Override
        public Prompt toPrompt() {
            return new Prompt(messages);
        }
    }
}
