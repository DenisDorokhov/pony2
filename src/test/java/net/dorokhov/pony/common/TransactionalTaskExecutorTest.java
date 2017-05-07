package net.dorokhov.pony.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.task.TaskExecutor;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TransactionalTaskExecutorTest {
    
    @InjectMocks
    private TransactionalTaskExecutor transactionalTaskExecutor;
    
    @Mock
    private TaskExecutor taskExecutor;
    
    @Test
    public void shouldExecute() throws Exception {
        Runnable task = () -> {};
        transactionalTaskExecutor.execute(task);
        verify(taskExecutor).execute(task);
    }
}