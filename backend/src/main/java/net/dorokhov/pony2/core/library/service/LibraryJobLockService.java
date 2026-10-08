package net.dorokhov.pony2.core.library.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class LibraryJobLockService {

    // ReentrantLock doesn't fit here, because we release in different thread.
    private final Semaphore semaphore = new Semaphore(1);

    public Optional<Permit> tryAcquire() {
        return semaphore.tryAcquire() ? Optional.of(new Permit()) : Optional.empty();
    }

    public final class Permit implements AutoCloseable {

        private final AtomicBoolean closed = new AtomicBoolean();

        private Permit() {
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}
