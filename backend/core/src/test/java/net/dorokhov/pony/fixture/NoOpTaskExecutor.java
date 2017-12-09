package net.dorokhov.pony.fixture;

import java.util.concurrent.Executor;
import javax.annotation.Nonnull;

public class NoOpTaskExecutor implements Executor {

    @Override
    public void execute(@Nonnull Runnable command) {
    }
}
