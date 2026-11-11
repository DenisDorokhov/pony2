package net.dorokhov.pony2.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;

import java.time.Duration;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class RetryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RetryExecutor.class);

    private RetryExecutor() {
    }

    public static <T> T execute(String name, int retriesOnException, Supplier<T> supplier) {
        return execute(name, retriesOnException, Duration.ofSeconds(1), supplier);
    }

    public static <T> T execute(String name, int retriesOnException, Duration retryDelay, Supplier<T> supplier) {
        requireNonNull(name, "Name must not be null.");
        requireNonNull(supplier, "Supplier must not be null.");
        requireNonNull(retryDelay, "Retry delay must not be null.");
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(retriesOnException)
                .delay(retryDelay)
                .predicate(failure -> failure instanceof RuntimeException && !Thread.currentThread().isInterrupted())
                .build();
        RetryTemplate retryTemplate = new RetryTemplate(retryPolicy);
        retryTemplate.setRetryListener(new RetryListener() {
            @Override
            public void beforeRetry(RetryPolicy policy, Retryable<?> retryable, RetryState state) {
                logger.trace("Executing request '{}' attempt {}/{}.", name,
                        state.getRetryCount() + 1, (long) retriesOnException + 1);
            }

            @Override
            public void onRetryableExecution(RetryPolicy policy, Retryable<?> retryable, RetryState state) {
                if (state.isSuccessful()) {
                    return;
                }
                Throwable failure = state.getLastException();
                if (!policy.shouldRetry(failure) || state.getRetryCount() >= retriesOnException) {
                    logger.debug("Request '{}' failed after {} attempt(s).", name, state.getRetryCount() + 1, failure);
                } else {
                    logger.debug("Request '{}' failed on attempt {}/{}. Retrying after {}.", name,
                            state.getRetryCount() + 1, (long) retriesOnException + 1, retryDelay, failure);
                }
            }
        });
        logger.trace("Executing request '{}' attempt 1/{}.", name, (long) retriesOnException + 1);
        return retryTemplate.invoke(supplier);
    }
}
