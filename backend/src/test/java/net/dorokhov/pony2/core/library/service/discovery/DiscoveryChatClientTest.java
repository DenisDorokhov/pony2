package net.dorokhov.pony2.core.library.service.discovery;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.llm.service.ChatModelImpl;
import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Timeout(15)
class DiscoveryChatClientTest {

    private static final byte[] RESPONSE = """
            {"id":"test","object":"chat.completion","created":1,"model":"test",
             "choices":[{"index":0,"message":{"role":"assistant","content":"OK"},"finish_reason":"stop"}]}
            """.getBytes(UTF_8);

    private final LibraryJobSynchronizer synchronizer = new LibraryJobSynchronizer();
    private final AtomicInteger requests = new AtomicInteger();
    private final ExecutorService serverExecutor = Executors.newCachedThreadPool();
    private final CountDownLatch responseBodyRead = new CountDownLatch(1);
    private Runnable beforeClientCreation = () -> {};
    private Runnable beforeRetry = () -> {};
    private LibraryJobSynchronizer.LibraryJobRegistration discovery;
    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        discovery = synchronizer.registerDiscoveryJob();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        serverExecutor.shutdownNow();
        discovery.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldCancelForScanBeforeHeadersOrDuringResponseBody(boolean responseStarted) throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        DiscoveryChatClient client = createClient(exchange -> blockResponse(exchange, responseStarted, received, release));
        LibraryJobSynchronizer.DiscoveryTaskRegistration task = synchronizer.registerDiscoveryTask();
        try (ExecutorService worker = Executors.newSingleThreadExecutor()) {
            Future<String> result = worker.submit(() -> {
                try (task) {
                    return call(client);
                } finally {
                    discovery.close();
                }
            });
            try {
                assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
                if (responseStarted) {
                    assertThat(responseBodyRead.await(5, TimeUnit.SECONDS)).isTrue();
                }
                try (LibraryJobSynchronizer.LibraryJobRegistration scan = synchronizer.registerScanJob(Duration.ofSeconds(2))) {
                    assertCancelled(result);
                    assertThat(requests).hasValue(1);
                    assertThat(synchronizer.hasRunningTasks()).isFalse();
                }
            } finally {
                release.countDown();
                synchronizer.cancelDiscovery();
            }
        } finally {
            task.close();
        }
    }

    @Test
    void shouldCancelParallelCallsAndAllowNextDiscovery() throws Exception {
        CountDownLatch received = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        DiscoveryChatClient client = createClient(exchange -> {
            if (requests.get() <= 2) {
                blockResponse(exchange, false, received, release);
            } else {
                respond(exchange);
            }
        });
        List<LibraryJobSynchronizer.DiscoveryTaskRegistration> tasks = new ArrayList<>();
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                LibraryJobSynchronizer.DiscoveryTaskRegistration task = synchronizer.registerDiscoveryTask();
                tasks.add(task);
                results.add(workers.submit(() -> {
                    try (task) {
                        return call(client);
                    }
                }));
            }
            try {
                assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
                synchronizer.cancelDiscovery();
                for (Future<String> result : results) {
                    assertCancelled(result);
                }
                assertThat(requests).hasValue(2);
                assertThat(synchronizer.hasRunningTasks()).isFalse();
            } finally {
                release.countDown();
                synchronizer.cancelDiscovery();
            }
        } finally {
            tasks.forEach(LibraryJobSynchronizer.DiscoveryTaskRegistration::close);
        }
        discovery.close();
        discovery = synchronizer.registerDiscoveryJob();
        assertThat(call(client)).isEqualTo("OK");
        assertThat(requests).hasValue(3);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldCancelParallelCallsOnShutdownBeforeHeadersOrDuringResponseBody(boolean responseStarted) throws Exception {
        CountDownLatch received = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        DiscoveryChatClient client = createClient(exchange -> blockResponse(exchange, responseStarted, received, release));
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DiscoveryChatClient.class, () -> client);
            context.refresh();
            try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
                List<Future<String>> results = List.of(workers.submit(() -> call(client)), workers.submit(() -> call(client)));
                try {
                    assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
                    if (responseStarted) {
                        assertThat(responseBodyRead.await(5, TimeUnit.SECONDS)).isTrue();
                    }
                    context.close();
                    for (Future<String> result : results) {
                        assertCancelled(result);
                    }
                    assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
                    assertThat(requests).hasValue(2);
                } finally {
                    release.countDown();
                    client.onApplicationShutdown();
                }
            }
        }
    }

    @Test
    void shouldRejectCallsAfterShutdownEvenForNextDiscovery() throws Exception {
        DiscoveryChatClient client = createClient(DiscoveryChatClientTest::respond);
        client.onApplicationShutdown();
        client.onApplicationShutdown();
        discovery.close();
        discovery = synchronizer.registerDiscoveryJob();
        beforeClientCreation = () -> fail("A client must not be created after shutdown.");

        assertThatThrownBy(() -> call(client)).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
        assertThat(requests).hasValue(0);
    }

    @Test
    void shouldCancelCallDuringClientCreation() throws Exception {
        CountDownLatch creatingClient = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        beforeClientCreation = () -> {
            creatingClient.countDown();
            try {
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        };
        DiscoveryChatClient client = createClient(DiscoveryChatClientTest::respond);
        try (ExecutorService worker = Executors.newSingleThreadExecutor()) {
            Future<String> result = worker.submit(() -> call(client));
            try {
                assertThat(creatingClient.await(5, TimeUnit.SECONDS)).isTrue();
                client.onApplicationShutdown();
                assertThatThrownBy(() -> call(client)).isInstanceOf(DiscoveryInterruptedException.class);
                release.countDown();
                assertCancelled(result);
                assertThat(requests).hasValue(0);
            } finally {
                release.countDown();
                client.onApplicationShutdown();
            }
        }
    }

    @Test
    void shouldKeepRetriesForServerErrors() throws IOException {
        DiscoveryChatClient client = createClient(exchange -> {
            if (requests.get() == 1) {
                exchange.sendResponseHeaders(503, -1);
            } else {
                respond(exchange);
            }
        });
        assertThat(call(client)).isEqualTo("OK");
        assertThat(requests).hasValue(2);
    }

    @Test
    void shouldCancelTheNextModelRequestAfterToolExecution() throws Exception {
        ToolCallback tool = mock(ToolCallback.class);
        when(tool.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name("lookup").description("Look up test data").inputSchema("{\"type\":\"object\"}").build());
        when(tool.getToolMetadata()).thenReturn(ToolMetadata.builder().build());
        when(tool.call(anyString(), any())).thenReturn("Tool result");
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        DiscoveryChatClient client = createClient(exchange -> {
            if (requests.get() == 1) {
                byte[] response = """
                        {"id":"test","object":"chat.completion","created":1,"model":"test",
                         "choices":[{"index":0,"message":{"role":"assistant","content":null,
                         "tool_calls":[{"id":"lookup-1","type":"function",
                         "function":{"name":"lookup","arguments":"{}"}}]},"finish_reason":"tool_calls"}]}
                        """.getBytes(UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } else {
                blockResponse(exchange, false, received, release);
            }
        }, tool);
        try (ExecutorService worker = Executors.newSingleThreadExecutor()) {
            Future<String> result = worker.submit(() -> call(client));
            try {
                assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
                synchronizer.cancelDiscovery();
                assertCancelled(result);
                assertThat(requests).hasValue(2);
                verify(tool).call(anyString(), any());
            } finally {
                release.countDown();
                synchronizer.cancelDiscovery();
            }
        }
    }

    @Test
    void shouldNotSendRetryAfterCancellation() throws IOException {
        beforeRetry = synchronizer::cancelDiscovery;
        DiscoveryChatClient client = createClient(exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "1");
            exchange.sendResponseHeaders(503, -1);
        });
        assertThatThrownBy(() -> call(client)).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(requests).hasValue(1);
    }

    @Test
    void shouldRejectCallAfterCancellationWithoutSendingRequest() throws IOException {
        DiscoveryChatClient client = createClient(DiscoveryChatClientTest::respond);
        synchronizer.cancelDiscovery();
        assertThatThrownBy(() -> call(client)).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(requests).hasValue(0);
    }

    private DiscoveryChatClient createClient(HttpHandler handler, Object... tools) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(serverExecutor);
        server.createContext("/v1/chat/completions", exchange -> {
            try (exchange) {
                requests.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                handler.handle(exchange);
            }
        });
        server.start();
        ConfigService configService = mock(ConfigService.class);
        when(configService.get()).thenReturn(new ConfigSet(null, List.of(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test", null));
        ChatModelImpl model = new ChatModelImpl(configService, ObservationRegistry.NOOP);
        return new DiscoveryChatClient(customizer -> {
            beforeClientCreation.run();
            return ChatClient.builder(model.createChatModel(builder -> {
                customizer.customize(builder);
                builder.interceptor(this::observeResponseBody);
            })).defaultTools(tools).build();
        },
                synchronizer, new DiscoveryAdvisor(new ShutdownService(), synchronizer));
    }

    private Response observeResponseBody(Interceptor.Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        ResponseBody body = requireNonNull(response.body());
        BufferedSource source = Okio.buffer(new ForwardingSource(body.source()) {
            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                if (bytesRead > 0) {
                    responseBodyRead.countDown();
                }
                return bytesRead;
            }

            @Override
            public void close() throws IOException {
                super.close();
                // The SDK closes retryable responses after the HTTP interceptor has returned.
                if (response.code() == 503) {
                    beforeRetry.run();
                }
            }
        });
        return response.newBuilder().body(ResponseBody.create(source, body.contentType(), body.contentLength())).build();
    }

    private static String call(DiscoveryChatClient client) {
        return client.call(prompt -> prompt.user("test").call().content());
    }

    private static void assertCancelled(Future<String> result) {
        assertThatThrownBy(() -> result.get(2, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(DiscoveryInterruptedException.class);
    }

    private static void respond(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, RESPONSE.length);
        exchange.getResponseBody().write(RESPONSE);
    }

    private static void blockResponse(HttpExchange exchange, boolean responseStarted,
                                      CountDownLatch received, CountDownLatch release) throws IOException {
        int offset = 0;
        if (responseStarted) {
            exchange.sendResponseHeaders(200, RESPONSE.length);
            offset = RESPONSE.length / 2;
            exchange.getResponseBody().write(RESPONSE, 0, offset);
            exchange.getResponseBody().flush();
        }
        received.countDown();
        try {
            if (!release.await(8, TimeUnit.SECONDS)) {
                throw new IOException("Test response was not released.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        if (!responseStarted) {
            exchange.sendResponseHeaders(200, RESPONSE.length);
        }
        exchange.getResponseBody().write(RESPONSE, offset, RESPONSE.length - offset);
    }
}
