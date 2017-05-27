package net.dorokhov.pony.fixture;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

public class NoOpTaskExecutor implements Executor {

    @Override
    public void execute(@Nonnull Runnable command) {
    }
}
