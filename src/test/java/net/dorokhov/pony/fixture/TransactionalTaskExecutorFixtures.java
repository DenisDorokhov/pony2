package net.dorokhov.pony.fixture;

import net.dorokhov.pony.common.TransactionalTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;

public final class TransactionalTaskExecutorFixtures {
    
    private TransactionalTaskExecutorFixtures() {
    }

    public static TransactionalTaskExecutor get() {
        return new TransactionalTaskExecutor(new SyncTaskExecutor());
    }
}
