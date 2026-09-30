package net.dorokhov.pony2.core.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class RateLimitedRequestExecutor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Object lock = new Object();
    private final String name;
    private final Settings settings;
    private final Clock clock;
    private final Sleeper sleeper;
    private final LongUnaryOperator randomMillisSupplier;

    private Instant lastRequestDate;

    public RateLimitedRequestExecutor(String name, Settings settings) {
        this(name, settings, Clock.systemUTC(), Thread::sleep, bound -> ThreadLocalRandom.current().nextLong(bound));
    }

    RateLimitedRequestExecutor(
            String name,
            Settings settings,
            Clock clock,
            Sleeper sleeper,
            LongUnaryOperator randomMillisSupplier
    ) {
        this.name = requireNonNull(name, "Name must not be null.");
        this.settings = requireNonNull(settings, "Settings must not be null.");
        this.clock = clock;
        this.sleeper = sleeper;
        this.randomMillisSupplier = randomMillisSupplier;
    }

    public <T> T execute(Supplier<T> supplier) {
        requireNonNull(supplier, "Supplier must not be null.");
        for (int retryIndex = 0; ; retryIndex++) {
            try {
                logger.trace("Executing rate-limited request '{}' attempt {}/{}.", name, retryIndex + 1,
                        settings.retriesOnException() + 1);
                return executeOnce(supplier);
            } catch (RuntimeException e) {
                if (retryIndex >= settings.retriesOnException()) {
                    logger.error("Rate-limited request '{}' failed after {} attempt(s).", name, retryIndex + 1, e);
                    throw e;
                }
                logger.warn("Rate-limited request '{}' failed on attempt {}/{}. Retrying.", name, retryIndex + 1,
                        settings.retriesOnException() + 1, e);
            }
        }
    }

    private <T> T executeOnce(Supplier<T> supplier) {
        synchronized (lock) {
            waitIfNeeded();
            lastRequestDate = clock.instant();
            return supplier.get();
        }
    }

    private void waitIfNeeded() {
        if (lastRequestDate == null) {
            return;
        }
        Duration randomDelay = randomDelay();
        Duration delay = settings.notMoreOftenThan().plus(randomDelay);
        Instant nextRequestDate = lastRequestDate.plus(delay);
        Instant now = clock.instant();
        Duration waitDuration = Duration.between(now, nextRequestDate);
        if (waitDuration.isPositive()) {
            logger.debug(
                    "Rate limiter '{}' waits {} before the next request. Last request: {}, now: {}, next request: {}, base delay: {}, random delay: {}.",
                    name,
                    waitDuration,
                    lastRequestDate,
                    now,
                    nextRequestDate,
                    settings.notMoreOftenThan(),
                    randomDelay
            );
            sleep(waitDuration);
        } else {
            logger.trace(
                    "Rate limiter '{}' does not wait. Last request: {}, now: {}, next request: {}, base delay: {}, random delay: {}.",
                    name,
                    lastRequestDate,
                    now,
                    nextRequestDate,
                    settings.notMoreOftenThan(),
                    randomDelay
            );
        }
    }

    private Duration randomDelay() {
        long maxMillis = settings.randomDelay().toMillis();
        if (maxMillis == 0) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(randomMillisSupplier.applyAsLong(maxMillis + 1));
    }

    private void sleep(Duration duration) {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request execution was interrupted.", e);
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    public record Settings(
            Duration notMoreOftenThan,
            Duration randomDelay,
            int retriesOnException
    ) {
        public Settings {
            requireNonNull(notMoreOftenThan, "Request interval must not be null.");
            requireNonNull(randomDelay, "Random delay must not be null.");
            if (notMoreOftenThan.isNegative()) {
                throw new IllegalArgumentException("Request interval must not be negative.");
            }
            if (randomDelay.isNegative()) {
                throw new IllegalArgumentException("Random delay must not be negative.");
            }
            if (retriesOnException < 0) {
                throw new IllegalArgumentException("Retries count must not be negative.");
            }
        }
    }
}
