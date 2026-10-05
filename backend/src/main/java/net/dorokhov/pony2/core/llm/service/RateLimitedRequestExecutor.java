package net.dorokhov.pony2.core.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Executes requests with per-context rate limiting, optional random delay, and retries.
 * Idle context state is bounded by evicting least recently used contexts.
 */
public class RateLimitedRequestExecutor {

    public static final int DEFAULT_MAX_CONTEXT_COUNT = 1000;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Object contextsLock = new Object();
    private final String name;
    private final Settings settings;
    private final Clock clock;
    private final Sleeper sleeper;
    private final LongUnaryOperator randomMillisSupplier;
    private final Map<Object, ContextState> contextStates = new LinkedHashMap<>(16, 0.75f, true);

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
        return execute(DefaultContext.INSTANCE, supplier);
    }

    public <T> T execute(Object context, Supplier<T> supplier) {
        requireNonNull(context, "Context must not be null.");
        requireNonNull(supplier, "Supplier must not be null.");
        for (int retryIndex = 0; ; retryIndex++) {
            try {
                logger.trace("Executing rate-limited request '{}' in context '{}' attempt {}/{}.", name, context,
                        retryIndex + 1, settings.retriesOnException() + 1);
                return executeOnce(context, supplier);
            } catch (RuntimeException e) {
                if (retryIndex >= settings.retriesOnException()) {
                    logger.error("Rate-limited request '{}' in context '{}' failed after {} attempt(s).", name,
                            context, retryIndex + 1, e);
                    throw e;
                }
                logger.warn("Rate-limited request '{}' in context '{}' failed on attempt {}/{}. Retrying.", name,
                        context, retryIndex + 1, settings.retriesOnException() + 1, e);
            }
        }
    }

    private <T> T executeOnce(Object context, Supplier<T> supplier) {
        ContextState contextState = contextState(context);
        try {
            synchronized (contextState.lock) {
                waitIfNeeded(context, contextState);
                contextState.lastRequestDate = clock.instant();
                return supplier.get();
            }
        } finally {
            release(contextState);
        }
    }

    private ContextState contextState(Object context) {
        synchronized (contextsLock) {
            ContextState contextState = contextStates.get(context);
            if (contextState == null) {
                contextState = new ContextState();
                contextStates.put(context, contextState);
            }
            contextState.inUse++;
            evictContextsIfNeeded();
            return contextState;
        }
    }

    private void release(ContextState contextState) {
        synchronized (contextsLock) {
            contextState.inUse--;
            evictContextsIfNeeded();
        }
    }

    private void evictContextsIfNeeded() {
        Iterator<Map.Entry<Object, ContextState>> iterator = contextStates.entrySet().iterator();
        while (contextStates.size() > settings.maxContextCount() && iterator.hasNext()) {
            Map.Entry<Object, ContextState> entry = iterator.next();
            if (entry.getValue().inUse == 0) {
                logger.trace("Evicting rate limiter '{}' context '{}'. Max context count: {}.", name, entry.getKey(),
                        settings.maxContextCount());
                iterator.remove();
            }
        }
    }

    private void waitIfNeeded(Object context, ContextState contextState) {
        if (contextState.lastRequestDate == null) {
            return;
        }
        Duration randomDelay = randomDelay();
        Duration delay = settings.notMoreOftenThan().plus(randomDelay);
        Instant nextRequestDate = contextState.lastRequestDate.plus(delay);
        Instant now = clock.instant();
        Duration waitDuration = Duration.between(now, nextRequestDate);
        if (waitDuration.isPositive()) {
            logger.debug(
                    "Rate limiter '{}' context '{}' waits {} before the next request. Last request: {}, now: {}, next request: {}, base delay: {}, random delay: {}.",
                    name,
                    context,
                    waitDuration,
                    contextState.lastRequestDate,
                    now,
                    nextRequestDate,
                    settings.notMoreOftenThan(),
                    randomDelay
            );
            sleep(waitDuration);
        } else {
            logger.trace(
                    "Rate limiter '{}' context '{}' does not wait. Last request: {}, now: {}, next request: {}, base delay: {}, random delay: {}.",
                    name,
                    context,
                    contextState.lastRequestDate,
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

    private enum DefaultContext {
        INSTANCE;

        @Override
        public String toString() {
            return "default";
        }
    }

    private static class ContextState {

        private final Object lock = new Object();

        private Instant lastRequestDate;
        private int inUse;
    }

    public record Settings(
            Duration notMoreOftenThan,
            Duration randomDelay,
            int retriesOnException,
            int maxContextCount
    ) {

        public Settings(Duration notMoreOftenThan, Duration randomDelay, int retriesOnException) {
            this(notMoreOftenThan, randomDelay, retriesOnException, DEFAULT_MAX_CONTEXT_COUNT);
        }

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
            if (maxContextCount < 1) {
                throw new IllegalArgumentException("Max context count must be positive.");
            }
        }
    }
}
