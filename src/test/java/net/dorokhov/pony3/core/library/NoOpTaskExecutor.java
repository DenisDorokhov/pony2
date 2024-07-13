package net.dorokhov.pony3.core.library;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

public class NoOpTaskExecutor implements Executor {

    @Override
    public void execute(@Nonnull Runnable command) {
    }
}
