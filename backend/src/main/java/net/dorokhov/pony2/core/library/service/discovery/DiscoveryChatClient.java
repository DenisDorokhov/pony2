package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Component
public class DiscoveryChatClient {

    private final Function<OpenAiHttpClientBuilderCustomizer, ChatClient> chatClientFactory;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final DiscoveryAdvisor discoveryAdvisor;

    public DiscoveryChatClient(
            Function<OpenAiHttpClientBuilderCustomizer, ChatClient> chatClientFactory,
            LibraryJobSynchronizer jobSynchronizer,
            DiscoveryAdvisor discoveryAdvisor
    ) {
        this.chatClientFactory = chatClientFactory;
        this.jobSynchronizer = jobSynchronizer;
        this.discoveryAdvisor = discoveryAdvisor;
    }

    /**
     * Executes a synchronous request, including tool-calling rounds, with discovery cancellation.
     * The callback must consume the response before returning; deferred and streaming results are not supported.
     */
    public <T> T call(Function<ChatClient.ChatClientRequestSpec, T> request) {
        Cancellation cancellation = new Cancellation();
        try (LibraryJobSynchronizer.CancellationSubscription ignored = jobSynchronizer.onCancel(cancellation::cancel)) {
            cancellation.check();
            ChatClient client = chatClientFactory.apply(builder -> builder.interceptor(cancellation::intercept));
            T result = request.apply(client.prompt().advisors(discoveryAdvisor));
            cancellation.check();
            return result;
        } catch (RuntimeException e) {
            cancellation.check();
            throw e;
        } finally {
            cancellation.currentCall.set(null);
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
            if (cancelled.get()) {
                throw new DiscoveryInterruptedException();
            }
        }
    }
}
