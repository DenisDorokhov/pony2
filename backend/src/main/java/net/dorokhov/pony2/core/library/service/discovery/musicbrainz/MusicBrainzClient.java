package net.dorokhov.pony2.core.library.service.discovery.musicbrainz;

import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.llm.service.RateLimitedRequestExecutor;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@Component
public class MusicBrainzClient {

    private static final int PAGE_SIZE = 100;

    private final HttpUrl baseUrl;
    private final OkHttpClient httpClient;
    private final RateLimitedRequestExecutor requestExecutor;
    private final int maxItems;
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    public MusicBrainzClient(
            @Value("${pony.discovery.musicbrainz.url:https://musicbrainz.org/ws/2/}") String baseUrl,
            @Value("${pony.discovery.musicbrainz.rateLimit.notMoreOftenThan:3s}") Duration requestInterval,
            @Value("${pony.discovery.musicbrainz.rateLimit.retryDelay:10s}") Duration retryDelay,
            @Value("${pony.discovery.musicbrainz.rateLimit.retriesOnException:3}") int retriesOnException,
            @Value("${pony.discovery.musicbrainz.timeout:30s}") Duration timeout,
            @Value("${pony.discovery.musicbrainz.maxItems:1000}") int maxItems
    ) {
        checkArgument(!requestInterval.isNegative(), "Request interval must not be negative.");
        checkArgument(!timeout.isNegative() && !timeout.isZero(), "Request timeout must be positive.");
        checkArgument(maxItems > 0, "Maximum item count must be positive.");
        this.baseUrl = HttpUrl.get(baseUrl);
        this.maxItems = maxItems;
        requestExecutor = new RateLimitedRequestExecutor("musicbrainz",
                new RateLimitedRequestExecutor.Settings(requestInterval, Duration.ZERO, retryDelay, retriesOnException, 1));
        httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .callTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    public Session openSession(String userAgent, ExchangeObserver observer) {
        checkArgument(!userAgent.isBlank(), "User-Agent must not be blank.");
        Session session = new Session(userAgent, observer);
        sessions.add(session);
        if (closed.get()) {
            session.close();
            throw new CancelledException();
        }
        return session;
    }

    @EventListener(ContextClosedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onApplicationShutdown() {
        closed.set(true);
        sessions.forEach(Session::cancel);
    }

    public interface ExchangeObserver {
        void onRequest(String url);
        void onResponse(String url, String response);
    }

    public static class ItemLimitException extends RuntimeException {
        public ItemLimitException(int maxItems) {
            super("MusicBrainz search exceeds the maximum of " + maxItems + " items.");
        }
    }

    public static class CancelledException extends RuntimeException {}

    public class Session implements AutoCloseable {

        private final String userAgent;
        private final ExchangeObserver observer;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<Call> currentCall = new AtomicReference<>();

        private Session(String userAgent, ExchangeObserver observer) {
            this.userAgent = userAgent;
            this.observer = observer;
        }

        public List<MusicBrainzDto.ReleaseGroup> searchReleaseGroups(String query) {
            List<MusicBrainzDto.ReleaseGroup> groups = new ArrayList<>();
            int offset = 0;
            while (true) {
                checkCancelled();
                HttpUrl url = baseUrl.newBuilder().addPathSegment("release-group")
                        .addQueryParameter("query", query)
                        .addQueryParameter("fmt", "json")
                        .addQueryParameter("limit", Integer.toString(Math.min(PAGE_SIZE, maxItems - offset)))
                        .addQueryParameter("offset", Integer.toString(offset)).build();
                MusicBrainzDto.ReleaseGroupSearch page = JsonConverter.fromJson(get(url), MusicBrainzDto.ReleaseGroupSearch.class);
                checkState(page.count() >= 0 && page.offset() == offset, "Invalid MusicBrainz pagination response.");
                if (page.count() > maxItems || page.releaseGroups().size() > maxItems - groups.size()) {
                    throw new ItemLimitException(maxItems);
                }
                groups.addAll(page.releaseGroups());
                offset += page.releaseGroups().size();
                if (offset >= page.count()) {
                    return List.copyOf(groups);
                }
                checkState(!page.releaseGroups().isEmpty(), "MusicBrainz returned an empty page before the end of its results.");
                if (offset >= maxItems) {
                    throw new ItemLimitException(maxItems);
                }
            }
        }

        public MusicBrainzDto.Artist getArtist(String id) {
            HttpUrl url = baseUrl.newBuilder().addPathSegment("artist").addPathSegment(id)
                    .addQueryParameter("fmt", "json").build();
            return JsonConverter.fromJson(get(url), MusicBrainzDto.Artist.class);
        }

        public void cancel() {
            cancelled.set(true);
            Call call = currentCall.get();
            if (call != null) {
                call.cancel();
            }
        }

        @Override
        public void close() {
            cancel();
            sessions.remove(this);
        }

        private String get(HttpUrl url) {
            try {
                return requestExecutor.execute("musicbrainz", () -> request(url), this::checkCancelled);
            } catch (RuntimeException e) {
                checkCancelled();
                throw e;
            }
        }

        private String request(HttpUrl url) {
            Call call = httpClient.newCall(new Request.Builder().url(url).header("User-Agent", userAgent).build());
            currentCall.set(call);
            try {
                checkCancelled();
                observer.onRequest(url.toString());
                try (Response response = call.execute()) {
                    String body = requireNonNull(response.body()).string();
                    observer.onResponse(url.toString(), body);
                    checkCancelled();
                    checkState(response.isSuccessful(), "MusicBrainz request '%s' failed with HTTP %s.", url, response.code());
                    return body;
                }
            } catch (IOException e) {
                checkCancelled();
                throw new IllegalStateException("Could not request MusicBrainz URL '" + url + "'.", e);
            } finally {
                currentCall.compareAndSet(call, null);
            }
        }

        private void checkCancelled() {
            if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                throw new CancelledException();
            }
        }
    }
}
