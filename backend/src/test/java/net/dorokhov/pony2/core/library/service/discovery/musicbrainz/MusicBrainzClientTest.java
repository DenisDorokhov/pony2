package net.dorokhov.pony2.core.library.service.discovery.musicbrainz;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.dorokhov.pony2.common.JsonConverter;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MusicBrainzClientTest {

    private HttpServer server;
    private ExecutorService executor;
    private String baseUrl;
    private final List<String> urls = new ArrayList<>();
    private final List<String> responses = new ArrayList<>();
    private final MusicBrainzClient.ExchangeObserver observer = new MusicBrainzClient.ExchangeObserver() {
        @Override
        public void onRequest(String url) {
            urls.add(url);
            responses.add(null);
        }

        @Override
        public void onResponse(String url, String response) {
            responses.set(responses.size() - 1, response);
        }
    };

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/ws/2/";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void shouldReadAllPagesAndSendUserAgentAndEncodedQuery() {
        List<Integer> offsets = new ArrayList<>();
        List<String> userAgents = new ArrayList<>();
        List<String> queries = new ArrayList<>();
        String query = "release:\"A \\\"B\\\" & C\" AND artistname:\"Artist\"";
        server.createContext("/ws/2/release-group", exchange -> {
            HttpUrl url = HttpUrl.get(baseUrl).resolve(exchange.getRequestURI().toString());
            int offset = Integer.parseInt(url.queryParameter("offset"));
            offsets.add(offset);
            queries.add(url.queryParameter("query"));
            userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            List<MusicBrainzDto.ReleaseGroup> groups = offset == 0 ? List.of(group("one"), group("two")) : List.of(group("three"));
            respond(exchange, 200, JsonConverter.toJson(new MusicBrainzDto.ReleaseGroupSearch(3, offset, groups)));
        });
        MusicBrainzClient client = client(Duration.ZERO, 1000);

        try (MusicBrainzClient.Session session = client.openSession("pony2/2.0 (admin@example.com)", observer)) {
            assertThat(session.searchReleaseGroups(query)).extracting(MusicBrainzDto.ReleaseGroup::id).containsExactly("one", "two", "three");
        }

        assertThat(offsets).containsExactly(0, 2);
        assertThat(queries).containsExactly(query, query);
        assertThat(userAgents).containsOnly("pony2/2.0 (admin@example.com)");
        assertThat(urls).hasSize(2);
        assertThat(responses).allSatisfy(response -> assertThat(response).contains("release-groups"));
    }

    @Test
    void shouldStopAtItemLimitWithoutReturningPartialResults() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/ws/2/release-group", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, JsonConverter.toJson(new MusicBrainzDto.ReleaseGroupSearch(1001, 0, List.of(group("one")))));
        });

        try (MusicBrainzClient.Session session = client(Duration.ZERO, 1000).openSession("pony2/test (admin@example.com)", observer)) {
            assertThatThrownBy(() -> session.searchReleaseGroups("query")).isInstanceOf(MusicBrainzClient.ItemLimitException.class);
        }

        assertThat(requests).hasValue(1);
        assertThat(responses).singleElement().satisfies(response -> assertThat(response).contains("1001"));
    }

    @Test
    void shouldAcceptExactlyTheItemLimitAndRejectPrematureEmptyPages() {
        server.createContext("/ws/2/release-group", exchange -> {
            String query = HttpUrl.get(baseUrl).resolve(exchange.getRequestURI().toString()).queryParameter("query");
            List<MusicBrainzDto.ReleaseGroup> groups = "complete".equals(query) ? List.of(group("one"), group("two")) : List.of();
            respond(exchange, 200, JsonConverter.toJson(new MusicBrainzDto.ReleaseGroupSearch(2, 0, groups)));
        });

        try (MusicBrainzClient.Session session = client(Duration.ZERO, 2).openSession("pony2/test (admin@example.com)", observer)) {
            assertThat(session.searchReleaseGroups("complete")).hasSize(2);
            assertThatThrownBy(() -> session.searchReleaseGroups("incomplete")).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty page");
        }
    }

    @Test
    void shouldReadCountryCodesAndPreserveRawErrorResponses() {
        String artist = "{\"id\":\"artist\",\"name\":\"Artist\",\"area\":{\"name\":\"Country\",\"iso-3166-1-codes\":[\"US\",\"GB\"]}}";
        server.createContext("/ws/2/artist/artist", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("User-Agent")).isEqualTo("pony2/test (admin@example.com)");
            respond(exchange, 200, artist);
        });
        server.createContext("/ws/2/artist/error", exchange -> respond(exchange, 503, "service unavailable"));

        try (MusicBrainzClient.Session session = client(Duration.ZERO, 1000).openSession("pony2/test (admin@example.com)", observer)) {
            assertThat(session.getArtist("artist").area().country()).containsExactly("US", "GB");
            assertThatThrownBy(() -> session.getArtist("error")).isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 503");
        }

        assertThat(responses).containsExactly(artist, "service unavailable");
    }

    @Test
    void shouldNotBypassRateLimitWithAutomaticRedirectRequests() {
        AtomicInteger targetRequests = new AtomicInteger();
        server.createContext("/ws/2/artist/old", exchange -> {
            exchange.getResponseHeaders().set("Location", baseUrl + "artist/new");
            respond(exchange, 301, "moved");
        });
        server.createContext("/ws/2/artist/new", exchange -> {
            targetRequests.incrementAndGet();
            respond(exchange, 200, "{\"id\":\"new\"}");
        });

        try (MusicBrainzClient.Session session = client(Duration.ZERO, 1000).openSession("pony2/test (admin@example.com)", observer)) {
            assertThatThrownBy(() -> session.getArtist("old")).isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 301");
        }

        assertThat(targetRequests).hasValue(0);
        assertThat(responses).containsExactly("moved");
    }

    @Test
    void shouldCancelAnActiveHttpRequest() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/ws/2/artist/artist", exchange -> {
            received.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
                respond(exchange, 200, "{\"id\":\"artist\"}");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        try (MusicBrainzClient.Session session = client(Duration.ZERO, 1000).openSession("pony2/test (admin@example.com)", observer)) {
            Future<MusicBrainzDto.Artist> result = executor.submit(() -> session.getArtist("artist"));
            assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
            session.cancel();
            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(MusicBrainzClient.CancelledException.class);
            assertThat(responses).isEqualTo(Arrays.asList((String) null));
        } finally {
            release.countDown();
        }
    }

    @Test
    void shouldCancelRateLimitWaitingAndRejectNewSessionsAfterShutdown() throws Exception {
        server.createContext("/ws/2/artist/artist", exchange -> respond(exchange, 200, "{\"id\":\"artist\"}"));
        MusicBrainzClient client = client(Duration.ofSeconds(30), 1000);
        try (MusicBrainzClient.Session session = client.openSession("pony2/test (admin@example.com)", observer)) {
            session.getArtist("artist");
            Future<MusicBrainzDto.Artist> result = executor.submit(() -> session.getArtist("artist"));
            client.onApplicationShutdown();
            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(MusicBrainzClient.CancelledException.class);
            assertThat(urls).hasSize(1);
            assertThatThrownBy(() -> client.openSession("pony2/test (admin@example.com)", observer))
                    .isInstanceOf(MusicBrainzClient.CancelledException.class);
        }
    }

    @Test
    void shouldShareRateLimitBetweenSessions() {
        server.createContext("/ws/2/artist/artist", exchange -> respond(exchange, 200, "{\"id\":\"artist\"}"));
        MusicBrainzClient client = client(Duration.ofSeconds(3), 1000);
        List<Long> requestTimes = new ArrayList<>();
        MusicBrainzClient.ExchangeObserver timedObserver = new MusicBrainzClient.ExchangeObserver() {
            @Override
            public void onRequest(String url) {
                requestTimes.add(System.nanoTime());
            }

            @Override
            public void onResponse(String url, String response) {}
        };
        try (MusicBrainzClient.Session first = client.openSession("pony2/test (admin@example.com)", timedObserver);
             MusicBrainzClient.Session second = client.openSession("pony2/test (admin@example.com)", timedObserver)) {
            first.getArtist("artist");
            second.getArtist("artist");
        }
        assertThat(requestTimes.get(1) - requestTimes.get(0)).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(2900));
    }

    private MusicBrainzClient client(Duration interval, int maxItems) {
        return new MusicBrainzClient(baseUrl, interval, Duration.ofSeconds(10), maxItems);
    }

    private MusicBrainzDto.ReleaseGroup group(String id) {
        return new MusicBrainzDto.ReleaseGroup(id, "Album", "Album", List.of(), "2000", List.of(), List.of());
    }

    private void respond(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
