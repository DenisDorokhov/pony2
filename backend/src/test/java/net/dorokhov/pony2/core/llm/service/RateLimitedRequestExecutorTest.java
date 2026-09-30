package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.core.llm.service.RateLimitedRequestExecutor.Settings;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RateLimitedRequestExecutorTest {

    private final MutableClock clock = new MutableClock();
    private final List<Duration> sleepDurations = new ArrayList<>();

    @Test
    public void shouldExecuteFirstRequestImmediately() {

        RateLimitedRequestExecutor requestExecutor = requestExecutor(
                settings(Duration.ofSeconds(30), Duration.ZERO, 0)
        );

        String result = requestExecutor.execute(() -> "value");

        assertThat(result).isEqualTo("value");
        assertThat(sleepDurations).isEmpty();
    }

    @Test
    public void shouldWaitBeforeNextRequest() {

        Settings settings = settings(Duration.ofSeconds(30), Duration.ofSeconds(10), 0);
        RateLimitedRequestExecutor requestExecutor = requestExecutor(settings);

        requestExecutor.execute(() -> "firstValue");
        String result = requestExecutor.execute(() -> "secondValue");

        assertThat(result).isEqualTo("secondValue");
        assertThat(sleepDurations).containsExactly(Duration.ofSeconds(40));
    }

    @Test
    public void shouldRetryAfterException() {

        AtomicInteger calls = new AtomicInteger();
        RateLimitedRequestExecutor requestExecutor = requestExecutor(
                settings(Duration.ofSeconds(1), Duration.ZERO, 2)
        );

        String result = requestExecutor.execute(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("Request failed.");
            }
            return "value";
        });

        assertThat(result).isEqualTo("value");
        assertThat(calls).hasValue(3);
        assertThat(sleepDurations).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Test
    public void shouldRethrowLastExceptionWhenRetriesAreExhausted() {

        AtomicInteger calls = new AtomicInteger();
        RuntimeException lastException = new IllegalArgumentException("Last failure.");
        RateLimitedRequestExecutor requestExecutor = requestExecutor(
                settings(Duration.ofSeconds(1), Duration.ZERO, 1)
        );

        assertThatThrownBy(() -> requestExecutor.execute(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("First failure.");
            }
            throw lastException;
        })).isSameAs(lastException);

        assertThat(calls).hasValue(2);
        assertThat(sleepDurations).containsExactly(Duration.ofSeconds(1));
    }

    @Test
    public void shouldValidateSettings() {

        assertThatThrownBy(() -> settings(Duration.ofSeconds(-1), Duration.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings(Duration.ZERO, Duration.ofSeconds(-1), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings(Duration.ZERO, Duration.ZERO, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Settings settings(Duration requestInterval, Duration randomDelay, int retriesOnException) {
        return new Settings(requestInterval, randomDelay, retriesOnException);
    }

    private RateLimitedRequestExecutor requestExecutor(Settings settings) {
        return new RateLimitedRequestExecutor(
                "test",
                settings,
                clock,
                duration -> {
                    sleepDurations.add(duration);
                    clock.plus(duration);
                },
                bound -> bound - 1
        );
    }

    private static class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-09-30T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void plus(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
