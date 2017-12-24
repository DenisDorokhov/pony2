package net.dorokhov.pony.core;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

public class NoOpTaskExecutor implements Executor {

    @Override
    public void execute(@Nonnull Runnable command) {
    }
}
