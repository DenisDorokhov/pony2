package net.dorokhov.pony2.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryExecutorTest {

    @Test
    void shouldReturnSuccessfulResultWithoutRetry() {
        AtomicInteger calls = new AtomicInteger();

        assertThat(RetryExecutor.execute("test", 3, calls::incrementAndGet)).isEqualTo(1);
        assertThat(calls).hasValue(1);
    }

    @Test
    void shouldRecoverAfterFailuresAndResetAttemptsForEachExecution() {
        for (int execution = 0; execution < 2; execution++) {
            AtomicInteger calls = new AtomicInteger();

            String result = RetryExecutor.execute("test", 2, Duration.ZERO, () -> {
                if (calls.incrementAndGet() < 3) {
                    throw new IllegalStateException("Temporary failure.");
                }
                return "value";
            });

            assertThat(result).isEqualTo("value");
            assertThat(calls).hasValue(3);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void shouldRethrowLastFailureAfterConfiguredRetries(int retries) {
        List<RuntimeException> failures = new ArrayList<>();

        assertThatThrownBy(() -> RetryExecutor.execute("test", retries, Duration.ZERO, () -> {
            RuntimeException failure = new IllegalStateException("Failure " + failures.size());
            failures.add(failure);
            throw failure;
        })).satisfies(failure -> assertThat(failure).isSameAs(failures.getLast()));

        assertThat(failures).hasSize(retries + 1);
    }

    @ParameterizedTest
    @CsvSource({"true, 1000", "false, 20"})
    void shouldWaitBeforeRetry(boolean useDefaultDelay, long delayMillis) {
        List<Long> attemptTimes = new ArrayList<>();

        Supplier<String> supplier = () -> {
            attemptTimes.add(System.nanoTime());
            if (attemptTimes.size() == 1) {
                throw new IllegalStateException("Temporary failure.");
            }
            return "value";
        };
        String result = useDefaultDelay ? RetryExecutor.execute("test", 1, supplier) :
                RetryExecutor.execute("test", 1, Duration.ofMillis(delayMillis), supplier);

        assertThat(result).isEqualTo("value");
        assertThat(attemptTimes).hasSize(2);
        assertThat(attemptTimes.getLast() - attemptTimes.getFirst()).isGreaterThanOrEqualTo(Duration.ofMillis(delayMillis).toNanos());
    }

    @Test
    void shouldNotRetryErrors() {
        AtomicInteger calls = new AtomicInteger();
        AssertionError failure = new AssertionError("Fatal failure.");

        assertThatThrownBy(() -> RetryExecutor.execute("test", 3, Duration.ZERO, () -> {
            calls.incrementAndGet();
            throw failure;
        })).isSameAs(failure);

        assertThat(calls).hasValue(1);
    }

    @Test
    void shouldNotRetryWhenOperationInterruptsThread() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException failure = new IllegalStateException("Interrupted operation.");
        try {
            assertThatThrownBy(() -> RetryExecutor.execute("test", 3, Duration.ZERO, () -> {
                calls.incrementAndGet();
                Thread.currentThread().interrupt();
                throw failure;
            })).isSameAs(failure);

            assertThat(calls).hasValue(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();
        }
    }

    @Test
    void shouldRejectInvalidRetrySettings() {
        assertThatThrownBy(() -> RetryExecutor.execute("test", -1, () -> "value"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RetryExecutor.execute("test", 1, Duration.ofMillis(-1), () -> "value"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RetryExecutor.execute("test", 1, null, () -> "value"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldLogAttemptsAtTraceAndFailuresAtDebug() {
        Logger logger = (Logger) LoggerFactory.getLogger(RetryExecutor.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        RuntimeException failure = new IllegalStateException("Request failed.");
        try {
            assertThatThrownBy(() -> RetryExecutor.execute("mcp-tools-list", 1, Duration.ZERO, () -> {
                throw failure;
            })).isSameAs(failure);

            assertThat(appender.list).extracting(ILoggingEvent::getLevel)
                    .containsExactly(Level.TRACE, Level.DEBUG, Level.TRACE, Level.DEBUG);
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly(
                            "Executing request 'mcp-tools-list' attempt 1/2.",
                            "Request 'mcp-tools-list' failed on attempt 1/2. Retrying after PT0S.",
                            "Executing request 'mcp-tools-list' attempt 2/2.",
                            "Request 'mcp-tools-list' failed after 2 attempt(s).");
            assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.DEBUG)
                    .allSatisfy(event -> assertThat(event.getThrowableProxy().getMessage()).isEqualTo(failure.getMessage()));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}
