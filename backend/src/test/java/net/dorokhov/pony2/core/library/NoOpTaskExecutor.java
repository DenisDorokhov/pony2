package net.dorokhov.pony2.core.library;

import jakarta.annotation.Nonnull;
import java.util.concurrent.Executor;

public class NoOpTaskExecutor implements Executor {
    @Override
    public void execute(@Nonnull Runnable command) {
    }
}
