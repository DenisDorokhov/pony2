package net.dorokhov.pony.common;

import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionalTaskExecutor {
    
    private final TaskExecutor taskExecutor;

    public TransactionalTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Transactional
    public void execute(Runnable task) {
        taskExecutor.execute(task);
    }
}
