package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryTaskExecutorTest {

    @Mock
    private DiscoveryTaskRepository taskRepository;
    @Mock
    private DiscoveryTaskExecution<String> execution;

    private final LibraryJobSynchronizer jobSynchronizer = new LibraryJobSynchronizer();
    private LibraryJobSynchronizer.LibraryJobRegistration jobRegistration;
    private DiscoveryTaskExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        jobRegistration = jobSynchronizer.registerDiscoveryJob();
        executor = new DiscoveryTaskExecutor(taskRepository, jobSynchronizer, transactionManager());
    }

    @AfterEach
    void tearDown() {
        jobRegistration.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "result")
    void shouldKeepTaskRegisteredThroughCompletionHook(String value) {
        DiscoveryTask task = task();
        when(execution.startTask()).thenAnswer(invocation -> {
            assertThat(jobSynchronizer.hasRunningTasks()).isTrue();
            return task;
        });
        when(execution.executeTask(task)).thenReturn(value);
        doAnswer(invocation -> {
            verify(taskRepository).save(task);
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo(JsonConverter.toJson(value));
            assertThat(jobSynchronizer.hasRunningTasks()).isTrue();
            return null;
        }).when(execution).onCompletion(task, value);

        DiscoveryTaskExecutor.TaskResult<String> result = executor.execute(execution);

        assertThat(result.status()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(result.value()).isEqualTo(value);
        verify(execution).onCompletion(task, value);
        verify(execution, never()).onError(any(), any());
        assertThat(jobSynchronizer.hasRunningTasks()).isFalse();
    }

    @Test
    void shouldPreserveCompletedTaskWhenCompletionHookFails() {
        DiscoveryTask task = task();
        IllegalStateException hookFailure = new IllegalStateException("Hook failed");
        when(execution.startTask()).thenReturn(task);
        when(execution.executeTask(task)).thenReturn("result");
        doThrow(hookFailure).when(execution).onCompletion(task, "result");

        assertThatThrownBy(() -> executor.execute(execution)).isSameAs(hookFailure);

        verify(taskRepository).save(task);
        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(task.getResult()).isEqualTo(JsonConverter.toJson("result"));
        verify(execution, never()).onError(any(), any());
        assertThat(jobSynchronizer.hasRunningTasks()).isFalse();
    }

    @Test
    void shouldPreserveFailedTaskWhenErrorHookFails() {
        DiscoveryTask task = task();
        IllegalArgumentException taskFailure = new IllegalArgumentException("Task failed");
        IllegalStateException hookFailure = new IllegalStateException("Hook failed");
        when(execution.startTask()).thenReturn(task);
        when(execution.executeTask(task)).thenThrow(taskFailure);
        doThrow(hookFailure).when(execution).onError(task, taskFailure);

        assertThatThrownBy(() -> executor.execute(execution)).isSameAs(hookFailure);

        verify(taskRepository).save(task);
        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(task.getResult()).contains("Task failed").doesNotContain("Hook failed");
        verify(execution, never()).onCompletion(any(), any());
        assertThat(jobSynchronizer.hasRunningTasks()).isFalse();
    }

    @Test
    void shouldReleaseRegistrationWhenTaskCannotStart() {
        IllegalStateException failure = new IllegalStateException("Could not create task");
        when(execution.startTask()).thenThrow(failure);

        assertThatThrownBy(() -> executor.execute(execution)).isSameAs(failure);

        assertThat(jobSynchronizer.hasRunningTasks()).isFalse();
        verify(execution).startTask();
        verifyNoMoreInteractions(execution);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void shouldNotStartTaskAfterCancellation() {
        jobSynchronizer.cancelDiscovery();

        assertThatThrownBy(() -> executor.execute(execution)).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(jobSynchronizer.hasRunningTasks()).isFalse();
        verifyNoInteractions(execution, taskRepository);
    }

    private DiscoveryTask task() {
        return new DiscoveryTask()
                .setJob(new DiscoveryJob().setId("job"))
                .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .setStatus(DiscoveryTask.Status.STARTED)
                .setParameter("{}");
    }
}
